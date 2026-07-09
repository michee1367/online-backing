package com.villagesat.wallet.application.service;

import com.villagesat.wallet.domain.model.Wallet;
import com.villagesat.wallet.domain.port.in.WalletKycUseCase;
import com.villagesat.wallet.domain.port.out.WalletEventPublisher;
import com.villagesat.wallet.domain.port.out.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WalletKycLimitService implements WalletKycUseCase {

    private static final Logger log = LoggerFactory.getLogger(WalletKycLimitService.class);

    private final WalletRepository walletRepository;
    private final WalletEventPublisher eventPublisher;

    public WalletKycLimitService(WalletRepository walletRepository,
                                 WalletEventPublisher eventPublisher) {
        this.walletRepository = walletRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<Wallet> applyKycLimits(UUID userId, int kycLevel) {
        List<Wallet> updated = new ArrayList<>();
        for (Wallet wallet : walletRepository.findByUserId(userId)) {
            if (wallet.status() == Wallet.WalletStatus.CLOSED) {
                continue;
            }
            if (wallet.kycLevel() == kycLevel
                    && wallet.dailyLimit().equals(wallet.applyKycLevel(kycLevel).dailyLimit())) {
                continue;
            }
            int previousLevel = wallet.kycLevel();
            Wallet withLimits = wallet.applyKycLevel(kycLevel);
            Wallet saved = walletRepository.save(withLimits);
            eventPublisher.publishLimitsUpdated(saved, previousLevel);
            updated.add(saved);
            log.info("Applied KYC level {} limits to wallet {} (user {})", kycLevel, saved.id(), userId);
        }
        return updated;
    }
}
