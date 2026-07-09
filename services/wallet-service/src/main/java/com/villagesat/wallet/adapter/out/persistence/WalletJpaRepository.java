package com.villagesat.wallet.adapter.out.persistence;

import com.villagesat.wallet.adapter.out.persistence.entity.WalletEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletJpaRepository extends JpaRepository<WalletEntity, UUID> {

    List<WalletEntity> findByUserId(UUID userId);

    Optional<WalletEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndCurrency(UUID userId, String currency);

    boolean existsByAccountNumber(String accountNumber);

    Optional<WalletEntity> findByAccountNumber(String accountNumber);

    @Query("SELECT w FROM WalletEntity w WHERE w.id = :id")
    @org.springframework.data.jpa.repository.Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WalletEntity> findByIdForUpdate(@Param("id") UUID id);
}
