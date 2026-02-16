package com.ksa.islamic.messaging.kafka.config;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Confluent Schema Registry integration configuration
 * Manages Avro schema validation and evolution
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "kafka.schema-registry")
public class SchemaRegistryConfig {

    /**
     * Schema Registry URL
     */
    private String url = "http://localhost:8081";

    /**
     * Maximum number of schemas to cache
     */
    private int cacheSize = 1000;

    /**
     * Whether to auto-register schemas
     */
    private boolean autoRegisterSchemas = true;

    /**
     * Compatibility level for schema evolution
     * Options: BACKWARD, FORWARD, FULL, NONE
     */
    private String compatibilityLevel = "BACKWARD";

    /**
     * Schema name strategy
     */
    private String subjectNameStrategy = "TopicRecordNameStrategy";

    /**
     * Authentication settings
     */
    private Authentication authentication = new Authentication();

    /**
     * SSL configuration
     */
    private Ssl ssl = new Ssl();

    @Data
    public static class Authentication {
        private boolean enabled = false;
        private String username;
        private String password;
        private String bearerToken;
    }

    @Data
    public static class Ssl {
        private boolean enabled = false;
        private String truststoreLocation;
        private String truststorePassword;
        private String keystoreLocation;
        private String keystorePassword;
        private String keyPassword;
    }

    /**
     * Schema Registry Client bean
     */
    @Bean
    public SchemaRegistryClient schemaRegistryClient() {
        log.info("Initializing Schema Registry client with URL: {}", url);

        Map<String, Object> configs = new HashMap<>();

        // Basic configuration
        configs.put("schema.registry.url", url);

        // Authentication configuration
        if (authentication.isEnabled()) {
            if (authentication.getBearerToken() != null) {
                configs.put("bearer.auth.token", authentication.getBearerToken());
            } else if (authentication.getUsername() != null) {
                configs.put("basic.auth.credentials.source", "USER_INFO");
                configs.put("basic.auth.user.info",
                           authentication.getUsername() + ":" + authentication.getPassword());
            }
        }

        // SSL configuration
        if (ssl.isEnabled()) {
            configs.put("schema.registry.ssl.truststore.location", ssl.getTruststoreLocation());
            configs.put("schema.registry.ssl.truststore.password", ssl.getTruststorePassword());

            if (ssl.getKeystoreLocation() != null) {
                configs.put("schema.registry.ssl.keystore.location", ssl.getKeystoreLocation());
                configs.put("schema.registry.ssl.keystore.password", ssl.getKeystorePassword());
                configs.put("schema.registry.ssl.key.password", ssl.getKeyPassword());
            }
        }

        RestService restService = new RestService(List.of(url));

        return new CachedSchemaRegistryClient(
            restService,
            cacheSize,
            configs
        );
    }

    /**
     * Get configuration properties for Kafka serializers/deserializers
     */
    public Map<String, Object> getSerializerConfigs() {
        Map<String, Object> configs = new HashMap<>();

        configs.put("schema.registry.url", url);
        configs.put("auto.register.schemas", autoRegisterSchemas);
        configs.put("use.latest.version", true);
        configs.put("specific.avro.reader", true);
        configs.put("value.subject.name.strategy",
                   "io.confluent.kafka.serializers.subject." + subjectNameStrategy);

        // Add authentication configs
        if (authentication.isEnabled()) {
            if (authentication.getBearerToken() != null) {
                configs.put("bearer.auth.token", authentication.getBearerToken());
            } else if (authentication.getUsername() != null) {
                configs.put("basic.auth.credentials.source", "USER_INFO");
                configs.put("basic.auth.user.info",
                           authentication.getUsername() + ":" + authentication.getPassword());
            }
        }

        // Add SSL configs
        if (ssl.isEnabled()) {
            configs.put("schema.registry.ssl.truststore.location", ssl.getTruststoreLocation());
            configs.put("schema.registry.ssl.truststore.password", ssl.getTruststorePassword());

            if (ssl.getKeystoreLocation() != null) {
                configs.put("schema.registry.ssl.keystore.location", ssl.getKeystoreLocation());
                configs.put("schema.registry.ssl.keystore.password", ssl.getKeystorePassword());
                configs.put("schema.registry.ssl.key.password", ssl.getKeyPassword());
            }
        }

        return configs;
    }
}