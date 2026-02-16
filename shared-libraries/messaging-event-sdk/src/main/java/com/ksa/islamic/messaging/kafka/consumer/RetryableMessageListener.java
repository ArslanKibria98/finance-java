package com.ksa.islamic.messaging.kafka.consumer;

import com.ksa.islamic.messaging.contract.EventEnvelope;
import com.ksa.islamic.messaging.exception.EventProcessingException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * Retryable Message Listener with exponential backoff
 * Wraps message processing with retry logic
 */
@Slf4j
public abstract class RetryableMessageListener<T> implements AcknowledgingMessageListener<String, T> {

    private final Retry retry;
    private final MeterRegistry meterRegistry;

    protected RetryableMessageListener(String listenerName, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.retry = configureRetry(listenerName);
    }

    /**
     * Configure retry with exponential backoff
     */
    private Retry configureRetry(String listenerName) {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(1000))
            .intervalFunction(intervalMillis -> {
                // Exponential backoff: 1s, 2s, 4s
                return intervalMillis * 2;
            })
            .retryExceptions(EventProcessingException.class, RuntimeException.class)
            .ignoreExceptions(IllegalArgumentException.class, NullPointerException.class)
            .retryOnResult(retryResultPredicate())
            .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry(listenerName);

        // Add event listeners for monitoring
        retry.getEventPublisher()
            .onRetry(event -> log.warn("Retry attempt {} for listener: {}, Event: {}",
                                      event.getNumberOfRetryAttempts(),
                                      listenerName,
                                      event.getLastThrowable().getMessage()))
            .onSuccess(event -> log.debug("Successfully processed after {} attempts",
                                         event.getNumberOfRetryAttempts()))
            .onError(event -> log.error("Failed after {} retry attempts: {}",
                                       event.getNumberOfRetryAttempts(),
                                       event.getLastThrowable().getMessage()));

        return retry;
    }

    @Override
    public void onMessage(ConsumerRecord<String, T> record, Acknowledgment acknowledgment) {
        log.debug("Received message: Topic={}, Partition={}, Offset={}",
                 record.topic(), record.partition(), record.offset());

        try {
            // Execute with retry
            Callable<ProcessingResult> callable = () -> processMessageInternal(record);
            ProcessingResult result = Retry.decorateCallable(retry, callable).call();

            if (result.isSuccess()) {
                // Acknowledge successful processing
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                log.debug("Message processed successfully: Offset={}", record.offset());
            } else {
                handleProcessingFailure(record, result, acknowledgment);
            }

        } catch (Exception e) {
            log.error("Failed to process message after all retry attempts: Topic={}, Offset={}, Error={}",
                     record.topic(), record.offset(), e.getMessage(), e);
            handleFinalFailure(record, e, acknowledgment);
        }
    }

    /**
     * Internal message processing with result wrapper
     */
    private ProcessingResult processMessageInternal(ConsumerRecord<String, T> record) {
        try {
            // Pre-processing hook
            beforeProcessing(record);

            // Process the message
            processMessage(record);

            // Post-processing hook
            afterProcessing(record);

            return ProcessingResult.success();

        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());

            // Check if error is retryable
            if (isRetryableError(e)) {
                throw new EventProcessingException("Retryable error occurred", e, true);
            } else {
                return ProcessingResult.failure(e.getMessage());
            }
        }
    }

    /**
     * Process the message - to be implemented by subclasses
     */
    protected abstract void processMessage(ConsumerRecord<String, T> record) throws Exception;

    /**
     * Optional pre-processing hook
     */
    protected void beforeProcessing(ConsumerRecord<String, T> record) {
        // Override in subclasses if needed
    }

    /**
     * Optional post-processing hook
     */
    protected void afterProcessing(ConsumerRecord<String, T> record) {
        // Override in subclasses if needed
    }

    /**
     * Determine if an error is retryable
     */
    protected boolean isRetryableError(Exception e) {
        // Non-retryable errors
        if (e instanceof IllegalArgumentException ||
            e instanceof NullPointerException ||
            e instanceof ClassCastException) {
            return false;
        }

        // Check for specific non-retryable business exceptions
        if (e instanceof EventProcessingException) {
            return ((EventProcessingException) e).isRecoverable();
        }

        // Default to retryable
        return true;
    }

    /**
     * Predicate for retry based on result
     */
    protected Predicate<ProcessingResult> retryResultPredicate() {
        return result -> !result.isSuccess() && result.isRetryable();
    }

    /**
     * Handle processing failure (non-retryable)
     */
    protected void handleProcessingFailure(
            ConsumerRecord<String, T> record,
            ProcessingResult result,
            Acknowledgment acknowledgment) {

        log.warn("Message processing failed (non-retryable): Topic={}, Offset={}, Reason={}",
                record.topic(), record.offset(), result.getErrorMessage());

        // Acknowledge to move past the message
        if (acknowledgment != null) {
            acknowledgment.acknowledge();
        }

        // Send to DLQ or alert
        sendToDeadLetterQueue(record, result.getErrorMessage());
    }

    /**
     * Handle final failure after all retries
     */
    protected void handleFinalFailure(
            ConsumerRecord<String, T> record,
            Exception exception,
            Acknowledgment acknowledgment) {

        log.error("Message processing failed after all retries: Topic={}, Offset={}",
                 record.topic(), record.offset(), exception);

        // Don't acknowledge - let the error handler deal with it
        // The message will be sent to DLQ by the error handler

        // Report to monitoring
        reportFailure(record, exception);
    }

    /**
     * Send message to DLQ
     */
    protected void sendToDeadLetterQueue(ConsumerRecord<String, T> record, String reason) {
        log.info("Sending message to DLQ: Topic={}, Offset={}, Reason={}",
                record.topic(), record.offset(), reason);
        // Implementation would send to DLQ topic
    }

    /**
     * Report failure to monitoring system
     */
    protected void reportFailure(ConsumerRecord<String, T> record, Exception exception) {
        // Override in subclasses to implement specific error reporting
    }

    /**
     * Result wrapper for processing outcome
     */
    protected static class ProcessingResult {
        private final boolean success;
        private final String errorMessage;
        private final boolean retryable;

        private ProcessingResult(boolean success, String errorMessage, boolean retryable) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.retryable = retryable;
        }

        public static ProcessingResult success() {
            return new ProcessingResult(true, null, false);
        }

        public static ProcessingResult failure(String errorMessage) {
            return new ProcessingResult(false, errorMessage, false);
        }

        public static ProcessingResult retryableFailure(String errorMessage) {
            return new ProcessingResult(false, errorMessage, true);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }
}