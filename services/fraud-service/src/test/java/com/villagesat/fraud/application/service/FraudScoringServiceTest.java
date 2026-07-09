package com.villagesat.fraud.application.service;

import com.villagesat.fraud.application.rules.*;
import com.villagesat.fraud.config.FraudProperties;
import com.villagesat.fraud.domain.model.FraudAlert;
import com.villagesat.fraud.domain.model.FraudScoreRequest;
import com.villagesat.fraud.domain.model.FraudScoreResult;
import com.villagesat.fraud.domain.port.out.FraudAlertRepository;
import com.villagesat.fraud.domain.port.out.FraudEventPublisher;
import com.villagesat.fraud.domain.port.out.TransactionHistoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.villagesat.fraud.support.FraudTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudScoringServiceTest {

    @Mock
    FraudAlertRepository alertRepository;

    @Mock
    FraudEventPublisher eventPublisher;

    @Mock
    TransactionHistoryPort transactionHistory;

    FraudScoringService scoringService;

    @BeforeEach
    void setUp() {
        FraudProperties properties = new FraudProperties();
        properties.setAmountThreshold(10_000);
        properties.setLargeTransferThreshold(50_000);
        properties.setVelocityLimit(5);
        properties.setVelocityWindowHours(1);

        List<ScoringRule> rules = List.of(
                new AmountThresholdRule(properties),
                new VelocityRule(transactionHistory, properties),
                new LargeTransferRule(properties),
                new NewAccountRule(),
                new CrossBorderRule(),
                new UnusualHourRule()
        );

        FraudRuleEngine ruleEngine = new FraudRuleEngine(rules);
        scoringService = new FraudScoringService(ruleEngine, alertRepository, eventPublisher);
    }

    @Test
    void lowAmount_localCurrency_normalHour_returnsAllow() {
        when(transactionHistory.getRecentTransactionCount(eq(USER_ID), anyInt())).thenReturn(1);

        FraudScoreResult result = scoringService.score(lowAmountRequest());

        assertThat(result.score()).isZero();
        assertThat(result.action().name()).isEqualTo("ALLOW");
        assertThat(result.rulesFired()).isEmpty();
        verify(alertRepository, never()).save(any());
        verify(eventPublisher, never()).publishFraudAlert(any());
    }

    @Test
    void highAmount_createsAlertWithReview() {
        when(transactionHistory.getRecentTransactionCount(eq(USER_ID), anyInt())).thenReturn(1);
        when(alertRepository.save(any(FraudAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        FraudScoreResult result = scoringService.score(highAmountRequest());

        assertThat(result.score()).isEqualTo(30);
        assertThat(result.action().name()).isEqualTo("REVIEW");
        assertThat(result.rulesFired()).contains("AMOUNT_THRESHOLD");

        ArgumentCaptor<FraudAlert> alertCaptor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertThat(alertCaptor.getValue().score()).isEqualTo(30);
        verify(eventPublisher, never()).publishFraudAlert(any());
    }

    @Test
    void largeTransfer_crossBorder_unusualHour_returnsBlockAndPublishesEvent() {
        when(transactionHistory.getRecentTransactionCount(eq(USER_ID), anyInt())).thenReturn(1);
        when(alertRepository.save(any(FraudAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        FraudScoreResult result = scoringService.score(largeTransferRequest());

        assertThat(result.score()).isGreaterThanOrEqualTo(80);
        assertThat(result.action().name()).isEqualTo("BLOCK");
        assertThat(result.rulesFired()).contains("AMOUNT_THRESHOLD", "LARGE_TRANSFER", "CROSS_BORDER", "UNUSUAL_HOUR");

        verify(alertRepository).save(any(FraudAlert.class));
        verify(eventPublisher).publishFraudAlert(any(FraudAlert.class));
    }

    @Test
    void highVelocity_lowAmount_triggersVelocityRuleOnly() {
        when(transactionHistory.getRecentTransactionCount(eq(USER_ID), anyInt())).thenReturn(8);

        FraudScoreResult result = scoringService.score(lowAmountRequest());

        assertThat(result.score()).isEqualTo(25);
        assertThat(result.action().name()).isEqualTo("ALLOW");
        assertThat(result.rulesFired()).contains("VELOCITY");
        verify(alertRepository, never()).save(any());
    }

    @Test
    void highVelocity_withHighAmount_createsAlertAndReview() {
        when(transactionHistory.getRecentTransactionCount(eq(USER_ID), anyInt())).thenReturn(8);
        when(alertRepository.save(any(FraudAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        FraudScoreResult result = scoringService.score(highAmountRequest());

        assertThat(result.score()).isEqualTo(55);
        assertThat(result.action().name()).isEqualTo("REVIEW");
        assertThat(result.rulesFired()).contains("VELOCITY", "AMOUNT_THRESHOLD");
        verify(alertRepository).save(any(FraudAlert.class));
    }
}
