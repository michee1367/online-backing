package com.villagesat.fraud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "villagesat.fraud")
public class FraudProperties {

    private long amountThreshold = 10_000;
    private long largeTransferThreshold = 50_000;
    private int velocityLimit = 5;
    private int velocityWindowHours = 1;

    public long getAmountThreshold() { return amountThreshold; }
    public void setAmountThreshold(long amountThreshold) { this.amountThreshold = amountThreshold; }
    public long getLargeTransferThreshold() { return largeTransferThreshold; }
    public void setLargeTransferThreshold(long largeTransferThreshold) { this.largeTransferThreshold = largeTransferThreshold; }
    public int getVelocityLimit() { return velocityLimit; }
    public void setVelocityLimit(int velocityLimit) { this.velocityLimit = velocityLimit; }
    public int getVelocityWindowHours() { return velocityWindowHours; }
    public void setVelocityWindowHours(int velocityWindowHours) { this.velocityWindowHours = velocityWindowHours; }
}
