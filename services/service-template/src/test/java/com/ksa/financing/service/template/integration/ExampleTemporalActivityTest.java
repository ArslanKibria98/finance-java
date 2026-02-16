package com.ksa.financing.service.template.integration;

import com.ksa.financing.domain.model.TenantId;
import com.ksa.financing.domain.model.UserId;
import com.ksa.financing.service.template.adapter.temporal.activity.ExampleActivity;
import com.ksa.financing.service.template.adapter.temporal.activity.ExampleActivityImpl;
import com.ksa.financing.service.template.domain.model.ExampleAggregate;
import com.ksa.financing.service.template.domain.port.out.ExampleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Temporal Activity test template.
 * Tests Temporal activities with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Example Temporal Activity Tests")
class ExampleTemporalActivityTest {

    @Mock
    private ExampleRepository repository;

    private ExampleActivity activity;

    @BeforeEach
    void setUp() {
        activity = new ExampleActivityImpl(repository);
    }

    @Test
    @DisplayName("Should process example successfully")
    void shouldProcessExampleSuccessfully() {
        // Given
        var request = new ExampleActivity.ProcessRequest(
                "tenant-123",
                "EXA-456",
                "APPROVE"
        );

        var aggregate = ExampleAggregate.create(
                new TenantId("tenant-123"),
                "Test",
                "Description",
                new UserId("user-1")
        );
        aggregate.activate(new UserId("user-1"));

        when(repository.findById(any(), any()))
                .thenReturn(Optional.of(aggregate));

        // When
        var result = activity.processExample(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.aggregateId()).isEqualTo("EXA-456");
        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.message()).contains("successfully");
    }

    @Test
    @DisplayName("Should handle aggregate not found")
    void shouldHandleAggregateNotFound() {
        // Given
        var request = new ExampleActivity.ProcessRequest(
                "tenant-123",
                "EXA-999",
                "APPROVE"
        );

        when(repository.findById(any(), any()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> activity.processExample(request))
                .hasMessageContaining("Aggregate not found");
    }

    @Test
    @DisplayName("Should validate example successfully")
    void shouldValidateExampleSuccessfully() {
        // Given
        var request = new ExampleActivity.ValidationRequest(
                "tenant-123",
                "EXA-456"
        );

        var aggregate = ExampleAggregate.create(
                new TenantId("tenant-123"),
                "Test",
                "Description",
                new UserId("user-1")
        );
        aggregate.activate(new UserId("user-1"));
        aggregate.addEntity("Entity", "Value", new UserId("user-1"));

        when(repository.findById(any(), any()))
                .thenReturn(Optional.of(aggregate));

        // When
        var result = activity.validateExample(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.valid()).isTrue();
        assertThat(result.reason()).isEqualTo("Valid");
    }

    @Test
    @DisplayName("Should return invalid for cancelled aggregate")
    void shouldReturnInvalidForCancelledAggregate() {
        // Given
        var request = new ExampleActivity.ValidationRequest(
                "tenant-123",
                "EXA-456"
        );

        var aggregate = ExampleAggregate.builder()
                .id(com.ksa.financing.service.template.domain.model.ExampleAggregateId.of("EXA-456"))
                .tenantId(new TenantId("tenant-123"))
                .name("Test")
                .status(com.ksa.financing.service.template.domain.model.ExampleStatus.CANCELLED)
                .createdBy(new UserId("user-1"))
                .createdAt(java.time.LocalDateTime.now())
                .entities(new java.util.ArrayList<>())
                .build();

        when(repository.findById(any(), any()))
                .thenReturn(Optional.of(aggregate));

        // When
        var result = activity.validateExample(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isEqualTo("Aggregate is cancelled");
    }

    @Test
    @DisplayName("Should send notification successfully")
    void shouldSendNotificationSuccessfully() {
        // Given
        var request = new ExampleActivity.NotificationRequest(
                "tenant-123",
                "EXA-456",
                "EMAIL",
                "user@example.com"
        );

        // When & Then - Should complete without exception
        activity.sendNotification(request);
    }
}