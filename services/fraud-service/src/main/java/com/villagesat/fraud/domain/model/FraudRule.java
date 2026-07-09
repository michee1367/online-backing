package com.villagesat.fraud.domain.model;

public sealed interface FraudRule permits
        FraudRule.AmountThreshold,
        FraudRule.Velocity,
        FraudRule.LargeTransfer,
        FraudRule.NewAccount,
        FraudRule.CrossBorder,
        FraudRule.UnusualHour {

    String name();

    record AmountThreshold() implements FraudRule {
        @Override public String name() { return "AMOUNT_THRESHOLD"; }
    }

    record Velocity() implements FraudRule {
        @Override public String name() { return "VELOCITY"; }
    }

    record LargeTransfer() implements FraudRule {
        @Override public String name() { return "LARGE_TRANSFER"; }
    }

    record NewAccount() implements FraudRule {
        @Override public String name() { return "NEW_ACCOUNT"; }
    }

    record CrossBorder() implements FraudRule {
        @Override public String name() { return "CROSS_BORDER"; }
    }

    record UnusualHour() implements FraudRule {
        @Override public String name() { return "UNUSUAL_HOUR"; }
    }
}
