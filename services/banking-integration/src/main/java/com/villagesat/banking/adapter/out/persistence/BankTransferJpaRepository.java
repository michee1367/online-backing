package com.villagesat.banking.adapter.out.persistence;

import com.villagesat.banking.adapter.out.persistence.entity.BankTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BankTransferJpaRepository extends JpaRepository<BankTransferEntity, UUID> {

    List<BankTransferEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
