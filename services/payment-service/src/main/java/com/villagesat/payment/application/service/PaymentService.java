package com.villagesat.payment.application.service;

import com.villagesat.payment.application.service.PaymentService.PaymentNotFoundException;
import com.villagesat.payment.domain.model.Merchant;
import com.villagesat.payment.domain.model.Payment;
import com.villagesat.payment.domain.model.QrCode;
import com.villagesat.payment.domain.port.in.PaymentUseCase;
import com.villagesat.payment.domain.port.out.MerchantRepository;
import com.villagesat.payment.domain.port.out.PaymentEventPublisher;
import com.villagesat.payment.domain.port.out.PaymentRepository;
import com.villagesat.payment.domain.port.out.WalletOperationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentService implements PaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final WalletOperationPort walletOperationPort;
    private final PaymentEventPublisher eventPublisher;
    private final QrCodeGenerator qrCodeGenerator;

    public PaymentService(PaymentRepository paymentRepository,
                          MerchantRepository merchantRepository,
                          WalletOperationPort walletOperationPort,
                          PaymentEventPublisher eventPublisher,
                          QrCodeGenerator qrCodeGenerator) {
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
        this.walletOperationPort = walletOperationPort;
        this.eventPublisher = eventPublisher;
        this.qrCodeGenerator = qrCodeGenerator;
    }

    @Override
    public Payment initiatePayment(InitiatePaymentCommand command) {
        Merchant merchant = merchantRepository.findByUserId(command.merchantUserId())
                .stream()
                .findFirst() // Transforme la List<Merchant> en Optional<Merchant>
                .orElseThrow(() -> new MerchantService.MerchantNotFoundException(
                        "No merchant found for user: " + command.merchantUserId()));

        if (!merchant.isActive()) {
            throw new MerchantNotActiveException(merchant.id());
        }

        UUID reference = generateReference();
        String qrData = qrCodeGenerator.generate(
                merchant.id(), merchant.merchantCode(),
                command.amount(), command.currency(), reference);

        Payment payment = new Payment(
                UUID.randomUUID(),
                merchant.id(),
                null,
                null,
                command.amount(),
                BigDecimal.ZERO,
                command.currency() != null ? command.currency() : "CDF",
                Payment.PaymentStatus.PENDING,
                command.paymentMethod() != null ? command.paymentMethod() : Payment.PaymentMethod.QR_CODE,
                reference,
                command.description(),
                command.merchantOrderId(),
                qrData,
                Instant.now(),
                null,
                null,
                0L
        );

        return paymentRepository.save(payment);
    }

    @Override
    public Payment confirmPayment(UUID reference, ConfirmPaymentCommand command) {
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new PaymentNotFoundException(reference));

        if (!payment.canConfirm()) {
            throw new InvalidPaymentStateException(reference, payment.status().name(), "PENDING");
        }

        Merchant merchant = merchantRepository.findById(payment.merchantId())
                .orElseThrow(() -> new MerchantService.MerchantNotFoundException(payment.merchantId()));

        Payment processing = payment.processing(command.customerId(), command.walletId());
        paymentRepository.save(processing);

        try {
            walletOperationPort.debitCustomer(command.walletId(), payment.amount(), payment.currency(), reference);

            BigDecimal fee = payment.amount()
                    .multiply(merchant.commissionRate())
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal merchantAmount = payment.amount().subtract(fee);

            walletOperationPort.creditMerchant(merchant.id(), merchantAmount, payment.currency(), reference);

            Payment completed = processing.complete(command.customerId(), command.walletId(), fee);
            Payment saved = paymentRepository.save(completed);
            eventPublisher.publishPaymentCompleted(saved);

            log.info("Payment {} completed: amount={}, fee={}, merchant={}",
                    reference, payment.amount(), fee, merchant.merchantCode());
            return saved;

        } catch (Exception e) {
            Payment failed = processing.fail(e.getMessage());
            Payment saved = paymentRepository.save(failed);
            eventPublisher.publishPaymentFailed(saved);

            log.error("Payment {} failed: {}", reference, e.getMessage(), e);
            return saved;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPayment(UUID reference) {
        return paymentRepository.findByReference(reference)
                .orElseThrow(() -> new PaymentNotFoundException(reference));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getByMerchant(UUID merchantId) {
        return paymentRepository.findByMerchantId(merchantId);
    }

    @Override
    public Payment refundPayment(UUID reference, UUID merchantUserId) {
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new PaymentNotFoundException(reference));

        Merchant merchant = merchantRepository.findByUserId(merchantUserId)
                .stream()
                .findFirst() // Transforme la List<Merchant> en Optional<Merchant>
                .orElseThrow(() -> new MerchantService.MerchantNotFoundException(
                        "No merchant found for user: " + merchantUserId));

        if (!payment.merchantId().equals(merchant.id())) {
            throw new UnauthorizedPaymentAccessException(reference);
        }

        if (!payment.canRefund()) {
            throw new InvalidPaymentStateException(reference, payment.status().name(), "COMPLETED");
        }

        if (payment.walletId() != null) {
            
            walletOperationPort.debitCustomer(payment.walletId(),
                    payment.amount().negate(), payment.currency(), UUID.randomUUID());
        }

        Payment refunded = payment.refund();
        Payment saved = paymentRepository.save(refunded);

        log.info("Payment {} refunded: amount={}", reference, payment.amount());
        return saved;
    }

    @Override
    public QrCode generateQrCode(GenerateQrCommand command) {
        Merchant merchant = merchantRepository.findByUserId(command.merchantUserId())
                .stream()
                .findFirst() // Transforme la List<Merchant> en Optional<Merchant>
                .orElseThrow(() -> new MerchantService.MerchantNotFoundException(
                        "No merchant found for user: " + command.merchantUserId()));

        if (!merchant.isActive()) {
            throw new MerchantNotActiveException(merchant.id());
        }

        UUID reference = generateReference();
        String currency = command.currency() != null ? command.currency() : "CDF";
        String qrData = qrCodeGenerator.generate(
                merchant.id(), merchant.merchantCode(),
                command.amount(), currency, reference);

        Payment payment = new Payment(
                UUID.randomUUID(),
                merchant.id(),
                null, null,
                command.amount(),
                BigDecimal.ZERO,
                currency,
                Payment.PaymentStatus.PENDING,
                Payment.PaymentMethod.QR_CODE,
                reference,
                command.description(),
                null,
                qrData,
                Instant.now(),
                null, null,
                0L
        );
        paymentRepository.save(payment);

        return new QrCode(
                merchant.id(),
                command.amount(),
                currency,
                reference,
                qrCodeGenerator.defaultExpiry(),
                qrData
        );
    }

    private UUID generateReference() {
        return UUID.randomUUID();
        //return "PAY-" + String.format("%012d", Math.abs(RANDOM.nextLong()) % 1_000_000_000_000L);
    }

    public static class PaymentNotFoundException extends RuntimeException {
        public PaymentNotFoundException(UUID reference) {
            super("Payment not found: " + reference);
        }
    }

    public static class InvalidPaymentStateException extends RuntimeException {
        public InvalidPaymentStateException(UUID reference, String current, String expected) {
            super("Payment %s is in state %s, expected %s".formatted(reference, current, expected));
        }
    }

    public static class MerchantNotActiveException extends RuntimeException {
        public MerchantNotActiveException(UUID merchantId) {
            super("Merchant is not active: " + merchantId);
        }
    }

    public static class UnauthorizedPaymentAccessException extends RuntimeException {
        public UnauthorizedPaymentAccessException(UUID reference) {
            super("Unauthorized access to payment: " + reference);
        }
    }
}
