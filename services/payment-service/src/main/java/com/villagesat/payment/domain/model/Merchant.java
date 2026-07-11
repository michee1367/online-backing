package com.villagesat.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Merchant(
        UUID id,
        UUID userId,
        String businessName,
        String businessType,
        String merchantCode,
        MerchantStatus status,
        String contactEmail,
        String contactPhone,
        String callbackUrl,
        String listProductsUrl,
        BigDecimal commissionRate,
        Instant createdAt,
        long version
) {
    // AJOUT : Constante statique pour le taux de commission par défaut (ex: 2.0%)
    public static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.02");

    public enum MerchantStatus {
        PENDING,      // En attente de validation KYC / Revue administrative
        ACTIVE,       // Opérationnel, transactions autorisées
        SUSPENDED,    // Bloqué temporairement pour raisons de sécurité/fraude
        REJECTED,     // Refusé lors de l'onboarding
        TERMINATED    // Clôturé définitivement
    }

    public boolean isActive() {
        return status == MerchantStatus.ACTIVE;
    }

    // AJOUT : Méthode permettant de faire évoluer le statut en conservant l'immutabilité du Record
    public Merchant withStatus(MerchantStatus newStatus) {
        return new Merchant(
                this.id,
                this.userId,
                this.businessName,
                this.businessType,
                this.merchantCode,
                newStatus, // On injecte le nouveau statut ici
                this.contactEmail,
                this.contactPhone,
                this.callbackUrl,
                this.listProductsUrl,
                this.commissionRate,
                this.createdAt,
                this.version
        );
    }
}
