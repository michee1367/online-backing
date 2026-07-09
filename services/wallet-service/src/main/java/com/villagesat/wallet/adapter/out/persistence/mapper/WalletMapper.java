package com.villagesat.wallet.adapter.out.persistence.mapper;

import com.villagesat.wallet.adapter.out.persistence.entity.BalanceEntity;
import com.villagesat.wallet.adapter.out.persistence.entity.WalletEntity;
import com.villagesat.wallet.domain.model.Balance;
import com.villagesat.wallet.domain.model.Wallet;

public final class WalletMapper {

    private WalletMapper() {}

    public static Wallet toDomain(WalletEntity entity) {
        return new Wallet(
                entity.getId(),
                entity.getUserId(),
                entity.getAccountNumber(),
                entity.getCurrency(),
                Wallet.WalletType.valueOf(entity.getType().name()),
                entity.getLabel(),
                Wallet.WalletStatus.valueOf(entity.getStatus().name()),
                entity.getKycLevel(),
                entity.getDailyLimit(),
                entity.getMonthlyLimit(),
                entity.getCreatedAt(),
                entity.getVersion() != null ? entity.getVersion() : 0L
        );
    }

    public static WalletEntity toEntity(Wallet domain) {
        WalletEntity entity = new WalletEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setAccountNumber(domain.accountNumber());
        entity.setCurrency(domain.currency());
        entity.setType(WalletEntity.WalletTypeEntity.valueOf(domain.type().name()));
        entity.setLabel(domain.label());
        entity.setStatus(WalletEntity.WalletStatusEntity.valueOf(domain.status().name()));
        entity.setKycLevel((short) domain.kycLevel());
        entity.setDailyLimit(domain.dailyLimit());
        entity.setMonthlyLimit(domain.monthlyLimit());
        entity.setCreatedAt(domain.createdAt());
        if (domain.version() > 0) {
            entity.setVersion(domain.version());
        }
        return entity;
    }

    public static Balance toDomain(BalanceEntity entity) {
        return new Balance(
                entity.getWalletId(),
                entity.getBalance(),
                entity.getAvailableBalance(),
                entity.getPendingBalance(),
                entity.getLastTransactionAt(),
                entity.getVersion() != null ? entity.getVersion() : 0L
        );
    }

    public static BalanceEntity toEntity(Balance domain) {
        BalanceEntity entity = new BalanceEntity();
        entity.setWalletId(domain.walletId());
        entity.setBalance(domain.balance());
        entity.setAvailableBalance(domain.availableBalance());
        entity.setPendingBalance(domain.pendingBalance());
        entity.setLastTransactionAt(domain.lastTransactionAt());
        if (domain.version() > 0) {
            entity.setVersion(domain.version());
        }
        return entity;
    }

    public static void updateBalanceEntity(BalanceEntity entity, Balance domain) {
        entity.setBalance(domain.balance());
        entity.setAvailableBalance(domain.availableBalance());
        entity.setPendingBalance(domain.pendingBalance());
        entity.setLastTransactionAt(domain.lastTransactionAt());
    }
}
