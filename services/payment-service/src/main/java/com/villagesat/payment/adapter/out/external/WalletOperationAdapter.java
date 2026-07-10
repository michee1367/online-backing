package com.villagesat.payment.adapter.out.external;

import com.villagesat.payment.domain.port.out.WalletOperationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;


import com.villagesat.payment.domain.port.out.RemoteInsufficientFundsException;
import com.villagesat.payment.domain.port.out.RemoteWalletUnavailableException;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import org.springframework.beans.factory.annotation.Value;

/**
 * Adaptateur simulé pour les opérations wallet.
 * En production, cet adaptateur appellera le wallet-service via HTTP ou messaging.
 */
@Component
public class WalletOperationAdapter implements WalletOperationPort {

    private static final Logger log = LoggerFactory.getLogger(WalletOperationAdapter.class);
    private final RestClient restClient;
    private final String internalToken;

    public WalletOperationAdapter(
        RestClient.Builder restClientBuilder,
        @Value("${app.services.wallet-url}") String walletUrl,
        @Value("${villagesat.internal-service-token:dev-internal-token}") String internalToken
    ) {
        this.internalToken = internalToken;
        this.restClient = restClientBuilder
                .baseUrl(walletUrl)
                .build();
    }
    /* 
    @Override
    public void debitCustomer(UUID walletId, BigDecimal amount, UUID reference) {
        log.info("[SIMULATED] Debit customer wallet {}: amount={}, reference={}",
                walletId, amount, reference);
    }

    @Override
    public void creditMerchant(UUID merchantId, BigDecimal amount, UUID reference) {
        log.info("[SIMULATED] Credit merchant {}: amount={}, reference={}",
                merchantId, amount, reference);
    }*/


    @Override
    @Retry(name = "walletApiRetry")
    public void creditMerchant(UUID walletId, BigDecimal amount, String currency, UUID reference) {
        log.info("Appel API Wallet (Crédit) — walletId={}, ref={}", walletId, reference);
        WalletTransactionRequest request = new WalletTransactionRequest(amount, currency, reference);

        restClient.post()
                .uri("/{walletId}/credit", walletId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Service-Token", internalToken)
                .body(request)
                .retrieve()
                // 💡 Gestion globale ou fine des statuts d'erreur
                .onStatus(HttpStatusCode::isError, (req, response) -> {
                    handleWalletError(walletId, response.getStatusCode());
                })
                .toBodilessEntity();
    }

    @Override
    @Retry(name = "walletApiRetry")
    public void debitCustomer(UUID walletId, BigDecimal amount, String currency, UUID reference) {
        log.info("Appel API Wallet (Débit) — walletId={}, ref={}", walletId, reference);
        WalletTransactionRequest request = new WalletTransactionRequest(amount, currency, reference);

        restClient.post()
                .uri("/{walletId}/debit", walletId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Service-Token", internalToken)
                .body(request)
                .retrieve()
                // 💡 Interception spécifique des erreurs du Wallet
                .onStatus(HttpStatusCode::isError, (req, response) -> {
                    handleWalletError(walletId, response.getStatusCode());
                })
                .toBodilessEntity();
    }

    /**
     * Convertit les codes d'erreur HTTP de l'API Wallet en exceptions métier claires.
     */
    private void handleWalletError(UUID walletId, HttpStatusCode statusCode) throws java.io.IOException {
        int code = statusCode.value();
        log.warn("L'API Wallet a renvoyé une erreur HTTP {} pour le wallet {}", code, walletId);

        // 💡 400 Bad Request : Généralement un solde insuffisant ou une validation de limite échouée
        if (code == 400) {
            throw new RemoteInsufficientFundsException(walletId);
        }
        
        // 💡 423 Locked / 403 Forbidden : Portefeuille gelé ou bloqué
        if (code == 423 || code == 403) {
            throw new RemoteWalletUnavailableException(walletId, "Portefeuille gelé ou restreint applicativement");
        }

        // 💡 404 Not Found : Le portefeuille n'existe pas
        if (code == 404) {
            throw new RemoteWalletUnavailableException(walletId, "Portefeuille introuvable côté distant");
        }

        // Laisse passer les autres codes (ex: 500) pour que Resilience4j sache qu'il doit déclencher un Retry
        throw new org.springframework.web.client.HttpServerErrorException(statusCode, "Erreur interne du Wallet Service");
    }

    private record WalletTransactionRequest(BigDecimal amount, String currency, UUID reference) {}


}
