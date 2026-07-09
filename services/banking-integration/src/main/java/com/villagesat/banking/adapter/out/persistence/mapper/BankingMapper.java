package com.villagesat.banking.adapter.out.persistence.mapper;

import com.villagesat.banking.adapter.out.persistence.entity.BankTransferEntity;
import com.villagesat.banking.adapter.out.persistence.entity.LinkedBankAccountEntity;
import com.villagesat.banking.domain.model.AccountStatus;
import com.villagesat.banking.domain.model.BankTransfer;
import com.villagesat.banking.domain.model.BankTransferType;
import com.villagesat.banking.domain.model.LinkedBankAccount;
import com.villagesat.banking.domain.model.TransferStatus;

public final class BankingMapper {

    private BankingMapper() {
    }

    public static LinkedBankAccount toDomain(LinkedBankAccountEntity entity) {
        LinkedBankAccount account = new LinkedBankAccount();
        account.setId(entity.getId());
        account.setUserId(entity.getUserId());
        account.setBankName(entity.getBankName());
        account.setBankCode(entity.getBankCode());
        account.setAccountNumberEncrypted(entity.getAccountNumberEncrypted());
        account.setAccountHolderName(entity.getAccountHolderName());
        account.setCurrency(entity.getCurrency());
        account.setStatus(AccountStatus.valueOf(entity.getStatus()));
        account.setCreatedAt(entity.getCreatedAt());
        account.setUpdatedAt(entity.getUpdatedAt());
        account.setVersion(entity.getVersion());
        return account;
    }

    public static LinkedBankAccountEntity toEntity(LinkedBankAccount account) {
        LinkedBankAccountEntity entity = new LinkedBankAccountEntity();
        entity.setId(account.getId());
        entity.setUserId(account.getUserId());
        entity.setBankName(account.getBankName());
        entity.setBankCode(account.getBankCode());
        entity.setAccountNumberEncrypted(account.getAccountNumberEncrypted());
        entity.setAccountHolderName(account.getAccountHolderName());
        entity.setCurrency(account.getCurrency());
        entity.setStatus(account.getStatus().name());
        entity.setCreatedAt(account.getCreatedAt());
        entity.setUpdatedAt(account.getUpdatedAt());
        entity.setVersion(account.getVersion());
        return entity;
    }

    public static BankTransfer toDomain(BankTransferEntity entity) {
        BankTransfer transfer = new BankTransfer();
        transfer.setId(entity.getId());
        transfer.setUserId(entity.getUserId());
        transfer.setWalletId(entity.getWalletId());
        transfer.setLinkedAccountId(entity.getLinkedAccountId());
        transfer.setTransferType(BankTransferType.valueOf(entity.getTransferType()));
        transfer.setAmount(entity.getAmount());
        transfer.setCurrency(entity.getCurrency());
        transfer.setStatus(TransferStatus.valueOf(entity.getStatus()));
        transfer.setSwiftCode(entity.getSwiftCode());
        transfer.setReference(entity.getReference());
        transfer.setExternalRef(entity.getExternalRef());
        transfer.setCreatedAt(entity.getCreatedAt());
        transfer.setCompletedAt(entity.getCompletedAt());
        transfer.setFailedReason(entity.getFailedReason());
        transfer.setVersion(entity.getVersion());
        return transfer;
    }

    public static BankTransferEntity toEntity(BankTransfer transfer) {
        BankTransferEntity entity = new BankTransferEntity();
        entity.setId(transfer.getId());
        entity.setUserId(transfer.getUserId());
        entity.setWalletId(transfer.getWalletId());
        entity.setLinkedAccountId(transfer.getLinkedAccountId());
        entity.setTransferType(transfer.getTransferType().name());
        entity.setAmount(transfer.getAmount());
        entity.setCurrency(transfer.getCurrency());
        entity.setStatus(transfer.getStatus().name());
        entity.setSwiftCode(transfer.getSwiftCode());
        entity.setReference(transfer.getReference());
        entity.setExternalRef(transfer.getExternalRef());
        entity.setCreatedAt(transfer.getCreatedAt());
        entity.setCompletedAt(transfer.getCompletedAt());
        entity.setFailedReason(transfer.getFailedReason());
        entity.setVersion(transfer.getVersion());
        return entity;
    }
}
