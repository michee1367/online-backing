package com.villagesat.wallet.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.wallet.domain.model.Balance;
import com.villagesat.wallet.domain.model.Wallet;
import com.villagesat.wallet.domain.port.out.WalletEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class WalletKafkaEventPublisher implements WalletEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WalletKafkaEventPublisher.class);
    static final String TOPIC = "wallet.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public WalletKafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                     ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishWalletCreated(Wallet wallet) {
        publish(wallet.userId().toString(), "wallet.created", Map.of(
                "walletId", wallet.id(),
                "userId", wallet.userId(),
                "currency", wallet.currency(),
                "accountNumber", wallet.accountNumber(),
                "timestamp", Instant.now().toString()
        ));
    }

    @Override
    public void publishBalanceUpdated(UUID walletId, Balance balance) {
        publish(walletId.toString(), "wallet.balance.updated", Map.of(
                "walletId", walletId,
                "balance", balance.balance().toPlainString(),
                "availableBalance", balance.availableBalance().toPlainString(),
                "timestamp", Instant.now().toString()
        ));
    }

    @Override
    public void publishLimitsUpdated(Wallet wallet, int previousKycLevel) {
        publish(wallet.userId().toString(), "wallet.limits.updated", Map.of(
                "walletId", wallet.id(),
                "userId", wallet.userId(),
                "kycLevel", wallet.kycLevel(),
                "previousKycLevel", previousKycLevel,
                "dailyLimit", wallet.dailyLimit().toPlainString(),
                "monthlyLimit", wallet.monthlyLimit().toPlainString(),
                "currency", wallet.currency(),
                "timestamp", Instant.now().toString()
        ));
    }

    private void publish(String key, String eventType, Map<String, Object> payload) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "eventType", eventType,
                    "payload", payload
            ));
            kafkaTemplate.send(TOPIC, key, message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize wallet event {}", eventType, e);
        }
    }
}
