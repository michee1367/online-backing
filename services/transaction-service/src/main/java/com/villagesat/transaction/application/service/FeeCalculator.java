package com.villagesat.transaction.application.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FeeCalculator {

    private static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.01");
    private static final BigDecimal MIN_FEE = new BigDecimal("100.00");

    public BigDecimal calculateTransferFee(BigDecimal amount) {
        BigDecimal fee = amount.multiply(TRANSFER_FEE_RATE).setScale(4, RoundingMode.HALF_UP);
        return fee.max(MIN_FEE);
    }
}
