package com.villagesat.banking.adapter.out.persistence;

import com.villagesat.banking.adapter.out.persistence.mapper.BankingMapper;
import com.villagesat.banking.domain.model.LinkedBankAccount;
import com.villagesat.banking.domain.port.out.LinkedBankAccountRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class LinkedBankAccountRepositoryAdapter implements LinkedBankAccountRepository {

    private final LinkedBankAccountJpaRepository jpaRepository;

    public LinkedBankAccountRepositoryAdapter(LinkedBankAccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public LinkedBankAccount save(LinkedBankAccount account) {
        var entity = BankingMapper.toEntity(account);
        var saved = jpaRepository.save(entity);
        return BankingMapper.toDomain(saved);
    }

    @Override
    public Optional<LinkedBankAccount> findById(UUID id) {
        return jpaRepository.findById(id).map(BankingMapper::toDomain);
    }

    @Override
    public List<LinkedBankAccount> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(BankingMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
