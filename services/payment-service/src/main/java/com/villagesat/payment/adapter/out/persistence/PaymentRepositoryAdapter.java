package com.villagesat.payment.adapter.out.persistence;

import com.villagesat.payment.adapter.out.persistence.mapper.PaymentMapper;
import com.villagesat.payment.domain.model.Payment;
import com.villagesat.payment.domain.port.out.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Payment save(Payment payment) {
        return PaymentMapper.toDomain(jpaRepository.save(PaymentMapper.toEntity(payment)));
    }

    @Override
    public Optional<Payment> findByReference(String reference) {
        return jpaRepository.findByReference(reference).map(PaymentMapper::toDomain);
    }

    @Override
    public List<Payment> findByMerchantId(UUID merchantId) {
        return jpaRepository.findByMerchantId(merchantId).stream().map(PaymentMapper::toDomain).toList();
    }

    @Override
    public List<Payment> findByCustomerId(UUID customerId) {
        return jpaRepository.findByCustomerId(customerId).stream().map(PaymentMapper::toDomain).toList();
    }
}
