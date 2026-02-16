package com.ksa.islamic.messaging.kafka.consumer;

import com.ksa.islamic.messaging.contract.EventEnvelope;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

/**
 * Dead Letter Queue Handler for processing failed messages after max retries
 * Handles messages that could not be processed successfully
 */
@Slf4j
@Component
public class DeadLetterQueueHandler {

    private final Counter dlqMessageCounter;
    private final Counter dlqProcessedCounter;
    private final Counter dlqRequeueCounter;

    public DeadLetterQueueHandler(MeterRegistry meterRegistry) {
        this.dlqMessageCounter = Counter.builder("dlq.messages.received")
            .description("Number of messages received in DLQ")
            .register(meterRegistry);

        this.dlqProcessedCounter = Counter.builder("dlq.messages.processed")
            .description("Number of DLQ messages successfully processed")
            .register(meterRegistry);

        this.dlqRequeueCounter = Counter.builder("dlq.messages.requeued")
            .description("Number of messages requeued from DLQ")
            .register(meterRegistry);
    }

    /**
     * Process messages from the DLQ
     */
    @KafkaListener(
        topics = "#{'${kafka.dlq.topics:dlq.*}'.split(',')}",
        groupId = "dlq-processor-group",
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void processDlqMessage(
            @Payload EventEnvelope envelope,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = "dlq.error.message", required = false) byte[] errorMessageBytes,
            @Header(value = "dlq.error.class", required = false) byte[] errorClassBytes,
            @Header(value = "dlq.original.topic", required = false) byte[] originalTopicBytes,
            @Header(value = "dlq.retry.count", required = false) byte[] retryCountBytes,
            @Header(value = "dlq.tenant.id", required = false) byte[] tenantIdBytes,
            Acknowledgment acknowledgment,
            ConsumerRecord<String, EventEnvelope> record) {

        dlqMessageCounter.increment();

        try {
            // Extract headers
            String errorMessage = extractHeader(errorMessageBytes);
            String errorClass = extractHeader(errorClassBytes);
            String originalTopic = extractHeader(originalTopicBytes);
            String retryCount = extractHeader(retryCountBytes);
            String tenantId = extractHeader(tenantIdBytes);

            log.warn("Processing DLQ message: Topic={}, OriginalTopic={}, Partition={}, Offset={}, " +
                    "EventId={}, TenantId={}, RetryCount={}, ErrorClass={}, ErrorMessage={}",
                    topic, originalTopic, partition, offset,
                    envelope.getEventId(), tenantId, retryCount, errorClass, errorMessage);

            // Analyze the error and determine action
            DlqAction action = determineAction(envelope, errorClass, errorMessage, retryCount);

            switch (action) {
                case STORE:
                    storeForManualInvestigation(envelope, originalTopic, errorMessage, record);
                    break;

                case REQUEUE:
                    requeueMessage(envelope, originalTopic);
                    dlqRequeueCounter.increment();
                    break;

                case COMPENSATE:
                    performCompensation(envelope, originalTopic, errorMessage);
                    break;

                case ALERT:
                    sendAlert(envelope, originalTopic, errorMessage, errorClass);
                    break;

                case DISCARD:
                    log.info("Discarding DLQ message: EventId={}, Reason=Configured to discard",
                            envelope.getEventId());
                    break;

                default:
                    log.warn("Unknown DLQ action: {}", action);
            }

            // Acknowledge processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            dlqProcessedCounter.increment();

            log.info("Successfully processed DLQ message: EventId={}, Action={}",
                    envelope.getEventId(), action);

        } catch (Exception e) {
            log.error("Error processing DLQ message: EventId={}, Error={}",
                     envelope != null ? envelope.getEventId() : "unknown",
                     e.getMessage(), e);

            // Still acknowledge to prevent infinite loop
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        }
    }

