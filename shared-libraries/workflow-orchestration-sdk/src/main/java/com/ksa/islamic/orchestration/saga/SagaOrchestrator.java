package com.ksa.islamic.orchestration.saga;

import io.temporal.workflow.Workflow;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orchestrator for executing SAGA transactions
 *
 * Manages the execution of saga steps and handles compensation
 * when failures occur. Ensures all-or-nothing semantics for
 * distributed transactions.
 */
@Slf4j
public class SagaOrchestrator<T> {

    private final List<SagaStep<?, ?>> steps;
    private final List<CompensationRecord> compensations;
    private final SagaOptions options;

    @Builder
    public SagaOrchestrator(List<SagaStep<?, ?>> steps, SagaOptions options) {
        this.steps = steps != null ? new ArrayList<>(steps) : new ArrayList<>();
        this.compensations = new ArrayList<>();
        this.options = options != null ? options : SagaOptions.defaultOptions();
    }

    /**
     * Execute the saga with the given input
     */
    @SuppressWarnings("unchecked")
    public SagaResult<T> execute(T initialInput) {
        Object currentInput = initialInput;
        Object lastResult = null;

        log.info("Starting SAGA execution with {} steps", steps.size());

        for (int i = 0; i < steps.size(); i++) {
            SagaStep<Object, Object> step = (SagaStep<Object, Object>) steps.get(i);

            try {
                log.debug("Executing step {}: {}", i + 1, step.getName());

                // Execute the step
                Object result = executeStep(step, currentInput);

                // Record compensation if needed
                if (step.isCompensatable() && step.getCompensation() != null) {
                    compensations.add(new CompensationRecord(step, result));
                }

                // Use result as input for next step if configured
                if (options.isChainStepResults()) {
                    currentInput = result;
                }

                lastResult = result;

                log.debug("Step {} completed successfully", step.getName());

            } catch (Exception e) {
                log.error("Step {} failed: {}", step.getName(), e.getMessage());

                if (step.isCritical()) {
                    // Compensate all previous steps
                    compensate(e);

                    // Return failure result
                    return SagaResult.<T>builder()
                            .success(false)
                            .failedStep(step.getName())
                            .error(e)
                            .compensated(true)
                            .build();
                } else {
                    log.warn("Non-critical step {} failed, continuing saga", step.getName());
                }
            }
        }

        log.info("SAGA completed successfully");

        return SagaResult.<T>builder()
                .success(true)
                .result((T) lastResult)
                .build();
    }

    /**
     * Execute a single step with retry logic
     */
    private Object executeStep(SagaStep<Object, Object> step, Object input) throws Exception {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < step.getMaxRetries()) {
            attempts++;

            try {
                return step.execute(input);
            } catch (Exception e) {
                lastException = e;

                if (attempts < step.getMaxRetries()) {
                    long backoffMillis = calculateBackoff(attempts);
                    log.warn("Step {} failed on attempt {}, retrying after {}ms",
                            step.getName(), attempts, backoffMillis);

                    // Use Workflow.sleep for deterministic sleep in Temporal
                    if (isInWorkflowContext()) {
                        Workflow.sleep(java.time.Duration.ofMillis(backoffMillis));
                    } else {
                        Thread.sleep(backoffMillis);
                    }
                }
            }
        }

        throw lastException;
    }

    /**
     * Compensate all completed steps in reverse order
     */
    private void compensate(Exception originalError) {
        if (compensations.isEmpty()) {
            log.info("No compensations to execute");
            return;
        }

        log.info("Starting compensation for {} steps", compensations.size());

        // Reverse the list to compensate in reverse order
        List<CompensationRecord> toCompensate = new ArrayList<>(compensations);
        Collections.reverse(toCompensate);

        if (options.isParallelCompensation()) {
            compensateParallel(toCompensate);
        } else {
            compensateSequential(toCompensate);
        }
    }

    /**
     * Compensate steps sequentially
     */
    @SuppressWarnings("unchecked")
    private void compensateSequential(List<CompensationRecord> toCompensate) {
        for (CompensationRecord record : toCompensate) {
            try {
                log.debug("Compensating step: {}", record.step.getName());
                SagaStep<Object, Object> step = (SagaStep<Object, Object>) record.step;
                step.compensate(record.result);
                log.debug("Compensation completed for step: {}", record.step.getName());
            } catch (Exception e) {
                log.error("Compensation failed for step {}: {}",
                        record.step.getName(), e.getMessage());
                // Continue compensating other steps
            }
        }
    }

    /**
     * Compensate steps in parallel (not yet implemented)
     */
    private void compensateParallel(List<CompensationRecord> toCompensate) {
        // In Temporal, we would use async activities here
        // For now, fall back to sequential
        compensateSequential(toCompensate);
    }

    /**
     * Calculate exponential backoff for retries
     */
    private long calculateBackoff(int attempt) {
        return Math.min(
                (long) (options.getInitialBackoffMillis() * Math.pow(2, attempt - 1)),
                options.getMaxBackoffMillis()
        );
    }

    /**
     * Check if we're running in a Temporal workflow context
     */
    private boolean isInWorkflowContext() {
        try {
            Workflow.getInfo();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Add a step to the saga
     */
    public <I, O> SagaOrchestrator<T> addStep(SagaStep<I, O> step) {
        this.steps.add(step);
        return this;
    }

    /**
     * Record of a completed step that needs compensation
     */
    private record CompensationRecord(SagaStep<?, ?> step, Object result) {}

    /**
     * Options for saga execution
     */
    @Data
    @Builder
    public static class SagaOptions {
        @Builder.Default
        private boolean parallelCompensation = false;

        @Builder.Default
        private boolean chainStepResults = false;

        @Builder.Default
        private long initialBackoffMillis = 1000;

        @Builder.Default
        private long maxBackoffMillis = 30000;

        public static SagaOptions defaultOptions() {
            return SagaOptions.builder().build();
        }
    }

    /**
     * Result of saga execution
     */
    @Data
    @Builder
    public static class SagaResult<T> {
        private final boolean success;
        private final T result;
        private final String failedStep;
        private final Exception error;
        private final boolean compensated;
    }
}