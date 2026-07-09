package com.villagesat.banking.domain.port.out;

import com.villagesat.banking.domain.model.LinkedBankAccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkedBankAccountRepository {

    LinkedBankAccount save(LinkedBankAccount account);

    Optional<LinkedBankAccount> findById(UUID id);

    List<LinkedBankAccount> findByUserId(UUID userId);

    void deleteById(UUID id);
}
