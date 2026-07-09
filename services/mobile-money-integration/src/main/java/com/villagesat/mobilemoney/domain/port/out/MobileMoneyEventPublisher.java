package com.villagesat.mobilemoney.domain.port.out;

import com.villagesat.mobilemoney.domain.model.MobileMoneyTransaction;

public interface MobileMoneyEventPublisher {

    void publishDeposit(MobileMoneyTransaction transaction);

    void publishWithdrawal(MobileMoneyTransaction transaction);
}
