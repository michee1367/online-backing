package com.villagesat.wallet.domain.model;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WalletAccountNumberGeneratorTest {

    @RepeatedTest(100)
    void generate_producesSixDigitsStartingWithOneToSix() {
        String number = WalletAccountNumberGenerator.generate();

        assertThat(number).hasSize(6);
        assertThat(number).matches("[1-6]\\d{5}");
        assertThat(WalletAccountNumberGenerator.isValid(number)).isTrue();
    }

    @Test
    void isValid_rejectsInvalidFormats() {
        assertThat(WalletAccountNumberGenerator.isValid(null)).isFalse();
        assertThat(WalletAccountNumberGenerator.isValid("012345")).isFalse();
        assertThat(WalletAccountNumberGenerator.isValid("712345")).isFalse();
        assertThat(WalletAccountNumberGenerator.isValid("12345")).isFalse();
        assertThat(WalletAccountNumberGenerator.isValid("1234567")).isFalse();
        assertThat(WalletAccountNumberGenerator.isValid("VS-123456")).isFalse();
    }

    @Test
    void isValid_acceptsValidNumbers() {
        assertThat(WalletAccountNumberGenerator.isValid("100000")).isTrue();
        assertThat(WalletAccountNumberGenerator.isValid("612345")).isTrue();
    }

    @RepeatedTest(20)
    void generate_producesVariedNumbers() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            generated.add(WalletAccountNumberGenerator.generate());
        }
        assertThat(generated.size()).isGreaterThan(40);
    }
}
