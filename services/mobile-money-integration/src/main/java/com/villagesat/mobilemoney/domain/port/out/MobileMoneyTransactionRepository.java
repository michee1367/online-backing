package com.villagesat.mobilemoney.domain.port.out;

import com.villagesat.mobilemoney.domain.model.MobileMoneyTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MobileMoneyTransactionRepository {

    MobileMoneyTransaction save(MobileMoneyTransaction transaction);

    Optional<MobileMoneyTransaction> findById(UUID id);

    Optional<MobileMoneyTransaction> findByExternalRef(String externalRef);

    List<MobileMoneyTransaction> findByUserId(UUID userId);
}
