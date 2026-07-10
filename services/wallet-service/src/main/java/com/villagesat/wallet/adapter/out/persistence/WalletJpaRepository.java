package com.villagesat.wallet.adapter.out.persistence;

import com.villagesat.wallet.adapter.out.persistence.entity.WalletEntity;
import com.villagesat.wallet.domain.model.Wallet;

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

    Optional<Wallet> findByUserIdAndCurrency(UUID userId, String currency);

    @Query("SELECT w FROM WalletEntity w WHERE w.id = :id")
    @org.springframework.data.jpa.repository.Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WalletEntity> findByIdForUpdate(@Param("id") UUID id);
}
