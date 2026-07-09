package com.villagesat.transaction.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FeeCalculatorTest {

    private final FeeCalculator calculator = new FeeCalculator();

    @Test
    void calculateTransferFee_belowMinimum_appliesMinFee() {
        assertThat(calculator.calculateTransferFee(new BigDecimal("1000.0000")))
                .isEqualByComparingTo("100.0000");
    }

    @ParameterizedTest
    @CsvSource({
            "20000.0000, 200.0000",
            "50000.0000, 500.0000"
    })
    void calculateTransferFee_percentageApplied(String amount, String expectedFee) {
        assertThat(calculator.calculateTransferFee(new BigDecimal(amount)))
                .isEqualByComparingTo(expectedFee);
    }
}
