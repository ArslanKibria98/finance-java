package com.ksa.islamic.reporting.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for read model database with separate connection pool.
 * Optimized for read-heavy workloads with appropriate connection settings.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.ksa.islamic.reporting.repository",
        entityManagerFactoryRef = "readModelEntityManagerFactory",
        transactionManagerRef = "readModelTransactionManager"
)
public class ReadModelDataSourceConfig {

    /**
     * Read model datasource properties.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.read-model")
    public DataSourceProperties readModelDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Read model datasource with optimized connection pool settings.
     */
    @Bean(name = "readModelDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.read-model.hikari")
    public DataSource readModelDataSource() {
        DataSourceProperties properties = readModelDataSourceProperties();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(properties.getDriverClassName());

        // Optimize for read-heavy workload
        config.setPoolName("ReadModelPool");
        config.setMaximumPoolSize(30); // More connections for parallel reads
        config.setMinimumIdle(10);
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes

        // Read-optimized settings
        config.setReadOnly(false); // Still need write for projections
        config.setAutoCommit(false);
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");

        // Connection pool metrics
        config.setRegisterMbeans(true);
        config.setMetricRegistry(null); // Will be injected if Micrometer is available

        // PostgreSQL specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");

        // Read preference for replicas (if using read replicas)
        config.addDataSourceProperty("readPreference", "secondary");
        config.addDataSourceProperty("targetServerType", "preferSlave");

        return new HikariDataSource(config);
    }

    /**
     * Entity manager factory for read models.
     */
    @Bean(name = "readModelEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean readModelEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("readModelDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();

        // Hibernate properties optimized for read operations
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "validate"); // Never modify schema
        properties.put("hibernate.show_sql", false);
        properties.put("hibernate.format_sql", false);

        // Second-level cache configuration
        properties.put("hibernate.cache.use_second_level_cache", true);
        properties.put("hibernate.cache.use_query_cache", true);
        properties.put("hibernate.cache.region.factory_class",
                "org.hibernate.cache.jcache.JCacheRegionFactory");
        properties.put("hibernate.javax.cache.provider",
                "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider");

        // Batch fetching for associations
        properties.put("hibernate.default_batch_fetch_size", 16);
        properties.put("hibernate.jdbc.batch_size", 25);
        properties.put("hibernate.order_inserts", true);
        properties.put("hibernate.order_updates", true);

        // Statistics for monitoring
        properties.put("hibernate.generate_statistics", true);
        properties.put("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", 100);

        // Multi-tenancy support
        properties.put("hibernate.multiTenancy", "SCHEMA");
        properties.put("hibernate.multi_tenant_connection_provider",
                "com.ksa.islamic.reporting.config.MultiTenantConnectionProvider");
        properties.put("hibernate.tenant_identifier_resolver",
                "com.ksa.islamic.reporting.config.TenantIdentifierResolver");

        return builder
                .dataSource(dataSource)
                .packages(
                        "com.ksa.islamic.reporting.readmodel",
                        "com.ksa.islamic.reporting.projector"
                )
                .persistenceUnit("readModel")
                .properties(properties)
                .build();
    }

    /**
     * Transaction manager for read models.
     */
    @Bean(name = "readModelTransactionManager")
    public PlatformTransactionManager readModelTransactionManager(
            @Qualifier("readModelEntityManagerFactory") EntityManagerFactory entityManagerFactory) {

        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);

        // Set default timeout for read transactions
        transactionManager.setDefaultTimeout(30); // 30 seconds

        return transactionManager;
    }
}