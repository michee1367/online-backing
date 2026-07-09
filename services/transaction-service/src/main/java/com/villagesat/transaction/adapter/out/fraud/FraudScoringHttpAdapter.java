package com.villagesat.transaction.adapter.out.fraud;

import com.villagesat.transaction.domain.port.out.FraudScoringPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Component
public class FraudScoringHttpAdapter implements FraudScoringPort {

    private static final Logger log = LoggerFactory.getLogger(FraudScoringHttpAdapter.class);

    private final WebClient webClient;
    private final long timeoutMs;

    public FraudScoringHttpAdapter(WebClient.Builder builder,
                                   @Value("${villagesat.fraud-service.url}") String fraudServiceUrl,
                                   @Value("${villagesat.fraud-service.timeout-ms:200}") long timeoutMs) {
        this.webClient = builder.baseUrl(fraudServiceUrl).build();
        this.timeoutMs = timeoutMs;
    }

    @Override
    public FraudResult score(FraudRequest request) {
        try {
            FraudResponse response = webClient.post()
                    .uri("/internal/fraud/score")
                    .bodyValue(new FraudScoreRequest(
                            request.userId(), request.walletId(),
                            request.amount().toPlainString(), request.currency()))
                    .retrieve()
                    .bodyToMono(FraudResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .retryWhen(Retry.max(1))
                    .block();

            if (response == null) {
                return allowFallback();
            }
            return new FraudResult(response.score(), FraudAction.valueOf(response.action()));
        } catch (Exception e) {
            log.warn("Fraud service unavailable, allowing transaction: {}", e.getMessage());
            return allowFallback();
        }
    }

    private FraudResult allowFallback() {
        return new FraudResult(0, FraudAction.ALLOW);
    }

    record FraudScoreRequest(UUID userId, UUID walletId, String amount, String currency) {}
    record FraudResponse(int score, String action) {}
}
