package com.ksa.islamic.orchestration.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Prometheus metrics for Temporal workflows
 *
 * Provides comprehensive metrics for monitoring workflow execution,
 * activity performance, and system health.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemporalMetrics {

    private final MeterRegistry meterRegistry;

    // Metric name prefixes
    private static final String WORKFLOW_PREFIX = "temporal.workflow";
    private static final String ACTIVITY_PREFIX = "temporal.activity";
    private static final String WORKER_PREFIX = "temporal.worker";

    // ========== Workflow Metrics ==========

    /**
     * Record workflow start
     */
    public void recordWorkflowStart(String workflowType, String taskQueue) {
        Counter.builder(WORKFLOW_PREFIX + ".started")
                .description("Number of workflows started")
                .tag("workflow_type", workflowType)
                .tag("task_queue", taskQueue)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record workflow completion
     */
    public void recordWorkflowCompletion(String workflowType, String taskQueue, Duration duration, boolean success) {
        // Record completion counter
        Counter.builder(WORKFLOW_PREFIX + ".completed")
                .description("Number of workflows completed")
                .tag("workflow_type", workflowType)
                .tag("task_queue", taskQueue)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();

        // Record duration
        Timer.builder(WORKFLOW_PREFIX + ".duration")
                .description("Workflow execution duration")
                .tag("workflow_type", workflowType)
                .tag("task_queue", taskQueue)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .record(duration);

        log.debug("Workflow {} completed in {}ms with status: {}",
                workflowType, duration.toMillis(), success ? "success" : "failure");
    }

    /**
     * Record workflow failure
     */
    public void recordWorkflowFailure(String workflowType, String taskQueue, String errorType) {
        Counter.builder(WORKFLOW_PREFIX + ".failed")
                .description("Number of workflow failures")
                .tag("workflow_type", workflowType)
                .tag("task_queue", taskQueue)
                .tag("error_type", errorType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record workflow retry
     */
    public void recordWorkflowRetry(String workflowType, int attemptNumber) {
        Counter.builder(WORKFLOW_PREFIX + ".retries")
                .description("Number of workflow retries")
                .tag("workflow_type", workflowType)
                .tag("attempt", String.valueOf(attemptNumber))
                .register(meterRegistry)
                .increment();
    }

    // ========== Activity Metrics ==========

    /**
     * Record activity execution
     */
    public Timer.Sample startActivityTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Record activity completion
     */
    public void recordActivityCompletion(Timer.Sample sample, String activityType, String taskQueue, boolean success) {
        sample.stop(Timer.builder(ACTIVITY_PREFIX + ".duration")
                .description("Activity execution duration")
                .tag("activity_type", activityType)
                .tag("task_queue", taskQueue)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry));

        Counter.builder(ACTIVITY_PREFIX + ".completed")
                .description("Number of activities completed")
                .tag("activity_type", activityType)
                .tag("task_queue", taskQueue)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record activity failure
     */
    public void recordActivityFailure(String activityType, String taskQueue, String errorType) {
        Counter.builder(ACTIVITY_PREFIX + ".failed")
                .description("Number of activity failures")
                .tag("activity_type", activityType)
                .tag("task_queue", taskQueue)
                .tag("error_type", errorType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record activity retry
     */
    public void recordActivityRetry(String activityType, int attemptNumber) {
        Counter.builder(ACTIVITY_PREFIX + ".retries")
                .description("Number of activity retries")
                .tag("activity_type", activityType)
                .tag("attempt", String.valueOf(attemptNumber))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record activity heartbeat
     */
    public void recordActivityHeartbeat(String activityType) {
        Counter.builder(ACTIVITY_PREFIX + ".heartbeats")
                .description("Number of activity heartbeats")
                .tag("activity_type", activityType)
                .register(meterRegistry)
                .increment();
    }

    // ========== Worker Metrics ==========

    /**
     * Record worker task execution
     */
    public void recordWorkerTaskExecution(String taskQueue, String taskType) {
        Counter.builder(WORKER_PREFIX + ".tasks.executed")
                .description("Number of tasks executed by workers")
                .tag("task_queue", taskQueue)
                .tag("task_type", taskType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Update worker task queue size
     */
    public void updateTaskQueueSize(String taskQueue, int size) {
        meterRegistry.gauge(WORKER_PREFIX + ".queue.size",
                java.util.List.of(
                        io.micrometer.core.instrument.Tag.of("task_queue", taskQueue)
                ),
                size);
    }

    /**
     * Record worker polling
     */
    public void recordWorkerPolling(String taskQueue) {
        Counter.builder(WORKER_PREFIX + ".polls")
                .description("Number of worker polls")
                .tag("task_queue", taskQueue)
                .register(meterRegistry)
                .increment();
    }

    // ========== SAGA Metrics ==========

    /**
     * Record SAGA step execution
     */
    public void recordSagaStepExecution(String sagaName, String stepName, boolean success) {
        Counter.builder("temporal.saga.step.executed")
                .description("Number of SAGA steps executed")
                .tag("saga_name", sagaName)
                .tag("step_name", stepName)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record SAGA compensation
     */
    public void recordSagaCompensation(String sagaName, String stepName) {
        Counter.builder("temporal.saga.compensation")
                .description("Number of SAGA compensations executed")
                .tag("saga_name", sagaName)
                .tag("step_name", stepName)
                .register(meterRegistry)
                .increment();
    }

    // ========== Custom Business Metrics ==========

    /**
     * Record custom business metric
     */
    public void recordBusinessMetric(String name, double value, String... tags) {
        if (tags.length % 2 != 0) {
            throw new IllegalArgumentException("Tags must be provided in key-value pairs");
        }

        io.micrometer.core.instrument.Tag[] metricTags = new io.micrometer.core.instrument.Tag[tags.length / 2];
        for (int i = 0; i < tags.length; i += 2) {
            metricTags[i / 2] = io.micrometer.core.instrument.Tag.of(tags[i], tags[i + 1]);
        }

        meterRegistry.counter("temporal.business." + name, metricTags).increment(value);
    }

    /**
     * Record custom timing metric
     */
    public void recordTiming(String name, long duration, TimeUnit unit, String... tags) {
        if (tags.length % 2 != 0) {
            throw new IllegalArgumentException("Tags must be provided in key-value pairs");
        }

        io.micrometer.core.instrument.Tag[] metricTags = new io.micrometer.core.instrument.Tag[tags.length / 2];
        for (int i = 0; i < tags.length; i += 2) {
            metricTags[i / 2] = io.micrometer.core.instrument.Tag.of(tags[i], tags[i + 1]);
        }

        Timer.builder("temporal.timing." + name)
                .tags(metricTags)
                .register(meterRegistry)
                .record(duration, unit);
    }
}