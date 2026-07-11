package com.villagesat.mobilemoney.adapter.out.wallet;

import com.villagesat.mobilemoney.domain.port.out.WalletCreditPort;
import com.villagesat.mobilemoney.domain.port.out.WalletDebitPort;
import com.villagesat.mobilemoney.domain.port.out.RemoteInsufficientFundsException;
import com.villagesat.mobilemoney.domain.port.out.RemoteWalletUnavailableException;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class WalletServiceAdapter implements WalletCreditPort, WalletDebitPort {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceAdapter.class);
    
    private final RestClient walletRestClient;
    private final RestClient transactionRestClient;
    private final String internalToken;

    public WalletServiceAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${app.services.wallet-url}") String walletUrl,
            @Value("${app.services.transaction-url}") String transactionUrl,
            @Value("${villagesat.internal-service-token:dev-internal-token}") String internalToken
    ) {
        this.internalToken = internalToken;
        
        // Cible l'API de gestion des comptes (wallet-service)
        this.walletRestClient = restClientBuilder
                .clone()
                .baseUrl(walletUrl)
                .build();
                
        // Cible l'API des mouvements financiers (transaction-service)
        this.transactionRestClient = restClientBuilder
                .clone()
                .baseUrl(transactionUrl)
                .build();
    }

    /**
     * Créditer le portefeuille d'un utilisateur (ex: suite à un dépôt Mobile Money réussi)
     * Flux : Wallet Système (Source) -> Wallet Utilisateur (Destination)
     */
    @Override
    @Retry(name = "walletApiRetry")
    public void creditWallet(UUID walletId, BigDecimal amount, String currency, String reference) {
        log.info("Mobile Money Callback — Crédit du wallet utilisateur {} de {} {} (Ref: {})", walletId, amount, currency, reference);
        
        // 1. Récupération du wallet système d'où viennent les fonds
        WalletResponse systemWallet = fetchSystemWallet(currency);
        
        // 2. Exécution du transfert atomique via le service transaction
        executeTransfer(
                UUID.randomUUID(), // Idempotency key générée pour cette action
                systemWallet.id(), 
                walletId, 
                amount, 
                currency, 
                reference
        );
    }

    /**
     * Débiter le portefeuille d'un utilisateur (ex: pour initier un retrait vers Mobile Money)
     * Flux : Wallet Utilisateur (Source) -> Wallet Système (Destination de collecte)
     */
    @Override
    @Retry(name = "walletApiRetry")
    public void debitWallet(UUID walletId, BigDecimal amount, String currency, String reference) {
        log.info("Mobile Money Request — Débit du wallet utilisateur {} de {} {} (Ref: {})", walletId, amount, currency, reference);
        
        // 1. Récupération du wallet système où stocker les fonds collectés
        WalletResponse systemWallet = fetchSystemWallet(currency);
        
        // 2. Exécution du transfert atomique via le service transaction
        executeTransfer(
                UUID.randomUUID(), 
                walletId, 
                systemWallet.id(), 
                amount, 
                currency, 
                reference
        );
    }

    /**
     * Récupère les données du portefeuille système depuis le WALLET-SERVICE
     */
    private WalletResponse fetchSystemWallet(String currency) {
        return walletRestClient.get()
                .uri("/system/currencies/{currency}", currency.toUpperCase())
                .header("X-Internal-Service-Token", internalToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, response) -> {
                    log.error("Impossible d'obtenir le wallet système pour la devise {} depuis wallet-service", currency);
                    throw new RemoteWalletUnavailableException(null, "Service Wallet Système indisponible");
                })
                .body(WalletResponse.class);
    }

    /**
     * Ordonne l'écriture comptable du transfert au TRANSACTION-SERVICE
     */
    private void executeTransfer(UUID idempotencyKey, UUID sourceId, UUID destId, BigDecimal amount, String currency, String reference) {
        TransferRequest request = new TransferRequest(
                sourceId,
                destId,
                amount.toPlainString(), // Conversion sûre sans notation scientifique pour la regex @Pattern
                currency.toUpperCase(),
                "Flux Mobile Money - Ref: " + reference,
                reference
        );

        transactionRestClient.post()
                .uri("/api/v1/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey.toString())
                .header("X-Internal-Service-Token", internalToken)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, response) -> {
                    handleTransactionError(sourceId, response.getStatusCode());
                })
                .toBodilessEntity();
    }

    /**
     * Traduction des erreurs HTTP du transaction-service en exceptions de domaine claires
     */
    private void handleTransactionError(UUID walletId, HttpStatusCode statusCode) {
        int code = statusCode.value();
        log.warn("Le moteur de transaction a retourné un code d'erreur HTTP {} pour le wallet {}", code, walletId);

        if (code == 400) {
            throw new RemoteInsufficientFundsException(walletId);
        }
        if (code == 423 || code == 403) {
            throw new RemoteWalletUnavailableException(walletId, "Le portefeuille impliqué est verrouillé ou restreint");
        }
        if (code == 404) {
            throw new RemoteWalletUnavailableException(walletId, "Le portefeuille impliqué n'existe pas");
        }

        throw new org.springframework.web.client.HttpServerErrorException(statusCode, "Panne du Transaction Service");
    }

    // --- Records DTO locaux ---
    private record WalletResponse(UUID id, String currency) {}

    private record TransferRequest(
            UUID sourceWalletId,
            UUID destinationWalletId,
            String amount,
            String currency,
            String description,
            String externalReference
    ) {}
}