package com.villagesat.admin.adapter.out.persistence;

import com.villagesat.admin.adapter.out.persistence.entity.AccountActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountActionJpaRepository extends JpaRepository<AccountActionEntity, UUID> {

    List<AccountActionEntity> findByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId);
}
