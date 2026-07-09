package com.villagesat.wallet.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WalletKycLimitsTest {

    @ParameterizedTest
    @CsvSource({
            "0, 200000, 2000000",
            "1, 500, 5000",
            "2, 5000, 50000",
            "3, 999999, 9999999"
    })
    void forLevel_returnsExpectedLimits(int level, String daily, String monthly) {
        WalletKycLimits limits = WalletKycLimits.forLevel(level);

        assertThat(limits.level()).isEqualTo(level);
        assertThat(limits.dailyLimit()).isEqualByComparingTo(new BigDecimal(daily));
        assertThat(limits.monthlyLimit()).isEqualByComparingTo(new BigDecimal(monthly));
    }

    @Test
    void forLevel_unknownLevel_fallsBackToL0() {
        WalletKycLimits limits = WalletKycLimits.forLevel(99);

        assertThat(limits.level()).isZero();
        assertThat(limits.dailyLimit()).isEqualByComparingTo("200000");
        assertThat(limits.monthlyLimit()).isEqualByComparingTo("2000000");
        assertThat(limits.internationalEnabled()).isFalse();
    }

    @Test
    void forLevel_L2_enablesInternationalAndCards() {
        WalletKycLimits limits = WalletKycLimits.forLevel(2);

        assertThat(limits.internationalEnabled()).isTrue();
        assertThat(limits.cardsEnabled()).isTrue();
    }
}
