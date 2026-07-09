package com.villagesat.admin.application.service;

import com.villagesat.admin.domain.model.AccountAction;
import com.villagesat.admin.domain.model.BlacklistEntry;
import com.villagesat.admin.domain.model.SystemConfig;
import com.villagesat.admin.domain.port.in.AdminUseCase;
import com.villagesat.admin.domain.port.out.AccountActionRepository;
import com.villagesat.admin.domain.port.out.BlacklistRepository;
import com.villagesat.admin.domain.port.out.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminService implements AdminUseCase {

    private final AccountActionRepository accountActionRepository;
    private final BlacklistRepository blacklistRepository;
    private final SystemConfigRepository systemConfigRepository;

    public AdminService(AccountActionRepository accountActionRepository,
                        BlacklistRepository blacklistRepository,
                        SystemConfigRepository systemConfigRepository) {
        this.accountActionRepository = accountActionRepository;
        this.blacklistRepository = blacklistRepository;
        this.systemConfigRepository = systemConfigRepository;
    }

    @Override
    public AccountAction performAccountAction(AccountAction action) {
        action.setId(UUID.randomUUID());
        action.setCreatedAt(Instant.now());
        return accountActionRepository.save(action);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountAction> getAccountActions(UUID userId) {
        return accountActionRepository.findByTargetUserId(userId);
    }

    @Override
    public BlacklistEntry addToBlacklist(BlacklistEntry entry) {
        entry.setId(UUID.randomUUID());
        entry.setCreatedAt(Instant.now());
        entry.setActive(true);
        return blacklistRepository.save(entry);
    }

    @Override
    public void removeFromBlacklist(UUID id) {
        blacklistRepository.deactivate(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlacklistEntry> getBlacklist() {
        return blacklistRepository.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfig> getSystemConfig() {
        return systemConfigRepository.findAll();
    }

    @Override
    public SystemConfig updateSystemConfig(String key, String value, UUID updatedBy) {
        SystemConfig config = systemConfigRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Config key not found: " + key));
        config.setValue(value);
        config.setUpdatedBy(updatedBy);
        config.setUpdatedAt(Instant.now());
        return systemConfigRepository.save(config);
    }
}
