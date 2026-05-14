package com.ksa.financing.wallet.infrastructure.config;

import com.ksa.financing.wallet.adapter.temporal.activity.FineractSyncActivityImpl;
import com.ksa.financing.wallet.adapter.temporal.activity.WithdrawalActivity;
import com.ksa.financing.wallet.adapter.temporal.workflow.FineractSyncWorkflowImpl;
import com.ksa.financing.wallet.adapter.temporal.workflow.WithdrawalWorkflowImpl;
import com.ksa.financing.wallet.domain.port.in.CreateWalletUseCase;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import com.ksa.financing.wallet.infrastructure.fineract.WalletFineractFeatureFlag;
import com.ksa.financing.wallet.workflow.activity.impl.WalletCreationActivityImpl;
import com.ksa.islamic.orchestration.common.TaskQueue;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TemporalWorkerConfig {

    private final WorkerFactory workerFactory;
    private final WorkerOptions defaultWorkerOptions;

    private final CreateWalletUseCase createWalletUseCase;
    private final WalletFineractFeatureFlag fineractConfig;
    private final FineractSavingsPort fineractSavingsPort;
    private final WalletRepository walletRepository;
    private final WithdrawalActivity withdrawalActivity;

    @PostConstruct
    public void startWorker() {
        log.info("Starting Temporal worker on queue: {}", TaskQueue.WALLET_QUEUE);

        Worker worker = workerFactory.newWorker(TaskQueue.WALLET_QUEUE, defaultWorkerOptions);

        worker.registerActivitiesImplementations(
                new WalletCreationActivityImpl(createWalletUseCase)
        );

        if (fineractConfig.isEnabled()) {
            worker.registerWorkflowImplementationTypes(FineractSyncWorkflowImpl.class);
            worker.registerActivitiesImplementations(
                    new FineractSyncActivityImpl(fineractSavingsPort, walletRepository, fineractConfig)
            );
            log.info("Fineract sync workflow + activity registered on queue: {}", TaskQueue.WALLET_QUEUE);
        }

        // Async withdrawal SAGA — for production SARIE async settlement.
        // Sync flow (InitiateWithdrawalService) is the demo path; this is the durable workflow path.
        worker.registerWorkflowImplementationTypes(WithdrawalWorkflowImpl.class);
        worker.registerActivitiesImplementations(withdrawalActivity);
        log.info("Withdrawal workflow + activity registered on queue: {}", TaskQueue.WALLET_QUEUE);

        workerFactory.start();
        log.info("Temporal worker started on queue: {}", TaskQueue.WALLET_QUEUE);
    }
}
