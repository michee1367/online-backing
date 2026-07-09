package com.villagesat.user.adapter.out.persistence;

import com.villagesat.user.adapter.out.persistence.mapper.UserMapper;
import com.villagesat.user.domain.model.DataExportRequest;
import com.villagesat.user.domain.port.out.DataExportRepository;
import org.springframework.stereotype.Component;

@Component
public class DataExportRepositoryAdapter implements DataExportRepository {

    private final DataExportJpaRepository jpaRepository;

    public DataExportRepositoryAdapter(DataExportJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DataExportRequest save(DataExportRequest request) {
        return UserMapper.toDomain(jpaRepository.save(UserMapper.toEntity(request)));
    }
}
