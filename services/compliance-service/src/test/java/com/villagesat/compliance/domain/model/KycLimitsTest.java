package com.villagesat.compliance.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class KycLimitsTest {

    @ParameterizedTest
    @CsvSource({
            "0, 200000, 2000000, false, false",
            "1, 500, 5000, false, false",
            "2, 5000, 50000, true, true",
            "3, 999999, 9999999, true, true"
    })
    void forLevel_returnsExpectedLimits(int level, String daily, String monthly,
                                        boolean international, boolean cards) {
        KycLimits limits = KycLimits.forLevel(level);

        assertThat(limits.level()).isEqualTo(level);
        assertThat(limits.dailyTransferLimit()).isEqualByComparingTo(new BigDecimal(daily));
        assertThat(limits.monthlyTransferLimit()).isEqualByComparingTo(new BigDecimal(monthly));
        assertThat(limits.internationalEnabled()).isEqualTo(international);
        assertThat(limits.cardsEnabled()).isEqualTo(cards);
    }

    @Test
    void forLevel_unknownLevel_fallsBackToL0() {
        KycLimits limits = KycLimits.forLevel(99);

        assertThat(limits.level()).isZero();
        assertThat(limits.dailyTransferLimit()).isEqualByComparingTo("200000");
        assertThat(limits.monthlyTransferLimit()).isEqualByComparingTo("200");
        assertThat(limits.internationalEnabled()).isFalse();
        assertThat(limits.cardsEnabled()).isFalse();
    }

    @Test
    void forLevel_negativeLevel_fallsBackToL0() {
        KycLimits limits = KycLimits.forLevel(-1);

        assertThat(limits.level()).isZero();
        assertThat(limits.dailyTransferLimit()).isEqualByComparingTo("200000");
    }

    @Test
    void forLevel_L0_restrictedFeatures() {
        KycLimits limits = KycLimits.forLevel(0);

        assertThat(limits.internationalEnabled()).isFalse();
        assertThat(limits.cardsEnabled()).isFalse();
    }

    @Test
    void forLevel_L1_restrictedFeatures() {
        KycLimits limits = KycLimits.forLevel(1);

        assertThat(limits.internationalEnabled()).isFalse();
        assertThat(limits.cardsEnabled()).isFalse();
    }

    @Test
    void forLevel_L2_enablesAllFeatures() {
        KycLimits limits = KycLimits.forLevel(2);

        assertThat(limits.internationalEnabled()).isTrue();
        assertThat(limits.cardsEnabled()).isTrue();
    }

    @Test
    void forLevel_L3_enablesAllFeatures() {
        KycLimits limits = KycLimits.forLevel(3);

        assertThat(limits.internationalEnabled()).isTrue();
        assertThat(limits.cardsEnabled()).isTrue();
    }

    @Test
    void forLevel_L3_hasHighestLimits() {
        KycLimits l2 = KycLimits.forLevel(2);
        KycLimits l3 = KycLimits.forLevel(3);

        assertThat(l3.dailyTransferLimit()).isGreaterThan(l2.dailyTransferLimit());
        assertThat(l3.monthlyTransferLimit()).isGreaterThan(l2.monthlyTransferLimit());
    }
}
