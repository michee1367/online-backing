package com.villagesat.payment.domain.port.out;

import com.villagesat.payment.domain.model.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findByReference(String reference);

    List<Payment> findByMerchantId(UUID merchantId);

    List<Payment> findByCustomerId(UUID customerId);
}
