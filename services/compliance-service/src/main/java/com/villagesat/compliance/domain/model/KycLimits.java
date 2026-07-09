package com.villagesat.compliance.domain.model;

import java.math.BigDecimal;

public record KycLimits(
        int level,
        BigDecimal dailyTransferLimit,
        BigDecimal monthlyTransferLimit,
        boolean internationalEnabled,
        boolean cardsEnabled
) {
    public static KycLimits forLevel(int level) {
        return switch (level) {
            case 0 -> new KycLimits(0, bd("200000"), bd("2000000"), false, false);
            case 1 -> new KycLimits(1, bd("500"), bd("5000"), false, false);
            case 2 -> new KycLimits(2, bd("5000"), bd("50000"), true, true);
            case 3 -> new KycLimits(3, bd("999999"), bd("9999999"), true, true);
            default -> new KycLimits(0, bd("200000"), bd("2000000"), false, false);
        };
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
