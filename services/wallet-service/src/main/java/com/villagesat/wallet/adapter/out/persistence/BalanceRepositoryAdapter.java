package com.villagesat.wallet.adapter.out.persistence;

import com.villagesat.wallet.adapter.out.persistence.entity.BalanceEntity;
import com.villagesat.wallet.adapter.out.persistence.mapper.WalletMapper;
import com.villagesat.wallet.domain.model.Balance;
import com.villagesat.wallet.domain.port.out.BalanceRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BalanceRepositoryAdapter implements BalanceRepository {

    private final BalanceJpaRepository jpaRepository;

    public BalanceRepositoryAdapter(BalanceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Balance save(Balance balance) {
        BalanceEntity entity = jpaRepository.findById(balance.walletId())
                .map(existing -> {
                    WalletMapper.updateBalanceEntity(existing, balance);
                    return existing;
                })
                .orElseGet(() -> WalletMapper.toEntity(balance));
        return WalletMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Balance> findByWalletId(UUID walletId) {
        return jpaRepository.findById(walletId).map(WalletMapper::toDomain);
    }

    @Override
    public Optional<Balance> findByWalletIdForUpdate(UUID walletId) {
        return jpaRepository.findByWalletIdForUpdate(walletId).map(WalletMapper::toDomain);
    }

    @Override
    public List<Balance> findByWalletIdIn(Collection<UUID> walletIds) {
        if (walletIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByWalletIdIn(walletIds).stream().map(WalletMapper::toDomain).toList();
    }
}
