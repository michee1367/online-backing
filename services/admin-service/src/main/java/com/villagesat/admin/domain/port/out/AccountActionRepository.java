package com.villagesat.admin.domain.port.out;

import com.villagesat.admin.domain.model.AccountAction;

import java.util.List;
import java.util.UUID;

public interface AccountActionRepository {

    AccountAction save(AccountAction action);

    List<AccountAction> findByTargetUserId(UUID userId);
}
