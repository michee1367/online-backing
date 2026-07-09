package com.villagesat.mobilemoney.adapter.out.wallet;

import com.villagesat.mobilemoney.domain.port.out.WalletCreditPort;
import com.villagesat.mobilemoney.domain.port.out.WalletDebitPort;
import com.villagesat.mobilemoney.domain.port.out.RemoteInsufficientFundsException;
import com.villagesat.mobilemoney.domain.port.out.RemoteWalletUnavailableException;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Component
public class WalletServiceAdapter implements WalletCreditPort, WalletDebitPort {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceAdapter.class);
    private final RestClient restClient;

    public WalletServiceAdapter(
        RestClient.Builder restClientBuilder,
        @Value("${app.services.wallet-url}") String walletUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(walletUrl)
                .build();
    }

    @Override
    @Retry(name = "walletApiRetry")
    public void creditWallet(UUID walletId, BigDecimal amount, String currency, UUID reference) {
        log.info("Appel API Wallet (Crédit) — walletId={}, ref={}", walletId, reference);
        WalletTransactionRequest request = new WalletTransactionRequest(amount, currency, reference);

        restClient.post()
                .uri("/{walletId}/credit", walletId)
                .contentType(MediaType.APPLICATION_JSON)
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
    public void debitWallet(UUID walletId, BigDecimal amount, String currency, UUID reference) {
        log.info("Appel API Wallet (Débit) — walletId={}, ref={}", walletId, reference);
        WalletTransactionRequest request = new WalletTransactionRequest(amount, currency, reference);

        restClient.post()
                .uri("/{walletId}/debit", walletId)
                .contentType(MediaType.APPLICATION_JSON)
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
