package com.ksa.financing.wallet.application.usecase;

// TODO: Move FineractSyncWorkflow interface to domain/port or application layer to fix adapter dependency (hexagonal violation)
import com.ksa.financing.wallet.adapter.temporal.workflow.FineractSyncWorkflow;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletStatus;
import com.ksa.financing.wallet.domain.port.in.CreateWalletUseCase;
import com.ksa.financing.wallet.domain.port.out.EventPublisherPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import com.ksa.financing.wallet.infrastructure.fineract.FineractWalletConfig;
import com.ksa.islamic.orchestration.common.TaskQueue;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;

@Service
public class CreateWalletService implements CreateWalletUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateWalletService.class);

    private final WalletRepository walletRepository;
    private final EventPublisherPort eventPublisher;
    private final WorkflowClient workflowClient;
    private final FineractWalletConfig fineractConfig;

    public CreateWalletService(WalletRepository walletRepository,
                               EventPublisherPort eventPublisher,
                               WorkflowClient workflowClient,
                               FineractWalletConfig fineractConfig) {
        this.walletRepository = walletRepository;
        this.eventPublisher = eventPublisher;
        this.workflowClient = workflowClient;
        this.fineractConfig = fineractConfig;
    }

    @Override
    @Transactional
    public Wallet create(CreateWalletCommand command) {
        if (walletRepository.existsByCustomerId(command.tenantId(), command.customerId())) {
            return walletRepository.findByCustomerId(command.tenantId(), command.customerId())
                .orElseThrow();
        }

        Wallet wallet = new Wallet();
        wallet.setTenantId(command.tenantId());
        wallet.setCustomerId(command.customerId());
        wallet.setWalletNumber("WLT" + System.currentTimeMillis() % 10000000000L);
        wallet.setCurrency(command.currency() != null ? command.currency() : "SAR");
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setReservedBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setDailyTopUpLimit(new BigDecimal("50000"));
        wallet.setMonthlyTopUpLimit(new BigDecimal("200000"));
        wallet.setSingleTopUpLimit(new BigDecimal("10000"));
        wallet.setTodayTopUpAmount(BigDecimal.ZERO);
        wallet.setMonthTopUpAmount(BigDecimal.ZERO);
        wallet.setAutoDebitEnabled(true);

        Wallet saved = walletRepository.save(wallet);

        // Start Fineract sync workflow AFTER transaction commits successfully
        if (fineractConfig.isEnabled()) {
            scheduleAfterCommit(saved);
        }

        try {
            eventPublisher.publishWalletCreated(saved);
        } catch (Exception e) {
            log.warn("Failed to publish wallet-created event for walletId={} — continuing: {}",
                    saved.getId(), e.getMessage());
        }
        return saved;
    }

    private void scheduleAfterCommit(Wallet wallet) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                startFineractSyncWorkflow(wallet);
            }
        });
    }

    private void startFineractSyncWorkflow(Wallet wallet) {
        try {
            var input = new FineractSyncWorkflow.FineractSyncInput(
                    wallet.getCustomerId().toString(),
                    wallet.getWalletNumber(),
                    wallet.getCurrency()
            );

            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId("fineract-sync-" + wallet.getWalletNumber())
                    .setTaskQueue(TaskQueue.WALLET_QUEUE)
                    .setWorkflowExecutionTimeout(Duration.ofMinutes(10))
                    .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                    .build();

            FineractSyncWorkflow workflow = workflowClient.newWorkflowStub(FineractSyncWorkflow.class, options);
            WorkflowClient.start(workflow::syncWalletToFineract, input);

            log.info("Fineract sync workflow started for wallet={}", wallet.getWalletNumber());
        } catch (Exception e) {
            log.warn("Failed to start Fineract sync workflow for wallet={}: {}. Manual retry needed.",
                    wallet.getWalletNumber(), e.getMessage());
        }
    }
}
