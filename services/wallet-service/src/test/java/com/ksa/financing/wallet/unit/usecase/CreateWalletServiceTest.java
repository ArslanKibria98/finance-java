package com.ksa.financing.wallet.unit.usecase;

import com.ksa.financing.wallet.adapter.temporal.workflow.FineractSyncWorkflow;
import com.ksa.financing.wallet.application.usecase.CreateWalletService;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletStatus;
import com.ksa.financing.wallet.domain.port.in.CreateWalletUseCase.CreateWalletCommand;
import com.ksa.financing.wallet.domain.port.out.EventPublisherPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import com.ksa.financing.wallet.infrastructure.fineract.FineractWalletConfig;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateWalletService Tests")
class CreateWalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private EventPublisherPort eventPublisher;

    @Mock
    private WorkflowClient workflowClient;

    @Mock
    private FineractWalletConfig fineractConfig;

    private CreateWalletService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CreateWalletService(walletRepository, eventPublisher, workflowClient, fineractConfig);
    }

    @Test
    @DisplayName("Should create wallet and publish event")
    void shouldCreateWalletAndPublishEvent() {
        // Given
        var command = new CreateWalletCommand(tenantId, customerId, "SAR");
        when(walletRepository.existsByCustomerId(tenantId, customerId)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> {
            Wallet w = inv.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });
        when(fineractConfig.isEnabled()).thenReturn(false);

        // When
        Wallet result = service.create(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getCurrency()).isEqualTo("SAR");
        assertThat(result.getStatus()).isEqualTo(WalletStatus.ACTIVE);
        assertThat(result.getAvailableBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.isAutoDebitEnabled()).isTrue();

        verify(walletRepository).save(any(Wallet.class));
        verify(eventPublisher).publishWalletCreated(result);
    }

    @Test
    @DisplayName("Should return existing wallet if customer already has one")
    void shouldReturnExistingWallet() {
        // Given
        var command = new CreateWalletCommand(tenantId, customerId, "SAR");
        Wallet existing = new Wallet();
        existing.setId(UUID.randomUUID());
        existing.setCustomerId(customerId);
        existing.setWalletNumber("WLT_EXISTING");

        when(walletRepository.existsByCustomerId(tenantId, customerId)).thenReturn(true);
        when(walletRepository.findByCustomerId(tenantId, customerId)).thenReturn(Optional.of(existing));

        // When
        Wallet result = service.create(command);

        // Then
        assertThat(result.getWalletNumber()).isEqualTo("WLT_EXISTING");
        verify(walletRepository, never()).save(any());
        verify(eventPublisher, never()).publishWalletCreated(any());
    }

    @Test
    @DisplayName("Should default currency to SAR when null")
    void shouldDefaultCurrencyToSar() {
        // Given
        var command = new CreateWalletCommand(tenantId, customerId, null);
        when(walletRepository.existsByCustomerId(tenantId, customerId)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fineractConfig.isEnabled()).thenReturn(false);

        // When
        Wallet result = service.create(command);

        // Then
        assertThat(result.getCurrency()).isEqualTo("SAR");
    }

    @Test
    @DisplayName("Should NOT schedule Fineract workflow when disabled")
    void shouldNotScheduleWorkflowWhenDisabled() {
        // Given
        var command = new CreateWalletCommand(tenantId, customerId, "SAR");
        when(walletRepository.existsByCustomerId(tenantId, customerId)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fineractConfig.isEnabled()).thenReturn(false);

        // When
        service.create(command);

        // Then - no interaction with workflow client
        verifyNoInteractions(workflowClient);
    }

    @Test
    @DisplayName("Should schedule Fineract sync after commit when enabled")
    void shouldScheduleFineractSyncWhenEnabled() {
        // Given
        var command = new CreateWalletCommand(tenantId, customerId, "SAR");
        when(walletRepository.existsByCustomerId(tenantId, customerId)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> {
            Wallet w = inv.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });
        when(fineractConfig.isEnabled()).thenReturn(true);

        // Initialize TransactionSynchronizationManager for test
        TransactionSynchronizationManager.initSynchronization();
        try {
            // When
            Wallet result = service.create(command);

            // Then - synchronization should be registered
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

            // Simulate after-commit callback
            TransactionSynchronization sync = TransactionSynchronizationManager.getSynchronizations().get(0);

            // Mock workflow stub creation
            FineractSyncWorkflow mockWorkflow = mock(FineractSyncWorkflow.class);
            when(workflowClient.newWorkflowStub(eq(FineractSyncWorkflow.class), any(WorkflowOptions.class)))
                    .thenReturn(mockWorkflow);

            sync.afterCommit();

            // Verify workflow client was used to create stub
            verify(workflowClient).newWorkflowStub(eq(FineractSyncWorkflow.class), any(WorkflowOptions.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("Should set correct wallet limits")
    void shouldSetCorrectWalletLimits() {
        // Given
        var command = new CreateWalletCommand(tenantId, customerId, "SAR");
        when(walletRepository.existsByCustomerId(tenantId, customerId)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fineractConfig.isEnabled()).thenReturn(false);

        // When
        Wallet result = service.create(command);

        // Then
        assertThat(result.getDailyTopUpLimit()).isEqualTo(new BigDecimal("50000"));
        assertThat(result.getMonthlyTopUpLimit()).isEqualTo(new BigDecimal("200000"));
        assertThat(result.getSingleTopUpLimit()).isEqualTo(new BigDecimal("10000"));
        assertThat(result.getTodayTopUpAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getMonthTopUpAmount()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should generate wallet number with WLT prefix")
    void shouldGenerateWalletNumberWithPrefix() {
        // Given
        var command = new CreateWalletCommand(tenantId, customerId, "SAR");
        when(walletRepository.existsByCustomerId(tenantId, customerId)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fineractConfig.isEnabled()).thenReturn(false);

        // When
        Wallet result = service.create(command);

        // Then
        assertThat(result.getWalletNumber()).startsWith("WLT");
    }
}
