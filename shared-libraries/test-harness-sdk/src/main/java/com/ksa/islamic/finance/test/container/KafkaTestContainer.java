package com.ksa.islamic.finance.test.container;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka test container for event streaming testing
 */
@Slf4j
public class KafkaTestContainer {
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:7.5.0";
    private static final Map<String, KafkaContainer> containers = new ConcurrentHashMap<>();

    private KafkaTestContainer() {}

    /**
     * Get or create a Kafka container with default settings
     */
    public static KafkaContainer getInstance() {
        return getInstance("test");
    }

    /**
     * Get or create a named Kafka container
     */
    public static KafkaContainer getInstance(String name) {
        return containers.computeIfAbsent(name, k -> createContainer());
    }

    /**
     * Create a new Kafka container with Islamic finance topics
     */
    public static KafkaContainer createContainer() {
        KafkaContainer container = new KafkaContainer(
            DockerImageName.parse(KAFKA_IMAGE)
        )
            .withKraft()
            .withStartupTimeout(Duration.ofMinutes(3))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withEnv("KAFKA_DELETE_TOPIC_ENABLE", "true")
            .withEnv("KAFKA_LOG_RETENTION_HOURS", "1")
            .withEnv("KAFKA_LOG_SEGMENT_BYTES", "1073741824")
            .withReuse(true);

        container.start();
        createTopics(container);

        log.info("Kafka container started: {}", container.getBootstrapServers());
        return container;
    }

    /**
     * Create Islamic finance specific topics
     */
    private static void createTopics(KafkaContainer container) {
        Properties props = new Properties();
        props.put("bootstrap.servers", container.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(props)) {
            List<NewTopic> topics = Arrays.asList(
                // Domain events
                createTopic("islamic-finance.loan.events", 3, 1),
                createTopic("islamic-finance.customer.events", 3, 1),
                createTopic("islamic-finance.payment.events", 3, 1),
                createTopic("islamic-finance.account.events", 3, 1),

                // Integration events
                createTopic("islamic-finance.integration.commands", 3, 1),
                createTopic("islamic-finance.integration.responses", 3, 1),

                // Audit events
                createTopic("islamic-finance.audit.events", 1, 1),

                // Dead letter queues
                createTopic("islamic-finance.dlq.loan", 1, 1),
                createTopic("islamic-finance.dlq.payment", 1, 1),

                // Test topics
                createTopic("test.events", 1, 1),
                createTopic("test.commands", 1, 1)
            );

            adminClient.createTopics(topics).all().get();
            log.debug("Kafka topics created successfully");
        } catch (Exception e) {
            log.error("Failed to create topics", e);
        }
    }

    private static NewTopic createTopic(String name, int partitions, int replication) {
        return new NewTopic(name, partitions, (short) replication)
            .configs(Map.of(
                "retention.ms", "3600000",
                "segment.ms", "600000",
                "min.insync.replicas", "1"
            ));
    }

    /**
     * Create a Kafka producer for testing
     */
    public static KafkaProducer<String, String> createProducer(KafkaContainer container) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new KafkaProducer<>(props);
    }

    /**
     * Create a Kafka consumer for testing
     */
    public static KafkaConsumer<String, String> createConsumer(
        KafkaContainer container,
        String groupId,
        String... topics
    ) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topics));
        return consumer;
    }

    /**
     * Get producer configuration
     */
    public static Properties getProducerConfig(KafkaContainer container) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());
        return props;
    }

    /**
     * Get consumer configuration
     */
    public static Properties getConsumerConfig(KafkaContainer container, String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        return props;
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