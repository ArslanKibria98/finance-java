package com.ksa.financing.service.template.integration;

import com.ksa.financing.domain.model.TenantId;
import com.ksa.financing.domain.model.UserId;
import com.ksa.financing.service.template.ServiceTemplateApplication;
import com.ksa.financing.service.template.domain.model.ExampleAggregate;
import com.ksa.financing.service.template.domain.model.ExampleAggregateId;
import com.ksa.financing.service.template.domain.port.out.ExampleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test template using Testcontainers.
 * Tests the full stack with real database and Kafka.
 */
@SpringBootTest(classes = ServiceTemplateApplication.class)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Example Integration Tests")
class ExampleIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Disable Keycloak for tests
        registry.add("spring.security.enabled", () -> "false");

        // Enable Flyway
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private ExampleRepository repository;

    @Test
    @Transactional
    @DisplayName("Should save and retrieve aggregate from database")
    void shouldSaveAndRetrieveAggregateFromDatabase() {
        // Given
        var tenantId = new TenantId("test-tenant");
        var userId = new UserId("test-user");
        var aggregate = ExampleAggregate.create(
                tenantId,
                "Integration Test",
                "Test Description",
                userId
        );

        // When
        var saved = repository.save(aggregate);
        var retrieved = repository.findById(tenantId, saved.getId());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(saved.getId());
        assertThat(retrieved.get().getName()).isEqualTo("Integration Test");
        assertThat(retrieved.get().getTenantId()).isEqualTo(tenantId);
    }

    @Test
    @Transactional
    @DisplayName("Should find aggregates by tenant")
    void shouldFindAggregatesByTenant() {
        // Given
        var tenantId = new TenantId("tenant-1");
        var otherTenantId = new TenantId("tenant-2");
        var userId = new UserId("user-1");

        // Create aggregates for different tenants
        repository.save(ExampleAggregate.create(tenantId, "Aggregate 1", "Desc 1", userId));
        repository.save(ExampleAggregate.create(tenantId, "Aggregate 2", "Desc 2", userId));
        repository.save(ExampleAggregate.create(otherTenantId, "Other Aggregate", "Desc", userId));

        // When
        var aggregates = repository.findAllByTenant(tenantId);

        // Then
        assertThat(aggregates).hasSize(2);
        assertThat(aggregates).allMatch(a -> a.getTenantId().equals(tenantId));
    }

    @Test
    @Transactional
    @DisplayName("Should update aggregate with entities")
    void shouldUpdateAggregateWithEntities() {
        // Given
        var tenantId = new TenantId("test-tenant");
        var userId = new UserId("test-user");
        var aggregate = ExampleAggregate.create(
                tenantId,
                "Test Aggregate",
                "Description",
                userId
        );

        // Save initial aggregate
        aggregate = repository.save(aggregate);
        var aggregateId = aggregate.getId();

        // Activate and add entity
        aggregate.activate(userId);
        aggregate.addEntity("Entity 1", "Value 1", userId);

        // When
        repository.save(aggregate);
        var updated = repository.findById(tenantId, aggregateId);

        // Then
        assertThat(updated).isPresent();
        assertThat(updated.get().getEntities()).hasSize(1);
        assertThat(updated.get().getEntities().get(0).getName()).isEqualTo("Entity 1");
    }

    @Test
    @Transactional
    @DisplayName("Should check aggregate existence")
    void shouldCheckAggregateExistence() {
        // Given
        var tenantId = new TenantId("test-tenant");
        var userId = new UserId("test-user");
        var aggregate = ExampleAggregate.create(
                tenantId,
                "Test",
                "Description",
                userId
        );
        aggregate = repository.save(aggregate);

        // When
        var exists = repository.exists(tenantId, aggregate.getId());
        var notExists = repository.exists(tenantId, ExampleAggregateId.generate());

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}