package com.villagesat.wallet.application.service;

import com.villagesat.wallet.domain.model.Wallet;
import com.villagesat.wallet.domain.port.out.WalletEventPublisher;
import com.villagesat.wallet.domain.port.out.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static com.villagesat.wallet.support.WalletTestFixtures.USER_ID;
import static com.villagesat.wallet.support.WalletTestFixtures.l0Wallet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletKycLimitServiceTest {

    @Mock
    WalletRepository walletRepository;

    @Mock
    WalletEventPublisher eventPublisher;

    @InjectMocks
    WalletKycLimitService service;

    @Test
    void applyKycLimits_upgradesActiveWallets() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = l0Wallet(walletId);
        when(walletRepository.findByUserId(USER_ID)).thenReturn(List.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Wallet> updated = service.applyKycLimits(USER_ID, 1);

        assertThat(updated).hasSize(1);
        assertThat(updated.getFirst().kycLevel()).isEqualTo(1);
        assertThat(updated.getFirst().dailyLimit()).isEqualByComparingTo("500");

        verify(eventPublisher).publishLimitsUpdated(updated.getFirst(), 0);
    }

    @Test
    void applyKycLimits_skipsWhenAlreadyAtLevel() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = l0Wallet(walletId).applyKycLevel(1);
        when(walletRepository.findByUserId(USER_ID)).thenReturn(List.of(wallet));

        List<Wallet> updated = service.applyKycLimits(USER_ID, 1);

        assertThat(updated).isEmpty();
        verify(walletRepository, never()).save(any());
        verify(eventPublisher, never()).publishLimitsUpdated(any(), anyInt());
    }

    @Test
    void applyKycLimits_skipsClosedWallets() {
        UUID walletId = UUID.randomUUID();
        Wallet closed = new Wallet(
                walletId, USER_ID, "VS-0000000001", "CDF", Wallet.WalletType.PERSONAL, "Test",
                Wallet.WalletStatus.CLOSED, 0,
                l0Wallet(walletId).dailyLimit(), l0Wallet(walletId).monthlyLimit(),
                l0Wallet(walletId).createdAt(), 0L);
        when(walletRepository.findByUserId(USER_ID)).thenReturn(List.of(closed));

        assertThat(service.applyKycLimits(USER_ID, 1)).isEmpty();
        verify(walletRepository, never()).save(any());
    }
}
