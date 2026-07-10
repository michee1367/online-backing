package com.villagesat.payment.domain.port.in;

import com.villagesat.payment.domain.model.Payment;
import com.villagesat.payment.domain.model.Payment.PaymentMethod;
//import com.villagesat.payment.domain.port.in.PaymentUseCase.InitiatePaymentCommand.ConfirmPaymentCommand;
import com.villagesat.payment.domain.model.QrCode;
import com.villagesat.payment.domain.port.in.PaymentUseCase.ConfirmPaymentCommand;
import com.villagesat.payment.domain.port.in.PaymentUseCase.InitiatePaymentCommand;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface PaymentUseCase {

    Payment initiatePayment(InitiatePaymentCommand command);

    Payment confirmPayment(UUID reference, ConfirmPaymentCommand confirmPaymentCommand);

    Payment getPayment(UUID reference);

    List<Payment> getByMerchant(UUID merchantId);

    Payment refundPayment(UUID reference, UUID merchantUserId);

    public QrCode generateQrCode(GenerateQrCommand command);

    record InitiatePaymentCommand(
            UUID merchantUserId,
            BigDecimal amount,
            String currency,
            String description,
            String merchantOrderId,
            PaymentMethod paymentMethod
    ) {}
    public record ConfirmPaymentCommand(
            UUID customerId,
            UUID walletId
    ) {
        // Validation de sécurité à la construction
        public ConfirmPaymentCommand {
            if (customerId == null) {
                throw new IllegalArgumentException("Customer ID cannot be null");
            }
            if (walletId == null) {
                throw new IllegalArgumentException("Wallet ID cannot be null");
            }
        }
    }
    public record GenerateQrCommand(
            UUID merchantUserId,
            BigDecimal amount,
            String currency,
            String description
    ) {
        // Constructeur compact pour valider les contraintes de base (Fail-Fast)
        public GenerateQrCommand {
            Objects.requireNonNull(merchantUserId, "User ID cannot be null");
            Objects.requireNonNull(amount, "Amount cannot be null");
            Objects.requireNonNull(currency, "Currency cannot be null");
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be strictly greater than zero");
            }
            
            if (currency.isBlank()) {
                throw new IllegalArgumentException("Currency cannot be blank");
            }
        }
    }
}