    /**
     * Determine what action to take for the DLQ message
     */
    private DlqAction determineAction(
            EventEnvelope envelope,
            String errorClass,
            String errorMessage,
            String retryCount) {

        // Check retry count
        int retries = parseRetryCount(retryCount);
        if (retries > 10) {
            // Too many retries, store for manual investigation
            return DlqAction.STORE;
        }

        // Check error type
        if (errorClass != null) {
            if (errorClass.contains("NullPointerException") ||
                errorClass.contains("IllegalArgumentException")) {
                // Programming errors - alert and store
                return DlqAction.ALERT;
            }

            if (errorClass.contains("TimeoutException") ||
                errorClass.contains("ConnectException")) {
                // Transient errors - could retry
                if (retries < 5) {
                    return DlqAction.REQUEUE;
                }
            }

            if (errorClass.contains("BusinessException") ||
                errorClass.contains("ValidationException")) {
                // Business rule violations - might need compensation
                return DlqAction.COMPENSATE;
            }
        }

        // Check event type for specific handling
        if (envelope != null && envelope.getEventType() != null) {
            // Add event-specific logic here
            if (envelope.getEventType().contains("Payment")) {
                // Payment events might need special handling
                return DlqAction.COMPENSATE;
            }
        }

        // Default action
        return DlqAction.STORE;
    }

    /**
     * Store message for manual investigation
     */
    private void storeForManualInvestigation(
            EventEnvelope envelope,
            String originalTopic,
            String errorMessage,
            ConsumerRecord<String, EventEnvelope> record) {

        log.info("Storing DLQ message for manual investigation: EventId={}, OriginalTopic={}",
                envelope.getEventId(), originalTopic);

        // In production, this would:
        // 1. Store in a database table for DLQ messages
        // 2. Include all context and error information
        // 3. Provide UI for manual review and reprocessing

        // For now, just log the details
        log.info("DLQ Storage - EventId: {}, EventType: {}, TenantId: {}, " +
                "OriginalTopic: {}, ErrorMessage: {}, Timestamp: {}",
                envelope.getEventId(), envelope.getEventType(), envelope.getTenantId(),
                originalTopic, errorMessage, Instant.now());
    }

    /**
     * Requeue message to original topic for retry
     */
    private void requeueMessage(EventEnvelope envelope, String originalTopic) {
        log.info("Requeuing DLQ message to original topic: EventId={}, Topic={}",
                envelope.getEventId(), originalTopic);

        // In production, this would:
        // 1. Publish the message back to the original topic
        // 2. Add headers indicating it's a retry from DLQ
        // 3. Update retry count
        // 4. Possibly add delay before requeuing
    }

    /**
     * Perform compensation for the failed message
     */
    private void performCompensation(
            EventEnvelope envelope,
            String originalTopic,
            String errorMessage) {

        log.info("Performing compensation for DLQ message: EventId={}, EventType={}",
                envelope.getEventId(), envelope.getEventType());

        // In production, this would:
        // 1. Identify the compensation action based on event type
        // 2. Execute compensating transaction
        // 3. Notify relevant systems of compensation
        // 4. Log compensation details for audit
    }

    /**
     * Send alert for critical DLQ messages
     */
    private void sendAlert(
            EventEnvelope envelope,
            String originalTopic,
            String errorMessage,
            String errorClass) {

        log.error("ALERT: Critical DLQ message requiring attention - " +
                 "EventId: {}, EventType: {}, TenantId: {}, " +
                 "OriginalTopic: {}, ErrorClass: {}, ErrorMessage: {}",
                 envelope.getEventId(), envelope.getEventType(), envelope.getTenantId(),
                 originalTopic, errorClass, errorMessage);

        // In production, this would:
        // 1. Send notification to ops team (email, Slack, PagerDuty)
        // 2. Create incident ticket
        // 3. Update monitoring dashboard
        // 4. Trigger automated recovery if possible
    }

    /**
     * Extract header value from bytes
     */
    private String extractHeader(byte[] headerBytes) {
        if (headerBytes == null) {
            return null;
        }
        return new String(headerBytes, StandardCharsets.UTF_8);
    }

    /**
     * Parse retry count from string
     */
    private int parseRetryCount(String retryCount) {
        if (retryCount == null) {
            return 0;
        }
        try {
            return Integer.parseInt(retryCount);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Actions that can be taken for DLQ messages
     */
    private enum DlqAction {
        STORE,      // Store for manual investigation
        REQUEUE,    // Requeue to original topic
        COMPENSATE, // Perform compensating action
        ALERT,      // Send alert to operations
        DISCARD     // Discard the message
    }
}