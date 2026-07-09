package com.villagesat.banking.domain.port.out;

import com.villagesat.banking.domain.model.BankTransfer;
import com.villagesat.banking.domain.model.LinkedBankAccount;

public interface BankingEventPublisher {

    void publishTransferCompleted(BankTransfer transfer);

    void publishTransferFailed(BankTransfer transfer);

    void publishAccountLinked(LinkedBankAccount account);
}
