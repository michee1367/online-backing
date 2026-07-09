package com.villagesat.payment.adapter.in.web;

import com.villagesat.common.security.SecurityUtils;
import com.villagesat.payment.domain.model.Payment;
import com.villagesat.payment.domain.model.QrCode;
import com.villagesat.payment.domain.port.in.PaymentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Paiements marchands — QR Code, checkout")
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    public PaymentController(PaymentUseCase paymentUseCase) {
        this.paymentUseCase = paymentUseCase;
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Initier un paiement (marchand)")
    public ResponseEntity<PaymentResponse> initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Payment payment = paymentUseCase.initiatePayment(new PaymentUseCase.InitiatePaymentCommand(
                userId,
                request.amount(),
                request.currency(),
                request.description(),
                request.merchantOrderId(),
                request.paymentMethod()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment));
    }

    @PostMapping("/{reference}/confirm")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Confirmer un paiement (client)")
    public ResponseEntity<PaymentResponse> confirm(
            @PathVariable String reference,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        UUID customerId = SecurityUtils.getCurrentUserId();
        Payment payment = paymentUseCase.confirmPayment(reference,
                new PaymentUseCase.ConfirmPaymentCommand(customerId, request.walletId()));
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @GetMapping("/{reference}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Détail d'un paiement")
    public PaymentResponse getPayment(@PathVariable String reference) {
        return PaymentResponse.from(paymentUseCase.getPayment(reference));
    }

    @PostMapping("/{reference}/refund")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Rembourser un paiement (marchand)")
    public ResponseEntity<PaymentResponse> refund(@PathVariable String reference) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Payment payment = paymentUseCase.refundPayment(reference, userId);
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @PostMapping("/qr/generate")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Générer un QR code de paiement (marchand)")
    public ResponseEntity<QrCodeResponse> generateQr(@Valid @RequestBody GenerateQrRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        QrCode qr = paymentUseCase.generateQrCode(new PaymentUseCase.GenerateQrCommand(
                userId, request.amount(), request.currency(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(QrCodeResponse.from(qr));
    }

    public record InitiatePaymentRequest(
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @Size(max = 255) String description,
            @Size(max = 100) String merchantOrderId,
            Payment.PaymentMethod paymentMethod
    ) {}

    public record ConfirmPaymentRequest(
            @NotNull UUID walletId
    ) {}

    public record GenerateQrRequest(
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @Size(max = 255) String description
    ) {}

    public record PaymentResponse(
            UUID paymentId,
            UUID merchantId,
            UUID customerId,
            String reference,
            String amount,
            String fee,
            String currency,
            String status,
            String paymentMethod,
            String description,
            String qrCodeData,
            Instant createdAt,
            Instant completedAt,
            String failedReason
    ) {
        static PaymentResponse from(Payment p) {
            return new PaymentResponse(
                    p.id(),
                    p.merchantId(),
                    p.customerId(),
                    p.reference(),
                    p.amount().toPlainString(),
                    p.fee().toPlainString(),
                    p.currency(),
                    p.status().name(),
                    p.paymentMethod().name(),
                    p.description(),
                    p.qrCodeData(),
                    p.createdAt(),
                    p.completedAt(),
                    p.failedReason()
            );
        }
    }

    public record QrCodeResponse(
            UUID merchantId,
            String reference,
            String amount,
            String currency,
            Instant expiresAt,
            String qrCodeData
    ) {
        static QrCodeResponse from(QrCode qr) {
            return new QrCodeResponse(
                    qr.merchantId(),
                    qr.reference(),
                    qr.amount().toPlainString(),
                    qr.currency(),
                    qr.expiresAt(),
                    qr.data()
            );
        }
    }
}
