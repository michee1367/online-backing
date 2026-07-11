package com.villagesat.payment.adapter.out.external;

import com.villagesat.payment.domain.port.out.WalletOperationPort;
import com.villagesat.payment.domain.port.out.RemoteInsufficientFundsException;
import com.villagesat.payment.domain.port.out.RemoteWalletUnavailableException;
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
public class WalletOperationAdapter implements WalletOperationPort {

    private static final Logger log = LoggerFactory.getLogger(WalletOperationAdapter.class);
    
    // Deux clients REST distincts pour cibler chaque microservice
    private final RestClient walletRestClient;
    private final RestClient transactionRestClient;
    private final String internalToken;

    public WalletOperationAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${villagesat.services.wallet-url}") String walletServiceUrl,
            @Value("${villagesat.services.transaction-url}") String transactionServiceUrl,
            @Value("${villagesat.internal-service-token:dev-internal-token}") String internalToken
    ) {
        this.internalToken = internalToken;
        
        // Initialisation du client pour le microservice Wallet
        this.walletRestClient = restClientBuilder
                .clone() // clone() permet d'isoler la configuration de la BaseURL
                .baseUrl(walletServiceUrl)
                .build();
                
        // Initialisation du client pour le microservice Transaction
        this.transactionRestClient = restClientBuilder
                .clone()
                .baseUrl(transactionServiceUrl)
                .build();
    }

    @Override
    @Retry(name = "walletApiRetry")
    public void creditMerchant(UUID merchantId, BigDecimal amount, String currency, String reference) {
        log.info("Traitement Crédit Marchand — Récupération du wallet système [{}]", currency);
        
        // 1. Appel HTTP vers wallet-service pour récupérer le compte système
        WalletResponse systemWallet = fetchSystemWalletFromWalletService(currency);
        WalletResponse merchantWallet = fetchMerchantWalletFromWalletService(merchantId, currency);
        
        // 2. Appel HTTP vers transaction-service pour effectuer le transfert (Système -> Marchand)
        executeTransferInTransactionService(
                UUID.randomUUID(), // Clé d'idempotence générée pour l'opération
                systemWallet.id(), 
                merchantWallet.id(), 
                amount, 
                currency, 
                reference
        );
    }

    @Override
    @Retry(name = "walletApiRetry")
    public void debitCustomer(UUID customerWalletId, BigDecimal amount, String currency, String reference) {
        log.info("Traitement Débit Client — Récupération du wallet système [{}]", currency);
        
        // 1. Appel HTTP vers wallet-service pour récupérer le compte système
        WalletResponse systemWallet = fetchSystemWalletFromWalletService(currency);
        
        // 2. Appel HTTP vers transaction-service pour effectuer le transfert (Client -> Système)
        executeTransferInTransactionService(
                UUID.randomUUID(), 
                customerWalletId, 
                systemWallet.id(), 
                amount, 
                currency, 
                reference
        );
    }

    /**
     * Appelle WALLET-SERVICE : GET /system/currencies/{currency}
     */
    private WalletResponse fetchSystemWalletFromWalletService(String currency) {
        return walletRestClient.get()
                .uri("/system/currencies/{currency}", currency.toUpperCase())
                .header("X-Internal-Service-Token", internalToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, response) -> {
                    log.error("Échec récupération wallet système pour la devise {} - Status: {}", currency, response.getStatusCode());
                    throw new RemoteWalletUnavailableException(null, "Wallet système introuvable ou indisponible");
                })
                .body(WalletResponse.class);
    }

    
    /**
     * Appelle WALLET-SERVICE : GET /system/currencies/{currency}
     */
    private WalletResponse fetchMerchantWalletFromWalletService(UUID merchantId, String currency) {
        return walletRestClient.get()
                .uri("/users/{merchantId}/currencies/{currency}/types/business", merchantId, currency.toUpperCase())
                .header("X-Internal-Service-Token", internalToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, response) -> {
                    log.error("Échec récupération wallet système pour la devise {} - Status: {}", currency, response.getStatusCode());
                    throw new RemoteWalletUnavailableException(null, "Wallet système introuvable ou indisponible");
                })
                .body(WalletResponse.class);
    }

    /**
     * Appelle TRANSACTION-SERVICE : POST /api/v1/transactions/transfer
     */
    private void executeTransferInTransactionService(UUID idempotencyKey, UUID sourceId, UUID destId, BigDecimal amount, String currency, String reference) {
        TransferRequest request = new TransferRequest(
                sourceId,
                destId,
                amount.toPlainString(), // Conversion propre pour matcher la validation @Pattern du controller
                currency.toUpperCase(),
                "Opération externe système: " + reference,
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
     * Gestion des exceptions basées sur les retours HTTP du transaction-service
     */
    private void handleTransactionError(UUID walletId, HttpStatusCode statusCode) {
        int code = statusCode.value();
        log.warn("Le service transaction a renvoyé une erreur HTTP {}", code);

        if (code == 400) throw new RemoteInsufficientFundsException(walletId);
        if (code == 423 || code == 403) throw new RemoteWalletUnavailableException(walletId, "Compte bloqué ou interdit");
        if (code == 404) throw new RemoteWalletUnavailableException(walletId, "Compte introuvable dans le système");
        
        throw new org.springframework.web.client.HttpServerErrorException(statusCode, "Erreur interne de transaction");
    }

    // --- DTOs pour le mapping des échanges REST ---
    
    // Calqué sur la réponse attendue depuis le wallet-service
    private record WalletResponse(UUID id, String currency) {}

    // Calqué sur le corps de requête attendu par le transaction-service
    private record TransferRequest(
            UUID sourceWalletId,
            UUID destinationWalletId,
            String amount,
            String currency,
            String description,
            String externalReference
    ) {}
}