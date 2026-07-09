package com.villagesat.mobilemoney.adapter.out.persistence;

import com.villagesat.mobilemoney.adapter.out.persistence.entity.ProviderConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderConfigJpaRepository extends JpaRepository<ProviderConfigEntity, String> {

    List<ProviderConfigEntity> findByActiveTrue();
}
