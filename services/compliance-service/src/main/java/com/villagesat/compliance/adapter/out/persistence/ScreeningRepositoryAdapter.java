package com.villagesat.compliance.adapter.out.persistence;

import com.villagesat.compliance.adapter.out.persistence.mapper.ComplianceMapper;
import com.villagesat.compliance.domain.model.Screening;
import com.villagesat.compliance.domain.port.out.ScreeningRepository;
import org.springframework.stereotype.Component;

@Component
public class ScreeningRepositoryAdapter implements ScreeningRepository {

    private final ScreeningJpaRepository jpaRepository;

    public ScreeningRepositoryAdapter(ScreeningJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Screening save(Screening screening) {
        return ComplianceMapper.toDomain(jpaRepository.save(ComplianceMapper.toEntity(screening)));
    }
}
