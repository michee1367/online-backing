package com.villagesat.banking.adapter.out.persistence;

import com.villagesat.banking.adapter.out.persistence.entity.LinkedBankAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LinkedBankAccountJpaRepository extends JpaRepository<LinkedBankAccountEntity, UUID> {

    List<LinkedBankAccountEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
