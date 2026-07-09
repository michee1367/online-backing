package com.villagesat.admin.adapter.out.persistence;

import com.villagesat.admin.adapter.out.persistence.entity.AccountActionEntity;
import com.villagesat.admin.domain.model.AccountAction;
import com.villagesat.admin.domain.model.ActionType;
import com.villagesat.admin.domain.port.out.AccountActionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AccountActionRepositoryAdapter implements AccountActionRepository {

    private final AccountActionJpaRepository jpaRepository;

    public AccountActionRepositoryAdapter(AccountActionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AccountAction save(AccountAction action) {
        AccountActionEntity entity = toEntity(action);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<AccountAction> findByTargetUserId(UUID userId) {
        return jpaRepository.findByTargetUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private AccountActionEntity toEntity(AccountAction action) {
        AccountActionEntity entity = new AccountActionEntity();
        entity.setId(action.getId());
        entity.setTargetUserId(action.getTargetUserId());
        entity.setActionType(action.getActionType().name());
        entity.setPerformedBy(action.getPerformedBy());
        entity.setReason(action.getReason());
        entity.setCreatedAt(action.getCreatedAt());
        return entity;
    }

    private AccountAction toDomain(AccountActionEntity entity) {
        return new AccountAction(
                entity.getId(),
                entity.getTargetUserId(),
                ActionType.valueOf(entity.getActionType()),
                entity.getPerformedBy(),
                entity.getReason(),
                entity.getCreatedAt()
        );
    }
}
