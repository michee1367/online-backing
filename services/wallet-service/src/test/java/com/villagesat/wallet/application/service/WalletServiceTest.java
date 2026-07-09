package com.villagesat.wallet.application.service;

import com.villagesat.wallet.domain.model.Balance;
import com.villagesat.wallet.domain.model.Wallet;
import com.villagesat.wallet.domain.port.in.WalletUseCase;
import com.villagesat.wallet.domain.port.out.BalanceRepository;
import com.villagesat.wallet.domain.port.out.WalletEventPublisher;
import com.villagesat.wallet.domain.port.out.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.villagesat.wallet.support.WalletTestFixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    WalletRepository walletRepository;

    @Mock
    BalanceRepository balanceRepository;

    @Mock
    WalletEventPublisher eventPublisher;

    @InjectMocks
    WalletService walletService;

    @Test
    void createWallet_initializesL0Limits() {
        when(walletRepository.existsByUserIdAndCurrency(USER_ID, "CDF")).thenReturn(false);
        when(walletRepository.existsByAccountNumber(any())).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet wallet = walletService.createWallet(
                new WalletUseCase.CreateWalletCommand(USER_ID, "CDF", Wallet.WalletType.PERSONAL, "Principal"));

        assertThat(wallet.kycLevel()).isZero();
        assertThat(wallet.dailyLimit()).isEqualByComparingTo("200000");
        assertThat(wallet.monthlyLimit()).isEqualByComparingTo("200");
        assertThat(wallet.accountNumber()).matches("[1-6]\\d{5}");

        ArgumentCaptor<Balance> balanceCaptor = ArgumentCaptor.forClass(Balance.class);
        verify(balanceRepository).save(balanceCaptor.capture());
        assertThat(balanceCaptor.getValue().balance()).isEqualByComparingTo("0");
        verify(eventPublisher).publishWalletCreated(wallet);
    }
}
