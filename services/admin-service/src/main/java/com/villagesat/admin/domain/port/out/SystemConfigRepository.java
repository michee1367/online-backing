package com.villagesat.admin.domain.port.out;

import com.villagesat.admin.domain.model.SystemConfig;

import java.util.List;
import java.util.Optional;

public interface SystemConfigRepository {

    List<SystemConfig> findAll();

    Optional<SystemConfig> findByKey(String key);

    SystemConfig save(SystemConfig config);
}
