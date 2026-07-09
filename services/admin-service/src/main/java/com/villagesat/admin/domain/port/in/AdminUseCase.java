package com.villagesat.admin.domain.port.in;

import com.villagesat.admin.domain.model.AccountAction;
import com.villagesat.admin.domain.model.BlacklistEntry;
import com.villagesat.admin.domain.model.SystemConfig;

import java.util.List;
import java.util.UUID;

public interface AdminUseCase {

    AccountAction performAccountAction(AccountAction action);

    List<AccountAction> getAccountActions(UUID userId);

    BlacklistEntry addToBlacklist(BlacklistEntry entry);

    void removeFromBlacklist(UUID id);

    List<BlacklistEntry> getBlacklist();

    List<SystemConfig> getSystemConfig();

    SystemConfig updateSystemConfig(String key, String value, UUID updatedBy);
}
