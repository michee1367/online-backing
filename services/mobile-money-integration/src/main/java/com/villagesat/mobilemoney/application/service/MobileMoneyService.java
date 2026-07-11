package com.villagesat.mobilemoney.application.service;

import com.villagesat.mobilemoney.domain.model.MobileMoneyProvider;
import com.villagesat.mobilemoney.domain.model.MobileMoneyTransaction;
import com.villagesat.mobilemoney.domain.model.ProviderConfig;
import com.villagesat.mobilemoney.domain.model.TransactionStatus;
import com.villagesat.mobilemoney.domain.model.TransactionType;
import com.villagesat.mobilemoney.domain.port.in.CallbackCommand;
import com.villagesat.mobilemoney.domain.port.in.DepositCommand;
import com.villagesat.mobilemoney.domain.port.in.MobileMoneyUseCase;
import com.villagesat.mobilemoney.domain.port.in.WithdrawalCommand;
import com.villagesat.mobilemoney.domain.port.out.MobileMoneyEventPublisher;
import com.villagesat.mobilemoney.domain.port.out.MobileMoneyGatewayPort;
import com.villagesat.mobilemoney.domain.port.out.MobileMoneyGatewayPort.GatewayRequest;
import com.villagesat.mobilemoney.domain.port.out.MobileMoneyGatewayPort.GatewayResponse;
import com.villagesat.mobilemoney.domain.port.out.MobileMoneyTransactionRepository;
import com.villagesat.mobilemoney.domain.port.out.ProviderConfigRepository;
import com.villagesat.mobilemoney.domain.port.out.WalletCreditPort;
import com.villagesat.mobilemoney.domain.port.out.WalletDebitPort;
import com.villagesat.mobilemoney.domain.port.out.RemoteInsufficientFundsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Service
// 💡 Retrait du @Transactional global pour éviter de bloquer le pool de connexions DB pendant les appels API
public class MobileMoneyService implements MobileMoneyUseCase {

    private static final Logger log = LoggerFactory.getLogger(MobileMoneyService.class);

    private final MobileMoneyTransactionRepository transactionRepository;
    private final ProviderConfigRepository providerConfigRepository;
    private final MobileMoneyGatewayPort gatewayPort;
    private final WalletCreditPort walletCreditPort;
    private final WalletDebitPort walletDebitPort; // 💡 Injecté pour gérer les retraits
    private final MobileMoneyEventPublisher eventPublisher;

