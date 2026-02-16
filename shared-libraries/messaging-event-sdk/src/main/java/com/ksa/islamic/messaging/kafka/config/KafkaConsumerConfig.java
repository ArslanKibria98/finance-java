package com.ksa.islamic.messaging.kafka.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer Configuration with manual acknowledgment and Avro deserialization
 * Supports both Avro and JSON message consumption with error handling
 */
@Slf4j
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;
    private final SchemaRegistryConfig schemaRegistryConfig;
    private final DeadLetterQueueConfig dlqConfig;

    /**
     * Consumer factory for Avro messages
     */
    @Bean
    public ConsumerFactory<String, Object> avroConsumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Basic Kafka connection
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());

        // Consumer group configuration
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                  kafkaProperties.getConsumer().getGroupId() != null ?
                  kafkaProperties.getConsumer().getGroupId() : "islamic-financing-consumer");

        // Manual offset management for better control
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Performance tuning
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        // Deserializers
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);

        // Schema Registry configuration
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
                  schemaRegistryConfig.getUrl());
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put(KafkaAvroDeserializerConfig.USE_LATEST_VERSION, true);

        // Security settings if configured
        if (kafkaProperties.getSecurity() != null && kafkaProperties.getSecurity().getProtocol() != null) {
            props.put("security.protocol", kafkaProperties.getSecurity().getProtocol());
            if (kafkaProperties.getSsl() != null) {
                props.putAll(kafkaProperties.getSsl().buildProperties());
            }
        }

        // Client ID for tracking
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "islamic-financing-consumer-" +
                  System.getProperty("POD_NAME", "local"));

        // Isolation level for transactional reads
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        // Error handling deserializer wrapper
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, KafkaAvroDeserializer.class);

        log.info("Initializing Kafka Avro consumer with schema registry URL: {}",
                 schemaRegistryConfig.getUrl());

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Consumer factory for JSON messages
     */
    @Bean
    public ConsumerFactory<String, Object> jsonConsumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                  kafkaProperties.getConsumer().getGroupId() != null ?
                  kafkaProperties.getConsumer().getGroupId() : "islamic-financing-consumer");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // JSON deserializer configuration
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ksa.islamic.*");
        props.put(JsonDeserializer.TYPE_MAPPINGS,
                  "event:com.ksa.islamic.messaging.contract.EventEnvelope");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                  "com.ksa.islamic.messaging.contract.EventEnvelope");

        // Error handling
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Listener container factory for Avro messages with retry and DLQ
     */
    @Bean(name = "avroKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> avroKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(avroConsumerFactory());

        // Manual acknowledgment mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Concurrency settings
        factory.setConcurrency(3); // 3 concurrent consumers per container

        // Error handling with retry and DLQ
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            dlqConfig.getDeadLetterPublishingRecoverer(),
            new FixedBackOff(dlqConfig.getRetryIntervalMs(), dlqConfig.getMaxRetries())
        );

        // Configure which exceptions should not be retried
        errorHandler.addNotRetryableExceptions(
            IllegalArgumentException.class,
            NullPointerException.class
        );

        factory.setCommonErrorHandler(errorHandler);

        // Enable batch listener if needed
        factory.setBatchListener(false);

        // Set up observation for metrics
        factory.getContainerProperties().setObservationEnabled(true);

        // Idle between polls
        factory.getContainerProperties().setIdleBetweenPolls(1000L);

        // Consumer rebalance listener for logging
        factory.getContainerProperties().setConsumerRebalanceListener(
            new LoggingConsumerRebalanceListener()
        );

        return factory;
    }

    /**
     * Listener container factory for JSON messages
     */
    @Bean(name = "jsonKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> jsonKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(jsonConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(3);

        // Error handling
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            dlqConfig.getDeadLetterPublishingRecoverer(),
            new FixedBackOff(dlqConfig.getRetryIntervalMs(), dlqConfig.getMaxRetries())
        );

        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setObservationEnabled(true);

        return factory;
    }

    /**
     * Batch listener container factory for high-throughput scenarios
     */
    @Bean(name = "batchKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> batchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(avroConsumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.setConcurrency(2);

        // Batch error handling
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            dlqConfig.getDeadLetterPublishingRecoverer(),
            new FixedBackOff(5000L, 3)
        ));

        return factory;
    }
}