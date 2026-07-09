package com.villagesat.wallet.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.villagesat.wallet.support.WalletTestFixtures.l0Wallet;
import static org.assertj.core.api.Assertions.assertThat;

class WalletKycLevelTest {

    @Test
    void applyKycLevel_updatesLimitsAndLevel() {
        Wallet wallet = l0Wallet(UUID.randomUUID());

        Wallet upgraded = wallet.applyKycLevel(1);

        assertThat(upgraded.kycLevel()).isEqualTo(1);
        assertThat(upgraded.dailyLimit()).isEqualByComparingTo("500");
        assertThat(upgraded.monthlyLimit()).isEqualByComparingTo("5000");
        assertThat(upgraded.id()).isEqualTo(wallet.id());
        assertThat(upgraded.status()).isEqualTo(wallet.status());
    }
}
