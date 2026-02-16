package com.ksa.islamic.finance.test.container;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PostgreSQL test container with pre-configured settings for Islamic finance testing
 */
@Slf4j
public class PostgreSQLTestContainer {
    private static final String POSTGRES_IMAGE = "postgres:15-alpine";
    private static final Map<String, PostgreSQLContainer<?>> containers = new ConcurrentHashMap<>();

    private PostgreSQLTestContainer() {}

    /**
     * Get or create a PostgreSQL container with default settings
     */
    public static PostgreSQLContainer<?> getInstance() {
        return getInstance("test");
    }

    /**
     * Get or create a named PostgreSQL container
     */
    public static PostgreSQLContainer<?> getInstance(String name) {
        return containers.computeIfAbsent(name, k -> createContainer());
    }

    /**
     * Create a new PostgreSQL container with Islamic finance schema
     */
    public static PostgreSQLContainer<?> createContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
            DockerImageName.parse(POSTGRES_IMAGE)
        )
            .withDatabaseName("islamic_finance_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("db/init.sql")
            .withTmpFs(Map.of("/var/lib/postgresql/data", "rw,noexec,nosuid,size=256m"))
            .withCommand("postgres", "-c", "max_connections=200")
            .withReuse(true);

        container.start();
        initializeSchema(container);

        log.info("PostgreSQL container started: {}", container.getJdbcUrl());
        return container;
    }

    /**
     * Create a DataSource for the container
     */
    public static DataSource createDataSource(PostgreSQLContainer<?> container) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(container.getJdbcUrl());
        config.setUsername(container.getUsername());
        config.setPassword(container.getPassword());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("TestPool");

        return new HikariDataSource(config);
    }

    /**
     * Initialize Islamic finance specific schema
     */
    private static void initializeSchema(PostgreSQLContainer<?> container) {
        try (Connection conn = container.createConnection("");
             Statement stmt = conn.createStatement()) {

            // Create schemas
            stmt.execute("CREATE SCHEMA IF NOT EXISTS islamic_finance");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS audit");

            // Create extensions
            stmt.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
            stmt.execute("CREATE EXTENSION IF NOT EXISTS \"pgcrypto\"");

            // Create test tables
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS islamic_finance.customers (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    customer_number VARCHAR(50) UNIQUE NOT NULL,
                    first_name VARCHAR(100) NOT NULL,
                    last_name VARCHAR(100) NOT NULL,
                    email VARCHAR(255),
                    phone VARCHAR(50),
                    status VARCHAR(50) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS islamic_finance.loans (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    loan_number VARCHAR(50) UNIQUE NOT NULL,
                    customer_id UUID REFERENCES islamic_finance.customers(id),
                    product_type VARCHAR(50) NOT NULL,
                    principal_amount DECIMAL(19,4) NOT NULL,
                    profit_rate DECIMAL(5,4) NOT NULL,
                    term_months INTEGER NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            log.debug("Database schema initialized");
        } catch (Exception e) {
            log.error("Failed to initialize schema", e);
            throw new RuntimeException("Schema initialization failed", e);
        }
    }

    /**
     * Clean up all containers
     */
    public static void cleanup() {
        containers.values().forEach(container -> {
            if (container.isRunning()) {
                container.stop();
            }
        });
        containers.clear();
    }
}