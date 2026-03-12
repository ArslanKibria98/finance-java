package com.ksa.financing.product.infrastructure.config;

import com.ksa.financing.product.adapter.temporal.activity.FineractProductActivityImpl;
import com.ksa.financing.product.adapter.temporal.activity.ProductLifecycleActivityImpl;
import com.ksa.financing.product.adapter.temporal.activity.ProductValidationActivityImpl;
import com.ksa.financing.product.adapter.temporal.activity.ShariaComplianceActivityImpl;
import com.ksa.financing.product.adapter.temporal.workflow.ProductActivationWorkflowImpl;
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

    @Value("${temporal.task-queue:product-activation-queue}")
    private String taskQueue;

    private final ProductValidationActivityImpl productValidationActivity;
    private final ShariaComplianceActivityImpl shariaComplianceActivity;
    private final FineractProductActivityImpl fineractProductActivity;
    private final ProductLifecycleActivityImpl productLifecycleActivity;

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

        // Register workflow implementation
        worker.registerWorkflowImplementationTypes(ProductActivationWorkflowImpl.class);

        // Register activity implementations
        worker.registerActivitiesImplementations(
                productValidationActivity,
                shariaComplianceActivity,
                fineractProductActivity,
                productLifecycleActivity
        );

        log.info("Temporal worker configured for queue: {} with 4 activity implementations", taskQueue);

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
