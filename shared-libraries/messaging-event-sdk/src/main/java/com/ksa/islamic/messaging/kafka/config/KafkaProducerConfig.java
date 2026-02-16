package com.ksa.islamic.messaging.kafka.config;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer Configuration with idempotent producer and Avro serialization
 * Ensures exactly-once semantics and schema validation
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;
    private final SchemaRegistryConfig schemaRegistryConfig;

    /**
     * Producer factory for Avro messages
     */
    @Bean
    public ProducerFactory<String, Object> avroProducerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Basic Kafka connection
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());

        // Idempotent producer settings for exactly-once semantics
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Performance settings
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        // Serializers
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

        // Schema Registry configuration
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
                  schemaRegistryConfig.getUrl());
        props.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS,
                  schemaRegistryConfig.isAutoRegisterSchemas());
        props.put(KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY,
                  "io.confluent.kafka.serializers.subject.TopicRecordNameStrategy");

        // Security settings if configured
        if (kafkaProperties.getSecurity() != null && kafkaProperties.getSecurity().getProtocol() != null) {
            props.put("security.protocol", kafkaProperties.getSecurity().getProtocol());
            if (kafkaProperties.getSsl() != null) {
                props.putAll(kafkaProperties.getSsl().buildProperties());
            }
            if (kafkaProperties.getProperties() != null) {
                props.putAll(kafkaProperties.getProperties());
            }
        }

        // Add client ID for tracking
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "islamic-financing-producer-" +
                  System.getProperty("POD_NAME", "local"));

        // Request timeout settings
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        log.info("Initializing Kafka Avro producer with schema registry URL: {}",
                 schemaRegistryConfig.getUrl());

        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Producer factory for JSON messages (for non-Avro events)
     */
    @Bean
    public ProducerFactory<String, Object> jsonProducerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // JSON serializer configuration
        props.put(JsonSerializer.TYPE_MAPPINGS,
                  "event:com.ksa.islamic.messaging.contract.EventEnvelope");
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        // Add error handling serializer wrapper
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ErrorHandlingSerializer.class);
        props.put(ErrorHandlingSerializer.VALUE_SERIALIZER_CLASS, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * KafkaTemplate for Avro messages
     */
    @Bean(name = "avroKafkaTemplate")
    public KafkaTemplate<String, Object> avroKafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(avroProducerFactory());

        // Set default topic if configured
        if (kafkaProperties.getTemplate() != null &&
            kafkaProperties.getTemplate().getDefaultTopic() != null) {
            template.setDefaultTopic(kafkaProperties.getTemplate().getDefaultTopic());
        }

        // Enable observation for metrics
        template.setObservationEnabled(true);

        return template;
    }

    /**
     * KafkaTemplate for JSON messages
     */
    @Bean(name = "jsonKafkaTemplate")
    public KafkaTemplate<String, Object> jsonKafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(jsonProducerFactory());
        template.setObservationEnabled(true);
        return template;
    }

    /**
     * Transactional KafkaTemplate for exactly-once processing
     */
    @Bean(name = "transactionalKafkaTemplate")
    public KafkaTemplate<String, Object> transactionalKafkaTemplate() {
        ProducerFactory<String, Object> factory = avroProducerFactory();

        // Enable transactions
        Map<String, Object> props = new HashMap<>(factory.getConfigurationProperties());
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG,
                  "islamic-financing-tx-" + System.getProperty("POD_NAME", "local"));

        DefaultKafkaProducerFactory<String, Object> transactionalFactory =
            new DefaultKafkaProducerFactory<>(props);
        transactionalFactory.setTransactionIdPrefix("islamic-tx-");

        KafkaTemplate<String, Object> template = new KafkaTemplate<>(transactionalFactory);
        template.setObservationEnabled(true);

        return template;
    }
}