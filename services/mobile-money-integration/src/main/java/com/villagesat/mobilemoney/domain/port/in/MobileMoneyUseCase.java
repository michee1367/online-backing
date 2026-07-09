package com.villagesat.mobilemoney.domain.port.in;

import com.villagesat.mobilemoney.domain.model.MobileMoneyTransaction;

import java.util.List;
import java.util.UUID;

public interface MobileMoneyUseCase {

    MobileMoneyTransaction initiateDeposit(DepositCommand command);

    MobileMoneyTransaction initiateWithdrawal(WithdrawalCommand command);

    MobileMoneyTransaction checkStatus(UUID transactionId);

    MobileMoneyTransaction handleCallback(CallbackCommand command);

    List<MobileMoneyTransaction> listTransactions(UUID userId);

    MobileMoneyTransaction getTransaction(UUID transactionId);
}
