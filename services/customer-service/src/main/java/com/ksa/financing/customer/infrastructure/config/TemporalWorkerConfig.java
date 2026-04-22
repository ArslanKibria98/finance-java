package com.ksa.financing.customer.infrastructure.config;

import com.ksa.financing.customer.domain.port.in.CreateCustomerUseCase;
import com.ksa.financing.customer.domain.port.in.ManageBankAccountsUseCase;
import com.ksa.financing.customer.domain.port.in.SubmitPepAnswerUseCase;
import com.ksa.financing.customer.domain.port.in.UpdateCustomerUseCase;
import com.ksa.financing.customer.workflow.activity.impl.ProfileCreationActivityImpl;
import com.ksa.financing.customer.workflow.activity.impl.UpdateCustomerActivityImpl;
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

    private final CreateCustomerUseCase createCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final ManageBankAccountsUseCase manageBankAccountsUseCase;
    private final SubmitPepAnswerUseCase submitPepAnswerUseCase;

    @PostConstruct
    public void startWorker() {
        log.info("Starting Temporal worker on queue: {}", TaskQueue.CUSTOMER_QUEUE);

        Worker worker = workerFactory.newWorker(TaskQueue.CUSTOMER_QUEUE, defaultWorkerOptions);

        worker.registerActivitiesImplementations(
                new ProfileCreationActivityImpl(createCustomerUseCase),
                new UpdateCustomerActivityImpl(updateCustomerUseCase, manageBankAccountsUseCase, submitPepAnswerUseCase)
        );

        workerFactory.start();
        log.info("Temporal worker started on queue: {} with 2 activity implementations", TaskQueue.CUSTOMER_QUEUE);
    }
}
