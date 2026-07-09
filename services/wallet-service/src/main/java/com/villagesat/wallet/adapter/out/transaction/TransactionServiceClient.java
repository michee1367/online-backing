package com.villagesat.wallet.adapter.out.transaction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Relais HTTP vers transaction-service (BFF mobile : une seule base /wallet/).
 */
@Component
public class TransactionServiceClient {

    private final RestClient restClient;

    public TransactionServiceClient(RestClient.Builder builder,
                                    @Value("${villagesat.transaction-service.url}") String transactionServiceUrl) {
        this.restClient = builder.baseUrl(transactionServiceUrl).build();
    }

    public ResponseEntity<String> forwardTransfer(String authorization, UUID idempotencyKey, String body) {
        return restClient.post()
                .uri("/api/v1/transactions/transfer")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header("Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((request, response) -> {
                    byte[] bytes = response.getBody().readAllBytes();
                    String responseBody = bytes.length > 0 ? new String(bytes, StandardCharsets.UTF_8) : "";
                    MediaType contentType = response.getHeaders().getContentType();
                    ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.getStatusCode());
                    if (contentType != null) {
                        builder.contentType(contentType);
                    }
                    return builder.body(responseBody);
                });
    }
}
