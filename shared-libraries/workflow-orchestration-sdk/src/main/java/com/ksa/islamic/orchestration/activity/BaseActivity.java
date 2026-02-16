package com.ksa.islamic.orchestration.activity;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;

/**
 * Base class for all Temporal activities
 *
 * Provides common functionality:
 * - Logging with activity context
 * - Heartbeat support for long-running activities
 * - Access to activity execution context
 * - Multi-tenant context propagation
 */
@Slf4j
public abstract class BaseActivity {

    /**
     * Gets the current activity execution context
     */
    protected ActivityExecutionContext getContext() {
        return Activity.getExecutionContext();
    }

    /**
     * Gets the current workflow ID
     */
    protected String getWorkflowId() {
        return getContext().getInfo().getWorkflowId();
    }

    /**
     * Gets the current activity type name
     */
    protected String getActivityType() {
        return getContext().getInfo().getActivityType();
    }

    /**
     * Gets the current activity ID
     */
    protected String getActivityId() {
        return getContext().getInfo().getActivityId();
    }

    /**
     * Gets tenant ID from activity context if available
     */
    protected Optional<String> getTenantId() {
        try {
            ActivityExecutionContext context = getContext();
            // Tenant ID would be passed through workflow metadata/headers
            // This is a placeholder for actual implementation
            return Optional.empty();
        } catch (Exception e) {
            log.debug("No tenant context available");
            return Optional.empty();
        }
    }

    /**
     * Records a heartbeat for long-running activities
     *
     * @param details Progress details to include in heartbeat
     */
    protected void heartbeat(Object details) {
        try {
            Activity.getExecutionContext().heartbeat(details);
            log.debug("Heartbeat recorded for activity {} with details: {}",
                    getActivityType(), details);
        } catch (Exception e) {
            log.warn("Failed to record heartbeat: {}", e.getMessage());
        }
    }

    /**
     * Records a simple heartbeat without details
     */
    protected void heartbeat() {
        heartbeat(null);
    }

    /**
     * Executes an operation with periodic heartbeats
     *
     * @param operation The operation to execute
     * @param heartbeatInterval Interval between heartbeats
     * @param progressSupplier Supplier for progress details
     */
    protected <T> T executeWithHeartbeat(
            java.util.function.Supplier<T> operation,
            Duration heartbeatInterval,
            java.util.function.Supplier<Object> progressSupplier) {

        // Start heartbeat thread
        Thread heartbeatThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(heartbeatInterval.toMillis());
                    heartbeat(progressSupplier != null ? progressSupplier.get() : null);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        heartbeatThread.setDaemon(true);
        heartbeatThread.start();

        try {
            return operation.get();
        } finally {
            heartbeatThread.interrupt();
        }
    }

    /**
     * Logs activity start
     */
    protected void logStart(String message, Object... args) {
        log.info("[WorkflowId: {}, ActivityType: {}] Starting - " + message,
                concatenate(new Object[]{getWorkflowId(), getActivityType()}, args));
    }

    /**
     * Logs activity completion
     */
    protected void logComplete(String message, Object... args) {
        log.info("[WorkflowId: {}, ActivityType: {}] Completed - " + message,
                concatenate(new Object[]{getWorkflowId(), getActivityType()}, args));
    }

    /**
     * Logs activity error
     */
    protected void logError(String message, Throwable error, Object... args) {
        log.error("[WorkflowId: {}, ActivityType: {}] Error - " + message,
                concatenate(new Object[]{getWorkflowId(), getActivityType()}, args), error);
    }

    /**
     * Helper to concatenate arrays
     */
    private static Object[] concatenate(Object[] first, Object[] second) {
        Object[] result = new Object[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    /**
     * Template method for activity execution with standard logging
     */
    protected <T> T executeActivity(String operationName, java.util.function.Supplier<T> operation) {
        logStart(operationName);
        try {
            T result = operation.get();
            logComplete(operationName);
            return result;
        } catch (Exception e) {
            logError(operationName + " failed", e);
            throw e;
        }
    }
}