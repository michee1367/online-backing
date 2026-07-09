package com.villagesat.wallet.domain.model;

import java.security.SecureRandom;

/**
 * Génère un numéro wallet mémorable : 6 chiffres, premier chiffre entre 1 et 6.
 * Espace : 600 000 combinaisons (6 × 10⁵).
 */
public final class WalletAccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PATTERN = "[1-6]\\d{5}";

    private WalletAccountNumberGenerator() {}

    public static String generate() {
        int firstDigit = 1 + RANDOM.nextInt(6);
        int suffix = RANDOM.nextInt(100_000);
        return "%d%05d".formatted(firstDigit, suffix);
    }

    public static boolean isValid(String accountNumber) {
        return accountNumber != null && accountNumber.matches(PATTERN);
    }
}
