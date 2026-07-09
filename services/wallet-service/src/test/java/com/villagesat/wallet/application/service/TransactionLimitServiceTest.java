package com.villagesat.wallet.application.service;

import com.villagesat.wallet.domain.model.TransactionLimitExceededException;
import com.villagesat.wallet.domain.port.out.LedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.villagesat.wallet.support.WalletTestFixtures.l0Wallet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

@ExtendWith(MockitoExtension.class)
class TransactionLimitServiceTest {

    @Mock
    LedgerRepository ledgerRepository;

    TransactionLimitService service;

    UUID walletId;

    @BeforeEach
    void setUp() {
        service = new TransactionLimitService(ledgerRepository);
        walletId = UUID.randomUUID();
    }

    @Test
    void validateDebit_singleTransactionExceedsDailyLimit_throws() {
        when(ledgerRepository.sumDebitsSince(eq(walletId), any(Instant.class)))
                .thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.validateDebitWithinLimits(
                l0Wallet(walletId), UUID.randomUUID(), new BigDecimal("51")))
                .isInstanceOf(TransactionLimitExceededException.class)
                .asInstanceOf(type(TransactionLimitExceededException.class))
                .extracting(TransactionLimitExceededException::limitType)
                .isEqualTo("DAILY_SINGLE");
        
    }

    @Test
    void validateDebit_cumulativeDailyExceeded_throws() {
        when(ledgerRepository.sumDebitsSince(eq(walletId), any(Instant.class)))
                .thenReturn(new BigDecimal("40"));

        assertThatThrownBy(() -> service.validateDebitWithinLimits(
                l0Wallet(walletId), UUID.randomUUID(), new BigDecimal("15")))
                .isInstanceOf(TransactionLimitExceededException.class)
                .asInstanceOf(type(TransactionLimitExceededException.class))
                .extracting(TransactionLimitExceededException::limitType)
                .isEqualTo("DAILY_CUMULATIVE");
    }

    @Test
    void validateDebit_cumulativeMonthlyExceeded_throws() {
        when(ledgerRepository.sumDebitsSince(eq(walletId), any(Instant.class)))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("190"));

        assertThatThrownBy(() -> service.validateDebitWithinLimits(
                l0Wallet(walletId), UUID.randomUUID(), new BigDecimal("15")))
                .isInstanceOf(TransactionLimitExceededException.class)
                .asInstanceOf(type(TransactionLimitExceededException.class))
                .extracting(TransactionLimitExceededException::limitType)
                .isEqualTo("MONTHLY_CUMULATIVE");
    }

    @Test
    void validateDebit_withinLimits_passes() {
        when(ledgerRepository.sumDebitsSince(eq(walletId), any(Instant.class)))
                .thenReturn(BigDecimal.ZERO);

        service.validateDebitWithinLimits(l0Wallet(walletId), UUID.randomUUID(), new BigDecimal("25"));

        verify(ledgerRepository).sumDebitsSince(eq(walletId), any(Instant.class));
    }

    @Test
    void validateDebit_zeroOrNegativeAmount_skipsValidation() {
        service.validateDebitWithinLimits(l0Wallet(walletId), UUID.randomUUID(), BigDecimal.ZERO);
        service.validateDebitWithinLimits(l0Wallet(walletId), UUID.randomUUID(), new BigDecimal("-1"));
    }
}
