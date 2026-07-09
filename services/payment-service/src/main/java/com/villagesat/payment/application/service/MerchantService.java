package com.villagesat.payment.application.service;

import com.villagesat.payment.domain.model.Merchant;
import com.villagesat.payment.domain.port.in.MerchantUseCase;
import com.villagesat.payment.domain.port.out.MerchantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MerchantService implements MerchantUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MerchantRepository merchantRepository;

    public MerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Override
    public Merchant registerMerchant(RegisterMerchantCommand command) {
        if (merchantRepository.existsByUserId(command.userId())) {
            throw new DuplicateMerchantException(command.userId());
        }

        Merchant merchant = new Merchant(
                UUID.randomUUID(),
                command.userId(),
                command.businessName(),
                command.businessType(),
                generateMerchantCode(),
                Merchant.MerchantStatus.PENDING,
                command.contactEmail(),
                command.contactPhone(),
                command.callbackUrl(),
                Merchant.DEFAULT_COMMISSION_RATE,
                Instant.now(),
                0L
        );

        return merchantRepository.save(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public Merchant getMerchant(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Merchant> listByUserId(UUID userId) {
        return merchantRepository.findAllByUserId(userId);
    }

    @Override
    public Merchant updateStatus(UUID merchantId, Merchant.MerchantStatus status) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));
        Merchant updated = merchant.withStatus(status);
        return merchantRepository.save(updated);
    }

    private String generateMerchantCode() {
        return "VS-M-" + String.format("%05d", Math.abs(RANDOM.nextInt()) % 100_000);
    }

    public static class MerchantNotFoundException extends RuntimeException {
        public MerchantNotFoundException(UUID merchantId) {
            super("Merchant not found: " + merchantId);
        }

        public MerchantNotFoundException(String message) {
            super(message);
        }
    }

    public static class DuplicateMerchantException extends RuntimeException {
        public DuplicateMerchantException(UUID userId) {
            super("Merchant already registered for user: " + userId);
        }
    }
}
