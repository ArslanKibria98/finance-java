package com.ksa.financing.lending.infrastructure.config;

import com.ksa.financing.lending.adapter.temporal.activity.LoanApplicationActivityImpl;
import com.ksa.financing.lending.adapter.temporal.workflow.LoanApplicationWorkflowImpl;
import com.ksa.islamic.orchestration.activity.lending.LoanApplicationWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
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

    @Value("${temporal.service-address}")
    private String temporalAddress;

    @Value("${temporal.namespace}")
    private String namespace;

    @Value("${temporal.task-queue}")
    private String taskQueue;

    private final LoanApplicationActivityImpl loanApplicationActivity;

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
        worker.registerWorkflowImplementationTypes(LoanApplicationWorkflowImpl.class);

        // Register activity implementations (Spring beans)
        worker.registerActivitiesImplementations(loanApplicationActivity);

        log.info("Temporal worker configured for queue: {}", taskQueue);
        return workerFactory;
    }

    @PostConstruct
    public void startWorker() {
        if (workerFactory != null) {
            workerFactory.start();
            log.info("Temporal worker started on queue: {}", taskQueue);
        }
    }

    @PreDestroy
    public void stopWorker() {
        if (workerFactory != null) {
            workerFactory.shutdown();
            log.info("Temporal worker stopped");
        }
    }
}
