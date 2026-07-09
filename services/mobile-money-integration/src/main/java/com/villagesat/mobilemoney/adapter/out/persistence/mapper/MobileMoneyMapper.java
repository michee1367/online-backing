package com.villagesat.mobilemoney.adapter.out.persistence.mapper;

import com.villagesat.mobilemoney.adapter.out.persistence.entity.MobileMoneyTransactionEntity;
import com.villagesat.mobilemoney.adapter.out.persistence.entity.ProviderConfigEntity;
import com.villagesat.mobilemoney.domain.model.MobileMoneyProvider;
import com.villagesat.mobilemoney.domain.model.MobileMoneyTransaction;
import com.villagesat.mobilemoney.domain.model.ProviderConfig;
import com.villagesat.mobilemoney.domain.model.TransactionStatus;
import com.villagesat.mobilemoney.domain.model.TransactionType;

public final class MobileMoneyMapper {

    private MobileMoneyMapper() {
    }

    public static MobileMoneyTransaction toDomain(MobileMoneyTransactionEntity entity) {
        MobileMoneyTransaction tx = new MobileMoneyTransaction();
        tx.setId(entity.getId());
        tx.setUserId(entity.getUserId());
        tx.setWalletId(entity.getWalletId());
        tx.setProvider(MobileMoneyProvider.valueOf(entity.getProvider()));
        tx.setPhoneNumber(entity.getPhoneNumber());
        tx.setAmount(entity.getAmount());
        tx.setCurrency(entity.getCurrency());
        tx.setTransactionType(TransactionType.valueOf(entity.getTransactionType()));
        tx.setStatus(TransactionStatus.valueOf(entity.getStatus()));
        tx.setExternalRef(entity.getExternalRef());
        tx.setProviderRef(entity.getProviderRef());
        tx.setCreatedAt(entity.getCreatedAt());
        tx.setCompletedAt(entity.getCompletedAt());
        tx.setFailedReason(entity.getFailedReason());
        tx.setVersion(entity.getVersion());
        return tx;
    }

    public static MobileMoneyTransactionEntity toEntity(MobileMoneyTransaction tx) {
        MobileMoneyTransactionEntity entity = new MobileMoneyTransactionEntity();
        entity.setId(tx.getId());
        entity.setUserId(tx.getUserId());
        entity.setWalletId(tx.getWalletId());
        entity.setProvider(tx.getProvider().name());
        entity.setPhoneNumber(tx.getPhoneNumber());
        entity.setAmount(tx.getAmount());
        entity.setCurrency(tx.getCurrency());
        entity.setTransactionType(tx.getTransactionType().name());
        entity.setStatus(tx.getStatus().name());
        entity.setExternalRef(tx.getExternalRef());
        entity.setProviderRef(tx.getProviderRef());
        entity.setCreatedAt(tx.getCreatedAt());
        entity.setCompletedAt(tx.getCompletedAt());
        entity.setFailedReason(tx.getFailedReason());
        entity.setVersion(tx.getVersion());
        return entity;
    }

    public static ProviderConfig toDomain(ProviderConfigEntity entity) {
        ProviderConfig config = new ProviderConfig();
        config.setProvider(MobileMoneyProvider.valueOf(entity.getProvider()));
        config.setApiUrl(entity.getApiUrl());
        config.setApiKeyEncrypted(entity.getApiKeyEncrypted());
        config.setMerchantId(entity.getMerchantId());
        config.setCallbackUrl(entity.getCallbackUrl());
        config.setActive(entity.isActive());
        config.setUpdatedAt(entity.getUpdatedAt());
        return config;
    }
}
