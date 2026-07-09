package com.villagesat.fraud.application.rules;

import com.villagesat.fraud.config.FraudProperties;
import com.villagesat.fraud.domain.model.FraudScoreRequest;
import com.villagesat.fraud.domain.model.RuleResult;
import com.villagesat.fraud.domain.port.out.TransactionHistoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import static com.villagesat.fraud.support.FraudTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleEngineTest {

    private FraudProperties properties;
    private TransactionHistoryPort transactionHistory;

    @BeforeEach
    void setUp() {
        properties = new FraudProperties();
        properties.setAmountThreshold(10_000);
        properties.setLargeTransferThreshold(50_000);
        properties.setVelocityLimit(5);
        properties.setVelocityWindowHours(1);
        transactionHistory = mock(TransactionHistoryPort.class);
    }

    @Test
    void amountThresholdRule_belowThreshold_doesNotTrigger() {
        var rule = new AmountThresholdRule(properties);
        RuleResult result = rule.evaluate(lowAmountRequest());
        assertThat(result.triggered()).isFalse();
    }

    @Test
    void amountThresholdRule_aboveThreshold_triggers() {
        var rule = new AmountThresholdRule(properties);
        RuleResult result = rule.evaluate(highAmountRequest());
        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(30);
        assertThat(result.ruleName()).isEqualTo("AMOUNT_THRESHOLD");
    }

    @Test
    void largeTransferRule_belowThreshold_doesNotTrigger() {
        var rule = new LargeTransferRule(properties);
        RuleResult result = rule.evaluate(highAmountRequest());
        assertThat(result.triggered()).isFalse();
    }

    @Test
    void largeTransferRule_aboveThreshold_triggers() {
        var rule = new LargeTransferRule(properties);
        RuleResult result = rule.evaluate(largeTransferRequest());
        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(40);
        assertThat(result.ruleName()).isEqualTo("LARGE_TRANSFER");
    }

    @Test
    void velocityRule_belowLimit_doesNotTrigger() {
        when(transactionHistory.getRecentTransactionCount(eq(USER_ID), anyInt())).thenReturn(3);
        var rule = new VelocityRule(transactionHistory, properties);
        RuleResult result = rule.evaluate(lowAmountRequest());
        assertThat(result.triggered()).isFalse();
    }

    @Test
    void velocityRule_aboveLimit_triggers() {
        when(transactionHistory.getRecentTransactionCount(eq(USER_ID), anyInt())).thenReturn(8);
        var rule = new VelocityRule(transactionHistory, properties);
        RuleResult result = rule.evaluate(lowAmountRequest());
        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(25);
        assertThat(result.ruleName()).isEqualTo("VELOCITY");
    }

    @Test
    void crossBorderRule_localCurrency_doesNotTrigger() {
        var rule = new CrossBorderRule();
        RuleResult result = rule.evaluate(lowAmountRequest());
        assertThat(result.triggered()).isFalse();
    }

    @Test
    void crossBorderRule_foreignCurrency_triggers() {
        var rule = new CrossBorderRule();
        RuleResult result = rule.evaluate(crossBorderRequest());
        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(10);
        assertThat(result.ruleName()).isEqualTo("CROSS_BORDER");
    }

    @Test
    void unusualHourRule_normalHour_doesNotTrigger() {
        var rule = new UnusualHourRule();
        RuleResult result = rule.evaluate(lowAmountRequest());
        assertThat(result.triggered()).isFalse();
    }

    @Test
    void unusualHourRule_suspiciousHour_triggers() {
        var rule = new UnusualHourRule();
        RuleResult result = rule.evaluate(unusualHourRequest());
        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(10);
        assertThat(result.ruleName()).isEqualTo("UNUSUAL_HOUR");
    }

    @Test
    void newAccountRule_currentlyAlwaysFalse() {
        var rule = new NewAccountRule();
        RuleResult result = rule.evaluate(lowAmountRequest());
        assertThat(result.triggered()).isFalse();
    }
}
