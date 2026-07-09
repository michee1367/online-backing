package com.villagesat.wallet.domain.model;

import java.math.BigDecimal;

/**
 * Plafonds transactionnels par niveau KYC — alignés sur compliance-service.
 */
public record WalletKycLimits(
        int level,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        boolean internationalEnabled,
        boolean cardsEnabled
) {
    public static WalletKycLimits forLevel(int level) {
        return switch (level) {
            case 0 -> limits(0, "200000", "2000000", false, false);
            case 1 -> limits(1, "500", "5000", false, false);
            case 2 -> limits(2, "5000", "50000", true, true);
            case 3 -> limits(3, "999999", "9999999", true, true);
            default -> limits(0, "200000", "2000000", false, false);
        };
    }

    private static WalletKycLimits limits(int level, String daily, String monthly,
                                         boolean international, boolean cards) {
        return new WalletKycLimits(level, new BigDecimal(daily), new BigDecimal(monthly),
                international, cards);
    }
}
