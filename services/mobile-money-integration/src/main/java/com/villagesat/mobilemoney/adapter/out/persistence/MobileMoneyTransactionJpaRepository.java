package com.villagesat.mobilemoney.adapter.out.persistence;

import com.villagesat.mobilemoney.adapter.out.persistence.entity.MobileMoneyTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MobileMoneyTransactionJpaRepository extends JpaRepository<MobileMoneyTransactionEntity, UUID> {

    Optional<MobileMoneyTransactionEntity> findByExternalRef(String externalRef);

    List<MobileMoneyTransactionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
