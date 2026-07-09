package com.villagesat.payment.adapter.out.persistence;

import com.villagesat.payment.adapter.out.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByReference(String reference);

    List<PaymentEntity> findByMerchantId(UUID merchantId);

    List<PaymentEntity> findByCustomerId(UUID customerId);
}
