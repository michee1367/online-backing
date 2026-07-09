package com.villagesat.banking.application.service;

import com.villagesat.banking.domain.model.AccountStatus;
import com.villagesat.banking.domain.model.BankTransfer;
import com.villagesat.banking.domain.model.LinkedBankAccount;
import com.villagesat.banking.domain.model.TransferStatus;
import com.villagesat.banking.domain.port.in.BankingUseCase;
import com.villagesat.banking.domain.port.in.InitiateTransferCommand;
import com.villagesat.banking.domain.port.in.LinkAccountCommand;
import com.villagesat.banking.domain.port.out.BankGatewayPort;
import com.villagesat.banking.domain.port.out.BankGatewayPort.TransferResponse;
import com.villagesat.banking.domain.port.out.BankTransferRepository;
import com.villagesat.banking.domain.port.out.BankingEventPublisher;
import com.villagesat.banking.domain.port.out.LinkedBankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BankingService implements BankingUseCase {

    private final LinkedBankAccountRepository accountRepository;
    private final BankTransferRepository transferRepository;
    private final BankGatewayPort bankGatewayPort;
    private final BankingEventPublisher eventPublisher;

    public BankingService(LinkedBankAccountRepository accountRepository,
                          BankTransferRepository transferRepository,
                          BankGatewayPort bankGatewayPort,
                          BankingEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.bankGatewayPort = bankGatewayPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public LinkedBankAccount linkAccount(LinkAccountCommand command) {
        LinkedBankAccount account = new LinkedBankAccount();
        account.setId(UUID.randomUUID());
        account.setUserId(command.userId());
        account.setBankName(command.bankName());
        account.setBankCode(command.bankCode());
        account.setAccountNumberEncrypted(encryptAccountNumber(command.accountNumber()));
        account.setAccountHolderName(command.accountHolderName());
        account.setCurrency(command.currency());
        account.setStatus(AccountStatus.PENDING_VERIFICATION);
        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        account.setVersion(0L);

        account = accountRepository.save(account);
        eventPublisher.publishAccountLinked(account);
        return account;
    }

    @Override
    public LinkedBankAccount verifyAccount(UUID accountId) {
        LinkedBankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        account.verify();
        return accountRepository.save(account);
    }

    @Override
    public void deleteAccount(UUID accountId, UUID userId) {
        LinkedBankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Account does not belong to user");
        }
        accountRepository.deleteById(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkedBankAccount> listAccounts(UUID userId) {
        return accountRepository.findByUserId(userId);
    }

    @Override
    public BankTransfer initiateTransfer(InitiateTransferCommand command) {
        LinkedBankAccount account = accountRepository.findById(command.linkedAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Linked account not found: " + command.linkedAccountId()));

        if (account.getStatus() != AccountStatus.VERIFIED) {
            throw new IllegalStateException("Account is not verified: " + account.getStatus());
        }

        BankTransfer transfer = new BankTransfer();
        transfer.setId(UUID.randomUUID());
        transfer.setUserId(command.userId());
        transfer.setWalletId(command.walletId());
        transfer.setLinkedAccountId(command.linkedAccountId());
        transfer.setTransferDirection(command.direction());
        transfer.setTransferType(command.transferType());
        transfer.setAmount(command.amount());
        transfer.setCurrency(command.currency());
        transfer.setStatus(TransferStatus.INITIATED);
        transfer.setSwiftCode(command.swiftCode());
        transfer.setReference(generateReference());
        transfer.setCreatedAt(Instant.now());
        transfer.setVersion(0L);

        transfer = transferRepository.save(transfer);

        TransferResponse response = bankGatewayPort.initiateSwiftTransfer(transfer);

        if (response.success()) {
            transfer.markProcessing(response.externalRef());
            transfer.markCompleted();
            eventPublisher.publishTransferCompleted(transfer);
        } else {
            transfer.markFailed(response.errorMessage());
            eventPublisher.publishTransferFailed(transfer);
        }

        return transferRepository.save(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public BankTransfer getTransfer(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankTransfer> listTransfers(UUID userId) {
        return transferRepository.findByUserId(userId);
    }

    private String encryptAccountNumber(String accountNumber) {
        return "ENC:" + accountNumber;
    }

    private String generateReference() {
        return "BT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
}
