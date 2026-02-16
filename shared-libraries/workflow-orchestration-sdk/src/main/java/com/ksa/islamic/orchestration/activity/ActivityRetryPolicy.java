package com.ksa.islamic.orchestration.activity;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;

import java.time.Duration;
import java.util.List;

/**
 * Standard retry policies for different activity types
 *
 * Provides pre-configured retry policies for common scenarios:
 * - Default: Standard operations
 * - Aggressive: Critical operations that must succeed
 * - Conservative: Expensive or rate-limited operations
 * - External API: Network calls with backoff
 */
public class ActivityRetryPolicy {

    // Non-retryable error types
    private static final List<String> NON_RETRYABLE_ERRORS = List.of(
            "ValidationException",
            "BusinessRuleViolation",
            "InsufficientFundsException",
            "CustomerBlacklistedException",
            "DuplicateRequestException"
    );

    /**
     * Default retry policy - 5 attempts with exponential backoff
     * Use for standard internal operations
     */
    public static ActivityOptions defaultRetry() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setScheduleToCloseTimeout(Duration.ofMinutes(5))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setBackoffCoefficient(2.0)
                        .setMaximumInterval(Duration.ofSeconds(30))
                        .setMaximumAttempts(5)
                        .setDoNotRetry(NON_RETRYABLE_ERRORS.toArray(String[]::new))
                        .build())
                .build();
    }

    /**
     * Aggressive retry policy - 10 attempts for critical operations
     * Use for operations that must succeed (e.g., critical state updates)
     */
    public static ActivityOptions aggressiveRetry() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(1))
                .setScheduleToCloseTimeout(Duration.ofMinutes(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setBackoffCoefficient(1.5)
                        .setMaximumInterval(Duration.ofMinutes(1))
                        .setMaximumAttempts(10)
                        .setDoNotRetry(NON_RETRYABLE_ERRORS.toArray(String[]::new))
                        .build())
                .build();
    }

    /**
     * Conservative retry policy - 3 attempts for expensive operations
     * Use for costly or rate-limited operations
     */
    public static ActivityOptions conservativeRetry() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(20))
                .setScheduleToCloseTimeout(Duration.ofMinutes(2))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(5))
                        .setBackoffCoefficient(2.0)
                        .setMaximumInterval(Duration.ofSeconds(30))
                        .setMaximumAttempts(3)
                        .setDoNotRetry(NON_RETRYABLE_ERRORS.toArray(String[]::new))
                        .build())
                .build();
    }

    /**
     * External API retry policy - 7 attempts with longer backoff
     * Use for external API calls (Simah, Nafath, etc.)
     */
    public static ActivityOptions externalApiRetry() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .setScheduleToCloseTimeout(Duration.ofMinutes(15))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(2))
                        .setBackoffCoefficient(2.0)
                        .setMaximumInterval(Duration.ofMinutes(2))
                        .setMaximumAttempts(7)
                        .setDoNotRetry(NON_RETRYABLE_ERRORS.toArray(String[]::new))
                        .build())
                .build();
    }

    /**
     * No retry policy - for operations that should not be retried
     */
    public static ActivityOptions noRetry() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(1)
                        .build())
                .build();
    }

    /**
     * Long-running activity policy - for batch operations
     * Includes heartbeat timeout for progress tracking
     */
    public static ActivityOptions longRunning() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofHours(1))
                .setScheduleToCloseTimeout(Duration.ofHours(2))
                .setHeartbeatTimeout(Duration.ofMinutes(1))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(10))
                        .setBackoffCoefficient(1.5)
                        .setMaximumInterval(Duration.ofMinutes(5))
                        .setMaximumAttempts(3)
                        .build())
                .build();
    }

    /**
     * Custom retry policy builder
     */
    public static ActivityOptionsBuilder custom() {
        return new ActivityOptionsBuilder();
    }

    /**
     * Builder for custom activity options
     */
    public static class ActivityOptionsBuilder {
        private Duration startToCloseTimeout = Duration.ofSeconds(30);
        private Duration scheduleToCloseTimeout = Duration.ofMinutes(5);
        private Duration heartbeatTimeout;
        private int maxAttempts = 5;
        private Duration initialInterval = Duration.ofSeconds(1);
        private double backoffCoefficient = 2.0;
        private Duration maximumInterval = Duration.ofSeconds(30);
        private List<String> doNotRetry = NON_RETRYABLE_ERRORS;

        public ActivityOptionsBuilder withStartToCloseTimeout(Duration timeout) {
            this.startToCloseTimeout = timeout;
            return this;
        }

        public ActivityOptionsBuilder withScheduleToCloseTimeout(Duration timeout) {
            this.scheduleToCloseTimeout = timeout;
            return this;
        }

        public ActivityOptionsBuilder withHeartbeatTimeout(Duration timeout) {
            this.heartbeatTimeout = timeout;
            return this;
        }

        public ActivityOptionsBuilder withMaxAttempts(int attempts) {
            this.maxAttempts = attempts;
            return this;
        }

        public ActivityOptionsBuilder withInitialInterval(Duration interval) {
            this.initialInterval = interval;
            return this;
        }

        public ActivityOptionsBuilder withBackoffCoefficient(double coefficient) {
            this.backoffCoefficient = coefficient;
            return this;
        }

        public ActivityOptionsBuilder withMaximumInterval(Duration interval) {
            this.maximumInterval = interval;
            return this;
        }

        public ActivityOptionsBuilder withDoNotRetry(List<String> errors) {
            this.doNotRetry = errors;
            return this;
        }

        public ActivityOptions build() {
            ActivityOptions.Builder builder = ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(startToCloseTimeout)
                    .setScheduleToCloseTimeout(scheduleToCloseTimeout);

            if (heartbeatTimeout != null) {
                builder.setHeartbeatTimeout(heartbeatTimeout);
            }

            builder.setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(initialInterval)
                    .setBackoffCoefficient(backoffCoefficient)
                    .setMaximumInterval(maximumInterval)
                    .setMaximumAttempts(maxAttempts)
                    .setDoNotRetry(doNotRetry.toArray(String[]::new))
                    .build());

            return builder.build();
        }
    }
}