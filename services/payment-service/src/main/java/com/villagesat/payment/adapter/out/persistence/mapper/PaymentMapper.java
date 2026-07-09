package com.villagesat.payment.adapter.out.persistence.mapper;

import com.villagesat.payment.adapter.out.persistence.entity.MerchantEntity;
import com.villagesat.payment.adapter.out.persistence.entity.PaymentEntity;
import com.villagesat.payment.domain.model.Merchant;
import com.villagesat.payment.domain.model.Merchant.MerchantStatus;
import com.villagesat.payment.domain.model.Payment;
import com.villagesat.payment.domain.model.Payment.PaymentMethod;
import com.villagesat.payment.domain.model.Payment.PaymentStatus;

public final class PaymentMapper {

    private PaymentMapper() {}

    public static Merchant toDomain(MerchantEntity entity) {
        return new Merchant(
                entity.getId(),
                entity.getUserId(),
                entity.getBusinessName(),
                entity.getBusinessType(),
                entity.getMerchantCode(),
                MerchantStatus.valueOf(entity.getStatus().name()),
                entity.getContactEmail(),
                entity.getContactPhone(),
                entity.getCallbackUrl(),
                entity.getCommissionRate(),
                entity.getCreatedAt(),
                entity.getVersion() != null ? entity.getVersion() : 0L
        );
    }

    public static MerchantEntity toEntity(Merchant domain) {
        MerchantEntity entity = new MerchantEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setBusinessName(domain.businessName());
        entity.setBusinessType(domain.businessType());
        entity.setMerchantCode(domain.merchantCode());
        entity.setStatus(MerchantEntity.MerchantStatusEntity.valueOf(domain.status().name()));
        entity.setContactEmail(domain.contactEmail());
        entity.setContactPhone(domain.contactPhone());
        entity.setCallbackUrl(domain.callbackUrl());
        entity.setCommissionRate(domain.commissionRate());
        entity.setCreatedAt(domain.createdAt());
        entity.setVersion(domain.version());
        return entity;
    }

    public static Payment toDomain(PaymentEntity entity) {
        return new Payment(
                entity.getId(),
                entity.getMerchantId(),
                entity.getCustomerId(),
                entity.getWalletId(),
                entity.getAmount(),
                entity.getFee(),
                entity.getCurrency(),
                PaymentStatus.valueOf(entity.getStatus().name()),
                PaymentMethod.valueOf(entity.getPaymentMethod().name()),
                entity.getReference(),
                entity.getDescription(),
                entity.getMerchantOrderId(),
                entity.getQrCodeData(),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getFailedReason(),
                entity.getVersion() != null ? entity.getVersion() : 0L
        );
    }

    public static PaymentEntity toEntity(Payment domain) {
        PaymentEntity entity = new PaymentEntity();
        entity.setId(domain.id());
        entity.setMerchantId(domain.merchantId());
        entity.setCustomerId(domain.customerId());
        entity.setWalletId(domain.walletId());
        entity.setAmount(domain.amount());
        entity.setFee(domain.fee());
        entity.setCurrency(domain.currency());
        entity.setStatus(PaymentEntity.PaymentStatusEntity.valueOf(domain.status().name()));
        entity.setPaymentMethod(PaymentEntity.PaymentMethodEntity.valueOf(domain.paymentMethod().name()));
        entity.setReference(domain.reference());
        entity.setDescription(domain.description());
        entity.setMerchantOrderId(domain.merchantOrderId());
        entity.setQrCodeData(domain.qrCodeData());
        entity.setCreatedAt(domain.createdAt());
        entity.setCompletedAt(domain.completedAt());
        entity.setFailedReason(domain.failedReason());
        entity.setVersion(domain.version());
        return entity;
    }
}
