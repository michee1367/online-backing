package com.villagesat.wallet.integration;

import com.villagesat.wallet.adapter.in.messaging.KycApprovedWalletKafkaConsumer;
import com.villagesat.wallet.config.WalletTestKafkaConfig;
import com.villagesat.wallet.domain.port.in.WalletUseCase;
import com.villagesat.wallet.domain.model.Wallet;
import com.villagesat.wallet.support.WalletTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles({"test", "dev"})
@Import(WalletTestKafkaConfig.class)
class KycKafkaConsumerIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("villagesat_wallet_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    KycApprovedWalletKafkaConsumer consumer;

    @Autowired
    WalletUseCase walletUseCase;

    @Test
    void onKycEvent_persistsUpgradedLimits() {
        walletUseCase.createWallet(new WalletUseCase.CreateWalletCommand(
                WalletTestFixtures.USER_ID, "EUR", Wallet.WalletType.PERSONAL, "EUR"));

        String message = """
                {
                  "eventType": "kyc.approved",
                  "payload": {
                    "userId": "%s",
                    "level": 1
                  }
                }
                """.formatted(WalletTestFixtures.USER_ID);

        consumer.onKycEvent(message);

        var wallets = walletUseCase.listWallets(WalletTestFixtures.USER_ID).stream()
                .filter(w -> "EUR".equals(w.currency()))
                .toList();

        assertThat(wallets).hasSize(1);
        assertThat(wallets.getFirst().kycLevel()).isEqualTo(1);
        assertThat(wallets.getFirst().dailyLimit()).isEqualByComparingTo("500");
    }
}
