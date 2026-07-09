package com.villagesat.mobilemoney.domain.port.out;

import com.villagesat.mobilemoney.domain.model.MobileMoneyProvider;
import com.villagesat.mobilemoney.domain.model.ProviderConfig;

import java.util.List;
import java.util.Optional;

public interface ProviderConfigRepository {

    Optional<ProviderConfig> findByProvider(MobileMoneyProvider provider);

    List<ProviderConfig> findAllActive();
}
