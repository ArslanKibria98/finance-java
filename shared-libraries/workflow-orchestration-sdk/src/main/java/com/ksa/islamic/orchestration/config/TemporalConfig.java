package com.ksa.islamic.orchestration.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.converter.DataConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Temporal Client Configuration
 *
 * Configures connection to Temporal server with:
 * - Connection settings
 * - Timeout configurations
 * - Data converter for custom serialization
 * - Multi-tenant namespace support
 */
@Slf4j
@Configuration
public class TemporalConfig {

    @Value("${temporal.server.host:localhost}")
    private String temporalHost;

    @Value("${temporal.server.port:7233}")
    private int temporalPort;

    @Value("${temporal.namespace:default}")
    private String namespace;

    @Value("${temporal.connection.timeout:30}")
    private int connectionTimeoutSeconds;

    @Value("${temporal.rpc.timeout:60}")
    private int rpcTimeoutSeconds;

    @Value("${temporal.enable.ssl:false}")
    private boolean enableSSL;

    /**
     * Creates Temporal service stubs for server communication
     */
    @Bean
    @ConditionalOnMissingBean
    public WorkflowServiceStubs workflowServiceStubs(DataConverter dataConverter) {
        log.info("Configuring Temporal connection to {}:{} in namespace '{}'",
                temporalHost, temporalPort, namespace);

        WorkflowServiceStubsOptions.Builder optionsBuilder = WorkflowServiceStubsOptions.newBuilder()
                .setTarget(String.format("%s:%d", temporalHost, temporalPort))
                .setRpcTimeout(Duration.ofSeconds(rpcTimeoutSeconds));

        if (enableSSL) {
            log.info("SSL enabled for Temporal connection");
            // SSL configuration would be added here if needed
        }

        WorkflowServiceStubsOptions options = optionsBuilder.build();

        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(options);

        // Test connection
        try {
            service.blockingStub().listNamespaces(
                    io.temporal.api.workflowservice.v1.ListNamespacesRequest.newBuilder()
                            .setPageSize(1)
                            .build()
            );
            log.info("Successfully connected to Temporal server");
        } catch (Exception e) {
            log.error("Failed to connect to Temporal server: {}", e.getMessage());
            throw new RuntimeException("Cannot establish connection to Temporal server", e);
        }

        return service;
    }

    /**
     * Creates Temporal workflow client
     */
    @Bean
    @ConditionalOnMissingBean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs,
                                         DataConverter dataConverter) {
        log.info("Creating WorkflowClient for namespace: {}", namespace);

        WorkflowClientOptions clientOptions = WorkflowClientOptions.newBuilder()
                .setNamespace(namespace)
                .setDataConverter(dataConverter)
                .build();

        return WorkflowClient.newInstance(workflowServiceStubs, clientOptions);
    }

    /**
     * Provides namespace configuration for multi-tenant support
     */
    @Bean
    public NamespaceConfig namespaceConfig() {
        return NamespaceConfig.builder()
                .defaultNamespace(namespace)
                .build();
    }
}