package com.villagesat.transaction.application.service;

import com.villagesat.common.idempotency.IdempotencyService;
import com.villagesat.common.security.SecurityUtils;
import com.villagesat.transaction.adapter.out.wallet.WalletClient;
import com.villagesat.transaction.domain.model.Transaction;
import com.villagesat.transaction.domain.port.out.FraudScoringPort;
import com.villagesat.transaction.domain.port.out.TransactionEventPublisher;
import com.villagesat.transaction.domain.port.out.TransactionRepository;
import com.villagesat.transaction.domain.port.out.WalletQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.villagesat.transaction.support.TransactionTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    WalletClient walletClient;

    @Mock
    WalletQueryPort walletQueryPort;

    @Mock
    FraudScoringPort fraudScoringPort;

    @Mock
    TransactionEventPublisher eventPublisher;

    @Mock
    IdempotencyService idempotencyService;

    TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(
                transactionRepository,
                walletClient,
                walletQueryPort,
                fraudScoringPort,
                eventPublisher,
                idempotencyService,
                new FeeCalculator());
    }

    private void stubMatchingWallets() {
        when(walletQueryPort.findById(SOURCE_WALLET))
                .thenReturn(new WalletQueryPort.WalletInfo(SOURCE_WALLET, "CDF"));
        when(walletQueryPort.findById(DEST_WALLET))
                .thenReturn(new WalletQueryPort.WalletInfo(DEST_WALLET, "CDF"));
    }

    @Test
    void executeTransfer_success_debitsWithFeeAndCreditsAmount() {
        UUID idempotencyKey = UUID.randomUUID();
        stubIdempotencyFresh(idempotencyKey);
        stubMatchingWallets();
        when(fraudScoringPort.score(any())).thenReturn(new FraudScoringPort.FraudResult(10, FraudScoringPort.FraudAction.ALLOW));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

            TransferService.TransferResult result = transferService.executeTransfer(
                    new TransferService.TransferCommand(
                            idempotencyKey, SOURCE_WALLET, DEST_WALLET,
                            new BigDecimal("5000.0000"), "CDF", "P2P-111", "P2P"));

            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.fee()).isEqualTo("100.0000");
            assertThat(result.totalDebited()).isEqualTo("5100.0000");

            verify(walletClient).debit(eq(SOURCE_WALLET), any(UUID.class),
                    eq(new BigDecimal("5100.0000")), anyString());
            verify(walletClient).credit(eq(DEST_WALLET), any(UUID.class),
                    eq(new BigDecimal("5000.0000")), anyString());
            verify(eventPublisher).publishTransactionCompleted(any(Transaction.class));
            verify(idempotencyService).storeResponse(eq(idempotencyKey), any(TransferService.TransferResult.class));
        }
    }

    @Test
    void executeTransfer_fraudBlocked_doesNotCallWallet() {
        UUID idempotencyKey = UUID.randomUUID();
        stubIdempotencyFresh(idempotencyKey);
        stubMatchingWallets();
        when(fraudScoringPort.score(any())).thenReturn(new FraudScoringPort.FraudResult(99, FraudScoringPort.FraudAction.BLOCK));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

            assertThatThrownBy(() -> transferService.executeTransfer(
                    new TransferService.TransferCommand(
                            idempotencyKey, SOURCE_WALLET, DEST_WALLET,
                            new BigDecimal("1000"), "CDF", "P2P-112", null)))
                    .isInstanceOf(TransferService.FraudBlockedException.class);

            verifyNoInteractions(walletClient);
            verify(transactionRepository, never()).save(any());
        }
    }

    @Test
    void executeTransfer_cachedIdempotencyKey_returnsWithoutWalletCalls() {
        UUID idempotencyKey = UUID.randomUUID();
        TransferService.TransferResult cached = new TransferService.TransferResult(
                UUID.randomUUID(), "COMPLETED", "1000", "100", "1100", "P2P-113", null);
        when(idempotencyService.getCachedResponse(idempotencyKey, TransferService.TransferResult.class))
                .thenReturn(Optional.of(cached));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            TransferService.TransferResult result = transferService.executeTransfer(
                    new TransferService.TransferCommand(
                            idempotencyKey, SOURCE_WALLET, DEST_WALLET,
                            new BigDecimal("1000"), "CDF", "P2P-115", null));

            assertThat(result).isEqualTo(cached);
            verifyNoInteractions(walletClient, fraudScoringPort);
        }
    }

    @Test
    void executeTransfer_lockNotAcquired_throwsDuplicate() {
        UUID idempotencyKey = UUID.randomUUID();
        when(idempotencyService.getCachedResponse(idempotencyKey, TransferService.TransferResult.class))
                .thenReturn(Optional.empty());
        when(idempotencyService.tryAcquireLock(idempotencyKey)).thenReturn(false);

        assertThatThrownBy(() -> transferService.executeTransfer(
                new TransferService.TransferCommand(
                        idempotencyKey, SOURCE_WALLET, DEST_WALLET,
                        new BigDecimal("1000"), "CDF", "P2P-116", null)))
                .isInstanceOf(TransferService.DuplicateTransactionException.class);
    }

    @Test
    void executeTransfer_walletDebitFails_marksTransactionFailed() {
        UUID idempotencyKey = UUID.randomUUID();
        stubIdempotencyFresh(idempotencyKey);
        stubMatchingWallets();
        when(fraudScoringPort.score(any())).thenReturn(new FraudScoringPort.FraudResult(0, FraudScoringPort.FraudAction.ALLOW));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("insufficient funds"))
                .when(walletClient).debit(any(), any(), any(), any());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

            assertThatThrownBy(() -> transferService.executeTransfer(
                    new TransferService.TransferCommand(
                            idempotencyKey, SOURCE_WALLET, DEST_WALLET,
                            new BigDecimal("1000"), "CDF", "P2P-117", null)))
                    .isInstanceOf(RuntimeException.class);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(
                    t -> t.status() == Transaction.TransactionStatus.FAILED);
            verify(walletClient, never()).credit(any(), any(), any(), any());
        }
    }

    @Test
    void executeTransfer_walletCurrenciesMismatch_throwsBeforeFraudCheck() {
        UUID idempotencyKey = UUID.randomUUID();
        stubIdempotencyFresh(idempotencyKey);
        when(walletQueryPort.findById(SOURCE_WALLET))
                .thenReturn(new WalletQueryPort.WalletInfo(SOURCE_WALLET, "CDF"));
        when(walletQueryPort.findById(DEST_WALLET))
                .thenReturn(new WalletQueryPort.WalletInfo(DEST_WALLET, "USD"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

            assertThatThrownBy(() -> transferService.executeTransfer(
                    new TransferService.TransferCommand(
                            idempotencyKey, SOURCE_WALLET, DEST_WALLET,
                            new BigDecimal("1000"), "CDF", null)))
                    .isInstanceOf(TransferService.CurrencyMismatchException.class)
                    .hasMessageContaining("CDF vs USD");

            verifyNoInteractions(fraudScoringPort, walletClient);
            verify(transactionRepository, never()).save(any());
        }
    }

    @Test
    void executeTransfer_requestCurrencyMismatch_throwsBeforeFraudCheck() {
        UUID idempotencyKey = UUID.randomUUID();
        stubIdempotencyFresh(idempotencyKey);
        stubMatchingWallets();

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

            assertThatThrownBy(() -> transferService.executeTransfer(
                    new TransferService.TransferCommand(
                            idempotencyKey, SOURCE_WALLET, DEST_WALLET,
                            new BigDecimal("1000"), "USD", null)))
                    .isInstanceOf(TransferService.CurrencyMismatchException.class)
                    .hasMessageContaining("USD vs CDF");

            verifyNoInteractions(fraudScoringPort, walletClient);
            verify(transactionRepository, never()).save(any());
        }
    }

    private void stubIdempotencyFresh(UUID idempotencyKey) {
        when(idempotencyService.getCachedResponse(idempotencyKey, TransferService.TransferResult.class))
                .thenReturn(Optional.empty());
        when(idempotencyService.tryAcquireLock(idempotencyKey)).thenReturn(true);
    }
}
