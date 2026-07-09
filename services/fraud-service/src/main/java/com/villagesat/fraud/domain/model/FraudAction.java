package com.villagesat.fraud.domain.model;

public enum FraudAction {
    ALLOW, REVIEW, STEP_UP_MFA, BLOCK;

    public static FraudAction fromScore(int score) {
        if (score >= 80) return BLOCK;
        if (score >= 60) return STEP_UP_MFA;
        if (score >= 30) return REVIEW;
        return ALLOW;
    }
}
