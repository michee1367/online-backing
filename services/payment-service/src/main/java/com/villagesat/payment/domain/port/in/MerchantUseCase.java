package com.villagesat.payment.domain.port.in;

import com.villagesat.payment.domain.model.Merchant;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface MerchantUseCase {

    Merchant registerMerchant(RegisterMerchantCommand command);

    Merchant getMerchant(UUID merchantId);

    List<Merchant> listByUserId(UUID userId);

    Merchant updateStatus(UUID merchantId, Merchant.MerchantStatus status);

    record RegisterMerchantCommand(
            UUID userId,
            String businessName,
            String businessType,
            String contactEmail,
            String contactPhone,
            String callbackUrl
    ) {}
}
