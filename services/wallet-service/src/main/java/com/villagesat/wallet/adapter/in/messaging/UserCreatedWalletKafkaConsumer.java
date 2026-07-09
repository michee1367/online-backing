package com.villagesat.wallet.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.wallet.application.service.WalletService;
import com.villagesat.wallet.domain.model.Wallet;
import com.villagesat.wallet.domain.port.in.WalletUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Provisionne un wallet CDF par défaut (plafonds KYC L0) à l'inscription.
 */
@Component
public class UserCreatedWalletKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserCreatedWalletKafkaConsumer.class);

    private final WalletUseCase walletUseCase;
    private final ObjectMapper objectMapper;

    public UserCreatedWalletKafkaConsumer(WalletUseCase walletUseCase, ObjectMapper objectMapper) {
        this.walletUseCase = walletUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "user.events", groupId = "wallet-service-user")
    public void onUserEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (!"user.created".equals(root.path("eventType").asText())) {
                return;
            }
            JsonNode payload = root.path("payload");
            UUID userId = UUID.fromString(payload.path("userId").asText());
            String countryCode = payload.path("countryCode").asText("CD");
            String currency = "CD".equalsIgnoreCase(countryCode) ? "CDF" : "USD";

            walletUseCase.createWallet(new WalletUseCase.CreateWalletCommand(
                    userId, currency, Wallet.WalletType.PERSONAL, "Principal"));
            log.info("Provisioned default {} wallet for user {}", currency, userId);
        } catch (WalletService.DuplicateWalletException e) {
            log.debug("Default wallet already exists: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to provision wallet from user event: {}", message, e);
        }
    }
}
