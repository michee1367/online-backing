package com.villagesat.wallet.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.wallet.domain.port.in.WalletKycUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KycApprovedWalletKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KycApprovedWalletKafkaConsumer.class);

    private final WalletKycUseCase walletKycUseCase;
    private final ObjectMapper objectMapper;

    public KycApprovedWalletKafkaConsumer(WalletKycUseCase walletKycUseCase, ObjectMapper objectMapper) {
        this.walletKycUseCase = walletKycUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "kyc.events", groupId = "wallet-service-kyc")
    public void onKycEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (!"kyc.approved".equals(root.path("eventType").asText())) {
                return;
            }
            JsonNode payload = root.path("payload");
            UUID userId = UUID.fromString(payload.path("userId").asText());
            int level = payload.path("level").asInt(1);

            walletKycUseCase.applyKycLimits(userId, level);
            log.info("Applied KYC limits level {} to wallets for user {}", level, userId);
        } catch (Exception e) {
            log.error("Failed to process KYC event for wallets: {}", message, e);
        }
    }
}
