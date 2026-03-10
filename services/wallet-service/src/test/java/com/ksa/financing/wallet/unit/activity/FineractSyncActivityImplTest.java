package com.ksa.financing.wallet.unit.activity;

import com.ksa.financing.wallet.adapter.temporal.activity.FineractSyncActivityImpl;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FineractSyncActivityImpl Tests")
class FineractSyncActivityImplTest {

    @Mock
    private FineractSavingsPort fineractPort;

    @Mock
    private WalletRepository walletRepository;

    private FineractSyncActivityImpl activity;

    @BeforeEach
    void setUp() {
        activity = new FineractSyncActivityImpl(fineractPort, walletRepository);
    }

    @Test
    @DisplayName("Should lookup Fineract client successfully")
    void shouldLookupClientSuccessfully() {
        // Given
        when(fineractPort.lookupClientByExternalId("customer-123")).thenReturn(42L);

        // When
        Long clientId = activity.lookupFineractClient("customer-123");

        // Then
        assertThat(clientId).isEqualTo(42L);
        verify(fineractPort).lookupClientByExternalId("customer-123");
    }

    @Test
    @DisplayName("Should throw when client not found in Fineract")
    void shouldThrowWhenClientNotFound() {
        // Given
        when(fineractPort.lookupClientByExternalId("unknown")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> activity.lookupFineractClient("unknown"))
                .hasMessageContaining("Fineract client not found");
    }

    @Test
    @DisplayName("Should create savings account successfully")
    void shouldCreateSavingsAccount() {
        // Given
        when(fineractPort.createSavings(10L, "WLT001")).thenReturn(100L);

        // When
        Long savingsId = activity.createSavingsAccount(10L, "WLT001");

        // Then
        assertThat(savingsId).isEqualTo(100L);
        verify(fineractPort).createSavings(10L, "WLT001");
    }

    @Test
    @DisplayName("Should approve savings account")
    void shouldApproveSavingsAccount() {
        // When
        activity.approveSavingsAccount(100L);

        // Then
        verify(fineractPort).approveSavings(100L);
    }

    @Test
    @DisplayName("Should activate savings account")
    void shouldActivateSavingsAccount() {
        // When
        activity.activateSavingsAccount(100L);

        // Then
        verify(fineractPort).activateSavings(100L);
    }

    @Test
    @DisplayName("Should link wallet to Fineract and update wallet fields")
    void shouldLinkWalletToFineract() {
        // Given
        Wallet wallet = new Wallet();
        wallet.setWalletNumber("WLT001");
        when(walletRepository.findByWalletNumber("WLT001")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        activity.linkWalletToFineract("WLT001", 100L);

        // Then
        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());

        Wallet saved = captor.getValue();
        assertThat(saved.getFineractSavingsAccountId()).isEqualTo(100L);
        assertThat(saved.isLedgerSynced()).isTrue();
        assertThat(saved.getLastLedgerSyncAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw when wallet not found during link")
    void shouldThrowWhenWalletNotFoundDuringLink() {
        // Given
        when(walletRepository.findByWalletNumber("WLT999")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> activity.linkWalletToFineract("WLT999", 100L))
                .hasMessageContaining("Wallet not found");
    }

    @Test
    @DisplayName("Should delete savings account for compensation")
    void shouldDeleteSavingsAccount() {
        // When
        activity.deleteSavingsAccount(100L);

        // Then
        verify(fineractPort).deleteSavings(100L);
    }
}
