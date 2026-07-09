package com.villagesat.fraud.application.rules;

import com.villagesat.fraud.domain.model.FraudScoreRequest;
import com.villagesat.fraud.domain.model.RuleResult;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class UnusualHourRule implements ScoringRule {

    @Override
    public RuleResult evaluate(FraudScoreRequest request) {
        ZonedDateTime utcTime = request.timestamp().atZone(ZoneOffset.UTC);
        int hour = utcTime.getHour();
        if (hour >= 1 && hour < 5) {
            return new RuleResult("UNUSUAL_HOUR", 10,
                    "Transaction à %02d:%02d UTC (plage suspecte 01:00-05:00)".formatted(hour, utcTime.getMinute()));
        }
        return RuleResult.NONE;
    }
}
