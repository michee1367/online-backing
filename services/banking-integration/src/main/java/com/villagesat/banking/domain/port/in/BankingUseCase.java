package com.villagesat.banking.domain.port.in;

import com.villagesat.banking.domain.model.BankTransfer;
import com.villagesat.banking.domain.model.LinkedBankAccount;

import java.util.List;
import java.util.UUID;

public interface BankingUseCase {

    LinkedBankAccount linkAccount(LinkAccountCommand command);

    LinkedBankAccount verifyAccount(UUID accountId);

    void deleteAccount(UUID accountId, UUID userId);

    List<LinkedBankAccount> listAccounts(UUID userId);

    BankTransfer initiateTransfer(InitiateTransferCommand command);

    BankTransfer getTransfer(UUID transferId);

    List<BankTransfer> listTransfers(UUID userId);
}
