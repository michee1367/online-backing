package com.villagesat.mobilemoney.domain.port.out;

import com.villagesat.mobilemoney.domain.model.MobileMoneyProvider;

import java.math.BigDecimal;
import java.util.UUID;

public interface MobileMoneyGatewayPort {

    GatewayResponse sendRequest(MobileMoneyProvider provider, GatewayRequest request);

    record GatewayRequest(
            String phoneNumber,
            BigDecimal amount,
            String currency,
            String externalRef,
            String transactionType
    ) {
    }

    record GatewayResponse(
            boolean success,
            String providerRef,
            String errorMessage
    ) {
    }
}
