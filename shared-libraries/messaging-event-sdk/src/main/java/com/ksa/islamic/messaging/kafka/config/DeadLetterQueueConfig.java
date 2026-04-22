package com.ksa.islamic.messaging.kafka.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.KafkaHeaders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.function.BiFunction;

/**
 * Configuration for Dead Letter Queue (DLQ) handling
 * Routes failed messages after max retries to DLQ topics
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "kafka.dlq")
@RequiredArgsConstructor
public class DeadLetterQueueConfig {

    /**
     * Prefix for DLQ topics
     */
    private String topicPrefix = "dlq.";

    /**
     * Max retry attempts before sending to DLQ
     */
    private int maxRetries = 3;

    /**
     * Retry interval in milliseconds
     */
    private long retryIntervalMs = 5000L;

    /**
     * Whether to include original headers in DLQ message
     */
    private boolean includeOriginalHeaders = true;

    /**
     * Whether to add error details to headers
     */
    private boolean addErrorHeaders = true;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Dead Letter Publishing Recoverer for failed messages
     */
    private DeadLetterPublishingRecoverer dlpRecoverer;

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer() {
        // Use simplified constructor for Spring Kafka 3.3.0 compatibility
        if (dlpRecoverer == null) {
            dlpRecoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        }
        return dlpRecoverer;
    }

    public DeadLetterPublishingRecoverer getDeadLetterPublishingRecoverer() {
        if (dlpRecoverer == null) {
            dlpRecoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        }
        return dlpRecoverer;
    }

    /**
     * Destination resolver for DLQ topic naming
     */
    private BiFunction<ConsumerRecord<?, ?>, Exception, ProducerRecord<String, Object>> destinationResolver() {
        return (consumerRecord, exception) -> {
            String originalTopic = consumerRecord.topic();
            String dlqTopic = topicPrefix + originalTopic;

            log.error("Sending failed message to DLQ. Original topic: {}, DLQ topic: {}, " +
                     "Partition: {}, Offset: {}, Error: {}",
                     originalTopic, dlqTopic,
                     consumerRecord.partition(), consumerRecord.offset(),
                     exception.getMessage());

            ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(
                dlqTopic,
                null, // Let Kafka decide partition
                consumerRecord.timestamp(),
                extractKey(consumerRecord),
                consumerRecord.value()
            );

            // Add headers with error information
            Headers headers = producerRecord.headers();

            if (includeOriginalHeaders && consumerRecord.headers() != null) {
                consumerRecord.headers().forEach(header ->
                    headers.add(header.key(), header.value())
                );
            }

            if (addErrorHeaders) {
                // Add error details
                headers.add("dlq.error.message",
                           exception.getMessage() != null ?
                           exception.getMessage().getBytes(StandardCharsets.UTF_8) :
                           "Unknown error".getBytes(StandardCharsets.UTF_8));

                headers.add("dlq.error.class",
                           exception.getClass().getName().getBytes(StandardCharsets.UTF_8));

                headers.add("dlq.error.stacktrace",
                           getStackTraceAsString(exception).getBytes(StandardCharsets.UTF_8));

                // Add original topic info
                headers.add("dlq.original.topic", originalTopic.getBytes(StandardCharsets.UTF_8));
                headers.add("dlq.original.partition",
                           String.valueOf(consumerRecord.partition()).getBytes(StandardCharsets.UTF_8));
                headers.add("dlq.original.offset",
                           String.valueOf(consumerRecord.offset()).getBytes(StandardCharsets.UTF_8));

                // Add timestamp
                headers.add("dlq.timestamp",
                           Instant.now().toString().getBytes(StandardCharsets.UTF_8));

                // Add retry count if present
                byte[] retryCount = consumerRecord.headers().lastHeader("retry.count") != null ?
                    consumerRecord.headers().lastHeader("retry.count").value() : "0".getBytes();
                headers.add("dlq.retry.count", retryCount);

                // Add tenant ID if present
                if (consumerRecord.headers().lastHeader("tenant.id") != null) {
                    headers.add("dlq.tenant.id",
                               consumerRecord.headers().lastHeader("tenant.id").value());
                }

                // Add correlation ID if present
                if (consumerRecord.headers().lastHeader(KafkaHeaders.CORRELATION_ID) != null) {
                    headers.add("dlq.correlation.id",
                               consumerRecord.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
                }
            }

            return producerRecord;
        };
    }

    /**
     * Extract key from consumer record
     */
    private String extractKey(ConsumerRecord<?, ?> record) {
        if (record.key() == null) {
            return null;
        }
        return record.key().toString();
    }

    /**
     * Convert exception stack trace to string
     */
    private String getStackTraceAsString(Exception exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.toString()).append("\n");

        StackTraceElement[] stackTrace = exception.getStackTrace();
        int maxLines = Math.min(stackTrace.length, 20); // Limit stack trace lines

        for (int i = 0; i < maxLines; i++) {
            sb.append("\tat ").append(stackTrace[i]).append("\n");
        }

        if (stackTrace.length > maxLines) {
            sb.append("\t... ").append(stackTrace.length - maxLines).append(" more\n");
        }

        // Include cause if present
        Throwable cause = exception.getCause();
        if (cause != null) {
            sb.append("Caused by: ").append(cause.toString()).append("\n");
            StackTraceElement[] causeTrace = cause.getStackTrace();
            int causeLines = Math.min(causeTrace.length, 10);
            for (int i = 0; i < causeLines; i++) {
                sb.append("\tat ").append(causeTrace[i]).append("\n");
            }
            if (causeTrace.length > causeLines) {
                sb.append("\t... ").append(causeTrace.length - causeLines).append(" more\n");
            }
        }

        return sb.toString();
    }
}