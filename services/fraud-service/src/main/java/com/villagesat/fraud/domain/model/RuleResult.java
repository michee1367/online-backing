package com.villagesat.fraud.domain.model;

public record RuleResult(
        String ruleName,
        int score,
        String reason
) {
    public static final RuleResult NONE = new RuleResult("", 0, "");

    public boolean triggered() {
        return score > 0;
    }
}
