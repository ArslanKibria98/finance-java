package com.ksa.islamic.orchestration.config;

import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Worker Configuration for Temporal
 *
 * Manages worker factory and worker registration for task queues.
 * Supports automatic worker creation and lifecycle management.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WorkerConfig {

    @Value("${temporal.worker.max-concurrent-activities:100}")
    private int maxConcurrentActivities;

    @Value("${temporal.worker.max-concurrent-workflows:50}")
    private int maxConcurrentWorkflows;

    @Value("${temporal.worker.max-activities-per-second:0}")
    private double maxActivitiesPerSecond;

    @Value("${temporal.worker.max-task-queue-activities-per-second:0}")
    private double maxTaskQueueActivitiesPerSecond;

    /**
     * Creates worker factory for creating workers
     */
    @Bean
    @ConditionalOnMissingBean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        log.info("Creating WorkerFactory");
        return WorkerFactory.newInstance(workflowClient);
    }

    /**
     * Creates worker options with configured limits
     */
    @Bean
    public WorkerOptions defaultWorkerOptions() {
        WorkerOptions.Builder builder = WorkerOptions.newBuilder()
                .setMaxConcurrentActivityExecutionSize(maxConcurrentActivities)
                .setMaxConcurrentWorkflowTaskExecutionSize(maxConcurrentWorkflows);

        if (maxActivitiesPerSecond > 0) {
            builder.setMaxWorkerActivitiesPerSecond(maxActivitiesPerSecond);
        }

        if (maxTaskQueueActivitiesPerSecond > 0) {
            builder.setMaxTaskQueueActivitiesPerSecond(maxTaskQueueActivitiesPerSecond);
        }

        WorkerOptions options = builder.build();

        log.info("Worker options configured: maxConcurrentActivities={}, maxConcurrentWorkflows={}",
                maxConcurrentActivities, maxConcurrentWorkflows);

        return options;
    }

    /**
     * Helper method to create a worker for a specific task queue
     */
    public Worker createWorker(WorkerFactory workerFactory, String taskQueue) {
        return createWorker(workerFactory, taskQueue, defaultWorkerOptions());
    }

    /**
     * Helper method to create a worker with custom options
     */
    public Worker createWorker(WorkerFactory workerFactory, String taskQueue, WorkerOptions options) {
        log.info("Creating worker for task queue: {}", taskQueue);
        return workerFactory.newWorker(taskQueue, options);
    }

    /**
     * Registers workflows and activities with a worker
     */
    public void registerWorkflowsAndActivities(Worker worker,
                                               Class<?>[] workflowImplementations,
                                               Object[] activityImplementations) {
        // Register workflows
        if (workflowImplementations != null) {
            for (Class<?> workflowImpl : workflowImplementations) {
                worker.registerWorkflowImplementationTypes(workflowImpl);
                log.debug("Registered workflow implementation: {}", workflowImpl.getSimpleName());
            }
        }

        // Register activities
        if (activityImplementations != null) {
            for (Object activityImpl : activityImplementations) {
                worker.registerActivitiesImplementations(activityImpl);
                log.debug("Registered activity implementation: {}", activityImpl.getClass().getSimpleName());
            }
        }
    }
}