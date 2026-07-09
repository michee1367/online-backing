package com.villagesat.mobilemoney.adapter.out.persistence;

import com.villagesat.mobilemoney.adapter.out.persistence.mapper.MobileMoneyMapper;
import com.villagesat.mobilemoney.domain.model.MobileMoneyProvider;
import com.villagesat.mobilemoney.domain.model.ProviderConfig;
import com.villagesat.mobilemoney.domain.port.out.ProviderConfigRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProviderConfigRepositoryAdapter implements ProviderConfigRepository {

    private final ProviderConfigJpaRepository jpaRepository;

    public ProviderConfigRepositoryAdapter(ProviderConfigJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ProviderConfig> findByProvider(MobileMoneyProvider provider) {
        return jpaRepository.findById(provider.name()).map(MobileMoneyMapper::toDomain);
    }

    @Override
    public List<ProviderConfig> findAllActive() {
        return jpaRepository.findByActiveTrue()
                .stream()
                .map(MobileMoneyMapper::toDomain)
                .toList();
    }
}
