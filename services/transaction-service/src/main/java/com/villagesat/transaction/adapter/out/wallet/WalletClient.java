package com.villagesat.transaction.adapter.out.wallet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Client HTTP interne vers wallet-service (mTLS en production via service mesh).
 */
@Component
public class WalletClient {

    private final WebClient webClient;
    private final String internalToken;

    public WalletClient(WebClient.Builder builder,
                        @Value("${villagesat.wallet-service.url}") String walletServiceUrl,
                        @Value("${villagesat.internal-service-token:dev-internal-token}") String internalToken) {
        this.webClient = builder.baseUrl(walletServiceUrl).build();
        this.internalToken = internalToken;
    }

    public void debit(UUID walletId, UUID transactionId, BigDecimal amount, String description) {
        webClient.post()
                .uri("/internal/wallets/{walletId}/debit", walletId)
                .header("X-Internal-Service-Token", internalToken)
                .bodyValue(new WalletOperationRequest(transactionId, amount.toPlainString(), description))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public void credit(UUID walletId, UUID transactionId, BigDecimal amount, String description) {
        webClient.post()
                .uri("/internal/wallets/{walletId}/credit", walletId)
                .header("X-Internal-Service-Token", internalToken)
                .bodyValue(new WalletOperationRequest(transactionId, amount.toPlainString(), description))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public WalletResponse getWallet(UUID walletId) {
        return webClient.get()
                .uri("/internal/wallets/{walletId}", walletId)
                .header("X-Internal-Service-Token", internalToken)
                .retrieve()
                .bodyToMono(WalletResponse.class)
                .block();
    }

    record WalletOperationRequest(UUID transactionId, String amount, String description) {}

    record WalletResponse(UUID walletId, String currency) {}
}
