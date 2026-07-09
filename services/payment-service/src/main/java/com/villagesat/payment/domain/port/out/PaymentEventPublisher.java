package com.villagesat.payment.domain.port.out;

import com.villagesat.payment.domain.model.Payment;

public interface PaymentEventPublisher {

    void publishPaymentCompleted(Payment payment);

    void publishPaymentFailed(Payment payment);
}
