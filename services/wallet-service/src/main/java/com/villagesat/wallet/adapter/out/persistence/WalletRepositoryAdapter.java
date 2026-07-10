package com.villagesat.wallet.adapter.out.persistence;

import com.villagesat.wallet.adapter.out.persistence.entity.WalletEntity;
import com.villagesat.wallet.adapter.out.persistence.mapper.WalletMapper;
import com.villagesat.wallet.domain.model.Wallet;
import com.villagesat.wallet.domain.port.out.WalletRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class WalletRepositoryAdapter implements WalletRepository {

    private final WalletJpaRepository jpaRepository;

    public WalletRepositoryAdapter(WalletJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Wallet save(Wallet wallet) {
        WalletEntity saved = jpaRepository.save(WalletMapper.toEntity(wallet));
        return WalletMapper.toDomain(saved);
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return jpaRepository.findById(id).map(WalletMapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(WalletMapper::toDomain);
    }

    @Override
    public List<Wallet> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream().map(WalletMapper::toDomain).toList();
    }

    @Override
    public boolean existsByUserIdAndCurrency(UUID userId, String currency) {
        return jpaRepository.existsByUserIdAndCurrency(userId, currency.toUpperCase());
    }
    @Override
    public Optional<Wallet> findByUserIdAndCurrency(UUID userId, String currency) {
        return jpaRepository.findByUserIdAndCurrency(userId, currency.toUpperCase());
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return jpaRepository.existsByAccountNumber(accountNumber);
    }
    

    @Override
    public Optional<Wallet> findByAccountNumber(String accountNumber) {
        return jpaRepository.findByAccountNumber(accountNumber).map(WalletMapper::toDomain);
    }
}
