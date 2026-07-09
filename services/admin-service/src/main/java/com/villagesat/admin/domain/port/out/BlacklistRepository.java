package com.villagesat.admin.domain.port.out;

import com.villagesat.admin.domain.model.BlacklistEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlacklistRepository {

    BlacklistEntry save(BlacklistEntry entry);

    Optional<BlacklistEntry> findById(UUID id);

    List<BlacklistEntry> findAllActive();

    void deactivate(UUID id);
}
