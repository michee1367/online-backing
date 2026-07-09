package com.villagesat.wallet.adapter.out.persistence;

import com.villagesat.wallet.adapter.out.persistence.entity.BalanceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BalanceJpaRepository extends JpaRepository<BalanceEntity, UUID> {

    List<BalanceEntity> findByWalletIdIn(Collection<UUID> walletIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BalanceEntity b WHERE b.walletId = :walletId")
    Optional<BalanceEntity> findByWalletIdForUpdate(@Param("walletId") UUID walletId);
}
