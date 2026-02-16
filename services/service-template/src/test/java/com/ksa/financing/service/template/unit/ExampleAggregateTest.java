package com.ksa.financing.service.template.unit;

import com.ksa.financing.domain.model.TenantId;
import com.ksa.financing.domain.model.UserId;
import com.ksa.financing.service.template.domain.model.ExampleAggregate;
import com.ksa.financing.service.template.domain.model.ExampleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit test template for domain aggregates.
 * Tests business logic without any infrastructure dependencies.
 */
@DisplayName("ExampleAggregate Unit Tests")
class ExampleAggregateTest {

    private TenantId tenantId;
    private UserId userId;

    @BeforeEach
    void setUp() {
        tenantId = new TenantId("tenant-123");
        userId = new UserId("user-456");
    }

    @Test
    @DisplayName("Should create aggregate with valid data")
    void shouldCreateAggregateWithValidData() {
        // Given
        String name = "Test Aggregate";
        String description = "Test Description";

        // When
        var aggregate = ExampleAggregate.create(tenantId, name, description, userId);

        // Then
        assertThat(aggregate).isNotNull();
        assertThat(aggregate.getId()).isNotNull();
        assertThat(aggregate.getName()).isEqualTo(name);
        assertThat(aggregate.getDescription()).isEqualTo(description);
        assertThat(aggregate.getStatus()).isEqualTo(ExampleStatus.DRAFT);
        assertThat(aggregate.getCreatedBy()).isEqualTo(userId);
        assertThat(aggregate.getUncommittedEvents()).hasSize(1);
    }

    @Test
    @DisplayName("Should throw exception when creating with null tenant ID")
    void shouldThrowExceptionWhenCreatingWithNullTenantId() {
        // Given
        String name = "Test Aggregate";
        String description = "Test Description";

        // When & Then
        assertThatThrownBy(() -> ExampleAggregate.create(null, name, description, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TenantId cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when creating with empty name")
    void shouldThrowExceptionWhenCreatingWithEmptyName() {
        // Given
        String name = "";
        String description = "Test Description";

        // When & Then
        assertThatThrownBy(() -> ExampleAggregate.create(tenantId, name, description, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name cannot be empty");
    }

    @Test
    @DisplayName("Should activate aggregate when in DRAFT status")
    void shouldActivateAggregateWhenInDraftStatus() {
        // Given
        var aggregate = ExampleAggregate.create(tenantId, "Test", "Description", userId);
        var activatedBy = new UserId("activator-789");

        // When
        aggregate.activate(activatedBy);

        // Then
        assertThat(aggregate.getStatus()).isEqualTo(ExampleStatus.ACTIVE);
        assertThat(aggregate.getLastModifiedBy()).isEqualTo(activatedBy);
        assertThat(aggregate.getUncommittedEvents()).hasSize(2); // Create + Activate events
    }

    @Test
    @DisplayName("Should throw exception when activating non-DRAFT aggregate")
    void shouldThrowExceptionWhenActivatingNonDraftAggregate() {
        // Given
        var aggregate = ExampleAggregate.create(tenantId, "Test", "Description", userId);
        aggregate.activate(userId);
        aggregate.markEventsAsCommitted();

        // When & Then
        assertThatThrownBy(() -> aggregate.activate(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only activate from DRAFT status");
    }

    @Test
    @DisplayName("Should add entity to active aggregate")
    void shouldAddEntityToActiveAggregate() {
        // Given
        var aggregate = ExampleAggregate.create(tenantId, "Test", "Description", userId);
        aggregate.activate(userId);
        String entityName = "Test Entity";
        String entityValue = "Test Value";

        // When
        aggregate.addEntity(entityName, entityValue, userId);

        // Then
        assertThat(aggregate.getEntities()).hasSize(1);
        assertThat(aggregate.getEntities().get(0).getName()).isEqualTo(entityName);
        assertThat(aggregate.getEntities().get(0).getValue()).isEqualTo(entityValue);
    }

    @Test
    @DisplayName("Should throw exception when adding entity to inactive aggregate")
    void shouldThrowExceptionWhenAddingEntityToInactiveAggregate() {
        // Given
        var aggregate = ExampleAggregate.create(tenantId, "Test", "Description", userId);

        // When & Then
        assertThatThrownBy(() -> aggregate.addEntity("Name", "Value", userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Can only add entities when ACTIVE");
    }

    @Test
    @DisplayName("Should enforce maximum entity limit")
    void shouldEnforceMaximumEntityLimit() {
        // Given
        var aggregate = ExampleAggregate.create(tenantId, "Test", "Description", userId);
        aggregate.activate(userId);

        // Add 10 entities (max limit)
        for (int i = 0; i < 10; i++) {
            aggregate.addEntity("Entity " + i, "Value " + i, userId);
        }

        // When & Then - Adding 11th entity should fail
        assertThatThrownBy(() -> aggregate.addEntity("Entity 11", "Value 11", userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot have more than 10 entities");
    }

    @Test
    @DisplayName("Should complete aggregate when valid")
    void shouldCompleteAggregateWhenValid() {
        // Given
        var aggregate = ExampleAggregate.create(tenantId, "Test", "Description", userId);
        aggregate.activate(userId);
        aggregate.addEntity("Entity", "Value", userId);
        var completedBy = new UserId("completer-999");

        // When
        aggregate.complete(completedBy);

        // Then
        assertThat(aggregate.getStatus()).isEqualTo(ExampleStatus.COMPLETED);
        assertThat(aggregate.getLastModifiedBy()).isEqualTo(completedBy);
    }

    @Test
    @DisplayName("Should throw exception when completing aggregate without entities")
    void shouldThrowExceptionWhenCompletingAggregateWithoutEntities() {
        // Given
        var aggregate = ExampleAggregate.create(tenantId, "Test", "Description", userId);
        aggregate.activate(userId);

        // When & Then
        assertThatThrownBy(() -> aggregate.complete(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot complete without any entities");
    }
}