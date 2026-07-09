package com.villagesat.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Payment(
        UUID id,
        UUID merchantId,
        UUID customerId,
        UUID walletId,
        BigDecimal amount,
        BigDecimal fee,
        String currency,
        PaymentStatus status,
        PaymentMethod paymentMethod,
        String reference,
        String description,
        String merchantOrderId,
        String qrCodeData,
        Instant createdAt,
        Instant completedAt,
        String failedReason,
        long version
) {
    public enum PaymentStatus {
        PENDING,      // Initialisé, en attente d'action
        PROCESSING,   // En cours de traitement auprès du fournisseur/réseau
        SUCCESSFUL,   // Terminé avec succès (Fonds transférés)
        FAILED,       // Échec de la transaction
        CANCELLED,    // Annulé par l'utilisateur
        EXPIRED,      // Délai de validation dépassé
        REFUNDED,     // Remboursé au client
        REVERSED,     // Annulé techniquement pour incohérence/rupture de flux
        COMPLETED     // Complété avec succès
    }

    public enum PaymentMethod {
        // Mobile Money
        MPESA,
        AIRTEL_MONEY,
        ORANGE_MONEY,
        AFRIMONEY,
        
        // Bancaire & Cartes
        CARD,
        BANK_TRANSFER,
        AGENCY_BANKING,
        
        // Interne & Cash
        WALLET,
        QR_CODE,
        CASH
    }

    // AJOUT : Règle métier pour autoriser la confirmation
    public boolean canConfirm() {
        return this.status == PaymentStatus.PENDING;
    }

    // AJOUT : Règle métier pour autoriser le remboursement
    public boolean canRefund() {
        return this.status == PaymentStatus.COMPLETED || this.status == PaymentStatus.SUCCESSFUL;
    }

    public Payment processing(UUID customerId, UUID walletId) {
        return new Payment(id, merchantId, customerId, walletId, amount, fee, currency,
                PaymentStatus.PROCESSING, paymentMethod, reference, description, merchantOrderId,
                qrCodeData, createdAt, Instant.now(), null, version);
    }

    public Payment complete(UUID customerId, UUID walletId, BigDecimal fee) {
        return new Payment(id, merchantId, customerId, walletId, amount, fee, currency,
                PaymentStatus.COMPLETED, paymentMethod, reference, description, merchantOrderId,
                qrCodeData, createdAt, Instant.now(), null, version);
    }

    public Payment fail(String reason) {
        return new Payment(id, merchantId, customerId, walletId, amount, fee, currency,
                PaymentStatus.FAILED, paymentMethod, reference, description, merchantOrderId,
                qrCodeData, createdAt, null, reason, version);
    }

    public Payment refund() {
        return new Payment(id, merchantId, customerId, walletId, amount, fee, currency,
                PaymentStatus.REFUNDED, paymentMethod, reference, description, merchantOrderId,
                qrCodeData, createdAt, completedAt, null, version);
    }
}
