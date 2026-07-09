package com.villagesat.banking.domain.port.out;

import com.villagesat.banking.domain.model.BankTransfer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankTransferRepository {

    BankTransfer save(BankTransfer transfer);

    Optional<BankTransfer> findById(UUID id);

    List<BankTransfer> findByUserId(UUID userId);
}
