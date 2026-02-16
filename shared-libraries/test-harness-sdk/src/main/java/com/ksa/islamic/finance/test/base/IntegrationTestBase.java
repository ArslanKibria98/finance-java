package com.ksa.islamic.finance.test.base;

import com.ksa.islamic.finance.test.container.KafkaTestContainer;
import com.ksa.islamic.finance.test.container.PostgreSQLTestContainer;
import com.ksa.islamic.finance.test.container.RedisTestContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Base class for integration tests with full container support
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    protected static PostgreSQLContainer<?> postgresContainer;
    protected static GenericContainer<?> redisContainer;
    protected static KafkaContainer kafkaContainer;

    @BeforeAll
    static void initContainers() {
        log.info("Starting test containers...");

        // Start PostgreSQL
        postgresContainer = PostgreSQLTestContainer.getInstance();

        // Start Redis
        redisContainer = RedisTestContainer.getInstance();

        // Start Kafka
        kafkaContainer = KafkaTestContainer.getInstance();

        log.info("All test containers started successfully");
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL properties
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);

        // Redis properties
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));

        // Kafka properties
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafkaContainer::getBootstrapServers);

        // Test-specific properties
        registry.add("test.containers.enabled", () -> true);
        registry.add("test.environment", () -> "integration");
    }

    @BeforeEach
    void setUp() {
        log.debug("Setting up test: {}", getClass().getSimpleName());
        cleanDatabase();
        cleanRedis();
        additionalSetup();
    }

    @AfterEach
    void tearDown() {
        log.debug("Tearing down test: {}", getClass().getSimpleName());
        additionalTearDown();
    }

    /**
     * Clean database before each test
     */
    protected void cleanDatabase() {
        try (Connection conn = postgresContainer.createConnection("");
             Statement stmt = conn.createStatement()) {

            // Clean test data
            stmt.execute("TRUNCATE TABLE islamic_finance.loans CASCADE");
            stmt.execute("TRUNCATE TABLE islamic_finance.customers CASCADE");
            stmt.execute("TRUNCATE TABLE audit.audit_logs CASCADE");

            log.debug("Database cleaned");
        } catch (Exception e) {
            log.error("Failed to clean database", e);
        }
    }

    /**
     * Clean Redis before each test
     */
    protected void cleanRedis() {
        RedisTestContainer.flushAll(redisContainer);
    }

    /**
     * Hook for additional setup in subclasses
     */
    protected void additionalSetup() {
        // Override in subclasses if needed
    }

    /**
     * Hook for additional teardown in subclasses
     */
    protected void additionalTearDown() {
        // Override in subclasses if needed
    }

    /**
     * Get DataSource for direct database operations
     */
    protected DataSource getDataSource() {
        return PostgreSQLTestContainer.createDataSource(postgresContainer);
    }

    /**
     * Get Kafka bootstrap servers
     */
    protected String getKafkaBootstrapServers() {
        return kafkaContainer.getBootstrapServers();
    }

    /**
     * Get Redis connection string
     */
    protected String getRedisConnectionString() {
        return RedisTestContainer.getConnectionString(redisContainer);
    }

    /**
     * Wait for condition with timeout
     */
    protected void waitFor(int timeoutSeconds, int intervalMillis, BooleanSupplier condition) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting", e);
            }
        }

        throw new AssertionError("Timeout waiting for condition");
    }

    @FunctionalInterface
    protected interface BooleanSupplier {
        boolean getAsBoolean();
    }
}