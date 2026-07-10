package com.villagesat.wallet.application.service;

import com.villagesat.wallet.domain.model.TransactionLimitExceededException;
import com.villagesat.wallet.domain.model.Wallet;
import com.villagesat.wallet.domain.port.out.LedgerRepository;
import com.villagesat.wallet.domain.port.out.TransactionLimitPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class TransactionLimitService implements TransactionLimitPort {

    private final LedgerRepository ledgerRepository;

    public TransactionLimitService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    public void validateDebitWithinLimits(Wallet wallet, UUID transactionId, BigDecimal amount) {

        if (wallet.type().equals(Wallet.WalletType.SYSTEM)) {
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal dailyUsed = ledgerRepository.sumDebitsSince(wallet.id(), startOfDay);
        BigDecimal monthlyUsed = ledgerRepository.sumDebitsSince(wallet.id(), startOfMonth);

        if (amount.compareTo(wallet.dailyLimit()) > 0) {
            throw new TransactionLimitExceededException(wallet.id(), "DAILY_SINGLE",
                    amount, wallet.dailyLimit(), dailyUsed);
        }
        if (dailyUsed.add(amount).compareTo(wallet.dailyLimit()) > 0) {
            throw new TransactionLimitExceededException(wallet.id(), "DAILY_CUMULATIVE",
                    amount, wallet.dailyLimit(), dailyUsed);
        }
        if (monthlyUsed.add(amount).compareTo(wallet.monthlyLimit()) > 0) {
            throw new TransactionLimitExceededException(wallet.id(), "MONTHLY_CUMULATIVE",
                    amount, wallet.monthlyLimit(), monthlyUsed);
        }
    }
}
