package com.ksa.islamic.orchestration.activity;

import io.temporal.activity.ActivityOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Activity Retry Policy Tests")
class ActivityRetryPolicyTest {

    @Test
    @DisplayName("Should create default retry policy with correct parameters")
    void testDefaultRetryPolicy() {
        // Act
        ActivityOptions options = ActivityRetryPolicy.defaultRetry();

        // Assert
        assertThat(options.getStartToCloseTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(options.getScheduleToCloseTimeout()).isEqualTo(Duration.ofMinutes(5));
        assertThat(options.getRetryOptions()).isNotNull();
        assertThat(options.getRetryOptions().getMaximumAttempts()).isEqualTo(5);
        assertThat(options.getRetryOptions().getInitialInterval()).isEqualTo(Duration.ofSeconds(1));
        assertThat(options.getRetryOptions().getBackoffCoefficient()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should create aggressive retry policy with more attempts")
    void testAggressiveRetryPolicy() {
        // Act
        ActivityOptions options = ActivityRetryPolicy.aggressiveRetry();

        // Assert
        assertThat(options.getStartToCloseTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(options.getScheduleToCloseTimeout()).isEqualTo(Duration.ofMinutes(30));
        assertThat(options.getRetryOptions()).isNotNull();
        assertThat(options.getRetryOptions().getMaximumAttempts()).isEqualTo(10);
        assertThat(options.getRetryOptions().getBackoffCoefficient()).isEqualTo(1.5);
    }

    @Test
    @DisplayName("Should create conservative retry policy with fewer attempts")
    void testConservativeRetryPolicy() {
        // Act
        ActivityOptions options = ActivityRetryPolicy.conservativeRetry();

        // Assert
        assertThat(options.getStartToCloseTimeout()).isEqualTo(Duration.ofSeconds(20));
        assertThat(options.getScheduleToCloseTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(options.getRetryOptions()).isNotNull();
        assertThat(options.getRetryOptions().getMaximumAttempts()).isEqualTo(3);
        assertThat(options.getRetryOptions().getInitialInterval()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Should create external API retry policy with longer intervals")
    void testExternalApiRetryPolicy() {
        // Act
        ActivityOptions options = ActivityRetryPolicy.externalApiRetry();

        // Assert
        assertThat(options.getStartToCloseTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(options.getScheduleToCloseTimeout()).isEqualTo(Duration.ofMinutes(15));
        assertThat(options.getRetryOptions()).isNotNull();
        assertThat(options.getRetryOptions().getMaximumAttempts()).isEqualTo(7);
        assertThat(options.getRetryOptions().getInitialInterval()).isEqualTo(Duration.ofSeconds(2));
        assertThat(options.getRetryOptions().getMaximumInterval()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    @DisplayName("Should create no retry policy with single attempt")
    void testNoRetryPolicy() {
        // Act
        ActivityOptions options = ActivityRetryPolicy.noRetry();

        // Assert
        assertThat(options.getRetryOptions()).isNotNull();
        assertThat(options.getRetryOptions().getMaximumAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should create long-running policy with heartbeat timeout")
    void testLongRunningPolicy() {
        // Act
        ActivityOptions options = ActivityRetryPolicy.longRunning();

        // Assert
        assertThat(options.getStartToCloseTimeout()).isEqualTo(Duration.ofHours(1));
        assertThat(options.getScheduleToCloseTimeout()).isEqualTo(Duration.ofHours(2));
        assertThat(options.getHeartbeatTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(options.getRetryOptions()).isNotNull();
        assertThat(options.getRetryOptions().getMaximumAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should create custom retry policy with builder")
    void testCustomRetryPolicy() {
        // Act
        ActivityOptions options = ActivityRetryPolicy.custom()
                .withStartToCloseTimeout(Duration.ofMinutes(5))
                .withMaxAttempts(7)
                .withInitialInterval(Duration.ofSeconds(3))
                .withBackoffCoefficient(1.8)
                .withHeartbeatTimeout(Duration.ofSeconds(30))
                .build();

        // Assert
        assertThat(options.getStartToCloseTimeout()).isEqualTo(Duration.ofMinutes(5));
        assertThat(options.getHeartbeatTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(options.getRetryOptions()).isNotNull();
        assertThat(options.getRetryOptions().getMaximumAttempts()).isEqualTo(7);
        assertThat(options.getRetryOptions().getInitialInterval()).isEqualTo(Duration.ofSeconds(3));
        assertThat(options.getRetryOptions().getBackoffCoefficient()).isEqualTo(1.8);
    }
}