    public MobileMoneyService(MobileMoneyTransactionRepository transactionRepository,
                              ProviderConfigRepository providerConfigRepository,
                              MobileMoneyGatewayPort gatewayPort,
                              WalletCreditPort walletCreditPort,
                              WalletDebitPort walletDebitPort,
                              MobileMoneyEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.providerConfigRepository = providerConfigRepository;
        this.gatewayPort = gatewayPort;
        this.walletCreditPort = walletCreditPort;
        this.walletDebitPort = walletDebitPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public MobileMoneyTransaction initiateDeposit(DepositCommand command) {
        // 1. Validation hors transaction lourde
        validateProviderActive(command.provider());
        String externalRef = generateExternalRef();

        // 2. Écriture initiale rapide (Commit immédiat)
        MobileMoneyTransaction tx = createInitialTransaction(command.userId(), command.walletId(), 
                command.provider(), command.phoneNumber(), command.amount(), command.currency(), 
                TransactionType.DEPOSIT, externalRef);

        GatewayRequest request = new GatewayRequest(
                command.phoneNumber(), command.amount(), command.currency(),
                tx.getExternalRef(), TransactionType.DEPOSIT.name()
        );

        try {
            // 3. Appel réseau (Pas de verrou DB actif ici pendant le timeout réseau)
            GatewayResponse response = gatewayPort.sendRequest(command.provider(), request);

            if (response.success()) {
                // 4. Crédit local + Clôture (Protégé par le Retry & Idempotence du wallet-service)
                return finalizeSuccessTransaction(tx.getId(), response.providerRef(), TransactionType.DEPOSIT);
            } else {
                return finalizeFailedTransaction(tx.getId(), response.errorMessage());
            }
        } catch (Exception e) {
            log.error("Échec critique réseau lors du dépôt pour la ref: {}", externalRef, e);
            // On laisse le statut en INITIATED. Le callback ou un batch de réconciliation viendra fixer le statut.
            throw e;
        }
    }

    @Override
    public MobileMoneyTransaction initiateWithdrawal(WithdrawalCommand command) {
        validateProviderActive(command.provider());
        String externalRef = generateExternalRef();

        // 1. Écriture initiale en base
        MobileMoneyTransaction tx = createInitialTransaction(command.userId(), command.walletId(), 
                command.provider(), command.phoneNumber(), command.amount(), command.currency(), 
                TransactionType.WITHDRAWAL, externalRef);

        try {
            // 💡 2. SÉCURITÉ : Débit immédiat du Wallet local AVANT l'appel API opérateur.
            // Si le solde est insuffisant, l'exception métier configurée plus tôt est levée ici.
            walletDebitPort.debitWallet(tx.getWalletId(), tx.getAmount(), tx.getCurrency(), tx.getExternalRef());
            
        } catch (RemoteInsufficientFundsException e) {
            finalizeFailedTransaction(tx.getId(), "Solde insuffisant sur le portefeuille local.");
            throw e;
        }

        GatewayRequest request = new GatewayRequest(
                command.phoneNumber(), command.amount(), command.currency(),
                tx.getExternalRef(), TransactionType.WITHDRAWAL.name()
        );

        try {
            // 3. Demande de transfert vers l'opérateur (ex: API Orange/M-Pesa)
            GatewayResponse response = gatewayPort.sendRequest(command.provider(), request);

            if (response.success()) {
                return finalizeSuccessTransaction(tx.getId(), response.providerRef(), TransactionType.WITHDRAWAL);
            } else {
                // En cas d'échec de l'opérateur, il faudra prévoir un REBOURSEMENT (Crédit) du wallet local.
                MobileMoneyTransaction failedTx = finalizeFailedTransaction(tx.getId(), response.errorMessage());
                triggerRefund(failedTx);
                return failedTx;
            }
        } catch (Exception e) {
            log.error("Coupure réseau pendant le retrait. Statut suspendu pour la ref: {}", externalRef, e);
            // Statut reste INITIATED. On attend le Callback ou le script de réconciliation automatique.
            throw e;
        }
    }

    @Override
    @Transactional // Callback s'exécute de bout en bout de façon atomique
    public MobileMoneyTransaction handleCallback(CallbackCommand command) {
        MobileMoneyTransaction tx = transactionRepository.findByExternalRef(command.externalRef())
                .orElseThrow(() -> new IllegalArgumentException("Transaction introuvable pour la ref: " + command.externalRef()));

        // Évite de traiter deux fois un callback déjà finalisé (Idempotence de callback)
        if (tx.getStatus() != TransactionStatus.INITIATED) {
            return tx;
        }

        if ("SUCCESS".equalsIgnoreCase(command.status())) {
            tx.markCompleted(command.providerRef());
            if (tx.getTransactionType() == TransactionType.DEPOSIT) {
                walletCreditPort.creditWallet(tx.getWalletId(), tx.getAmount(), tx.getCurrency(), tx.getExternalRef());
                //walletCreditPort.creditWallet(tx.getWalletId(), tx.getAmount(), tx.getCurrency(), tx.getExternalRef());
                eventPublisher.publishDeposit(tx);
            } else {
                eventPublisher.publishWithdrawal(tx);
            }
        } else {
            tx.markFailed(command.failedReason());
            if (tx.getTransactionType() == TransactionType.WITHDRAWAL) {
                triggerRefund(tx); // Rembourser le débit initial
            }
        }

        return transactionRepository.save(tx);
    }

    // --- Transactions isolées et courtes pour protéger les performances de la base de données ---

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected MobileMoneyTransaction createInitialTransaction(UUID userId, UUID walletId, MobileMoneyProvider provider, 
                                                             String phoneNumber, BigDecimal amount, String currency, 
                                                             TransactionType type, String externalRef) {
        MobileMoneyTransaction tx = new MobileMoneyTransaction();
        tx.setId(UUID.randomUUID());
        tx.setUserId(userId);
        tx.setWalletId(walletId);
        tx.setProvider(provider);
        tx.setPhoneNumber(phoneNumber);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        tx.setTransactionType(type);
        tx.setStatus(TransactionStatus.INITIATED);
        tx.setExternalRef(externalRef);
        tx.setCreatedAt(Instant.now());
        tx.setVersion(0L);
        return transactionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected MobileMoneyTransaction finalizeSuccessTransaction(UUID id, String providerRef, TransactionType type) {
        MobileMoneyTransaction tx = transactionRepository.findById(id).orElseThrow();
        
        if (type == TransactionType.DEPOSIT) {
            //walletCreditPort.creditWallet(tx.getWalletId(), tx.getAmount(), tx.getCurrency(), tx.getExternalRef());
            tx.markPending(providerRef);
            eventPublisher.publishDeposit(tx);
        } else {
            tx.markCompleted(providerRef);
            eventPublisher.publishWithdrawal(tx);
        }
        return transactionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected MobileMoneyTransaction finalizeFailedTransaction(UUID id, String reason) {
        MobileMoneyTransaction tx = transactionRepository.findById(id).orElseThrow();
        tx.markFailed(reason);
        return transactionRepository.save(tx);
    }

    private void triggerRefund(MobileMoneyTransaction tx) {
        log.info("Lancement du remboursement (Crédit) suite à un échec opérateur pour la ref: {}", tx.getExternalRef());
        // On utilise l'idempotence naturelle : une référence spécifique de remboursement "REFUND-"
        walletCreditPort.creditWallet(tx.getWalletId(), tx.getAmount(), tx.getCurrency(), generateExternalRef());
    }

    // --- Requêtes de lecture simples ---

    @Override
    @Transactional(readOnly = true)
    public MobileMoneyTransaction checkStatus(UUID transactionId) {
        return getTransaction(transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MobileMoneyTransaction> listTransactions(UUID userId) {
        return transactionRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public MobileMoneyTransaction getTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction introuvable: " + transactionId));
    }

    private void validateProviderActive(MobileMoneyProvider provider) {
        ProviderConfig config = providerConfigRepository.findByProvider(provider)
                .orElseThrow(() -> new IllegalArgumentException("Opérateur non configuré: " + provider));
        if (!config.isActive()) {
            throw new IllegalStateException("L'opérateur " + provider + " est actuellement désactivé.");
        }
    }

    private String generateExternalRef() {
        
        return "MM-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    /*private UUID generateExternalRef() {
        return UUID.randomUUID();
        
        //return "MM-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }*/
}
