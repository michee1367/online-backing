package com.villagesat.payment.adapter.out.persistence;

import com.villagesat.payment.adapter.out.persistence.mapper.PaymentMapper;
import com.villagesat.payment.domain.model.Merchant;
import com.villagesat.payment.domain.port.out.MerchantRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MerchantRepositoryAdapter implements MerchantRepository {

    private final MerchantJpaRepository jpaRepository;

    public MerchantRepositoryAdapter(MerchantJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Merchant save(Merchant merchant) {
        return PaymentMapper.toDomain(jpaRepository.save(PaymentMapper.toEntity(merchant)));
    }

    @Override
    public Optional<Merchant> findById(UUID id) {
        return jpaRepository.findById(id).map(PaymentMapper::toDomain);
    }

    @Override
    public List<Merchant> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream().map(PaymentMapper::toDomain).toList();
    }

    @Override
    public Optional<Merchant> findByMerchantCode(String merchantCode) {
        return jpaRepository.findByMerchantCode(merchantCode).map(PaymentMapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndBusinessName(UUID userId, String businessName) {
        return jpaRepository.existsByUserIdAndBusinessName(userId, businessName);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return jpaRepository.existsByUserId(userId);
    }

    // AJOUT : Implémentation du findAllByUserId avec mapping Domain
    @Override
    public List<Merchant> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUserId(userId).stream()
                .map(PaymentMapper::toDomain)
                .toList();
    }
}
