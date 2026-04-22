package com.ksa.financing.lending.infrastructure.config;

import com.ksa.financing.lending.adapter.temporal.activity.*;
import com.ksa.financing.lending.adapter.temporal.workflow.LoanApplicationWorkflowImpl;
import com.ksa.financing.lending.adapter.temporal.workflow.LoanRescheduleWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "temporal.worker.enabled", havingValue = "true", matchIfMissing = true)
public class TemporalConfig {

    @Value("${temporal.service-address:${TEMPORAL_ADDRESS:localhost:7233}}")
    private String temporalAddress;

    @Value("${temporal.namespace:${TEMPORAL_NAMESPACE:default}}")
    private String namespace;

    @Value("${temporal.task-queue:loan-application-queue}")
    private String taskQueue;

    // ══════════ Activity Implementations (Spring-managed beans) ══════════
    private final LoanApplicationActivityImpl loanApplicationActivity;
    private final ProductValidationActivityImpl productValidationActivity;
    private final CustomerValidationActivityImpl customerValidationActivity;
    private final CreditCheckActivityImpl creditCheckActivity;
    private final ThirdPartyActivityImpl thirdPartyActivity;
    private final ContractActivityImpl contractActivity;
    private final DisbursementActivityImpl disbursementActivity;
    private final LedgerActivityImpl ledgerActivity;
    private final RescheduleActivityImpl rescheduleActivity;

    private WorkerFactory workerFactory;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(temporalAddress)
                        .build()
        );
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs stubs) {
        return WorkflowClient.newInstance(stubs);
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient client) {
        this.workerFactory = WorkerFactory.newInstance(client);

        Worker worker = workerFactory.newWorker(taskQueue);

        // Register workflow implementations
        worker.registerWorkflowImplementationTypes(
                LoanApplicationWorkflowImpl.class,
                LoanRescheduleWorkflowImpl.class
        );

        // Register ALL activity implementations on the same task queue for MVP.
        // In production, external activities (product-service, customer-service, risk-service)
        // would run on their own task queues in their respective services.
        worker.registerActivitiesImplementations(
                loanApplicationActivity,       // Lending-service internal CRUD
                productValidationActivity,     // Calls product-service REST
                customerValidationActivity,    // Calls customer-service REST
                creditCheckActivity,           // Calls risk-service REST (→ SIMAH via middleware)
                thirdPartyActivity,            // Calls middleware-third-party (IBAN, commodity, IVR, e-promissory)
                contractActivity,              // Calls middleware-third-party (contract gen, OTP)
                disbursementActivity,          // Calls Fineract + middleware (payment gateway)
                ledgerActivity,                // Calls ledger-service → posts GL entries → Fineract GL sync
                rescheduleActivity             // Loan rescheduling — eligibility, schedule, Fineract proxy via ledger-service
        );

        log.info("Temporal worker configured for queue: {} with 9 activity implementations", taskQueue);

        workerFactory.start();
        log.info("Temporal worker started on queue: {}", taskQueue);

        return workerFactory;
    }

    @PreDestroy
    public void stopWorker() {
        if (workerFactory != null) {
            workerFactory.shutdown();
            log.info("Temporal worker stopped");
        }
    }
}
