package com.ksa.islamic.messaging.exception;

/**
 * Exception thrown when event publishing fails
 */
public class EventPublishingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String eventId;
    private final String topicName;
    private final boolean recoverable;

    public EventPublishingException(String message) {
        super(message);
        this.eventId = null;
        this.topicName = null;
        this.recoverable = true;
    }

    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
        this.eventId = null;
        this.topicName = null;
        this.recoverable = determineRecoverable(cause);
    }

    public EventPublishingException(String message, String eventId, String topicName) {
        super(message);
        this.eventId = eventId;
        this.topicName = topicName;
        this.recoverable = true;
    }

    public EventPublishingException(String message, String eventId, String topicName, Throwable cause) {
        super(message, cause);
        this.eventId = eventId;
        this.topicName = topicName;
        this.recoverable = determineRecoverable(cause);
    }

    public EventPublishingException(String message, boolean recoverable) {
        super(message);
        this.eventId = null;
        this.topicName = null;
        this.recoverable = recoverable;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTopicName() {
        return topicName;
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    private boolean determineRecoverable(Throwable cause) {
        if (cause == null) {
            return true;
        }

        // Non-recoverable exceptions
        if (cause instanceof IllegalArgumentException ||
            cause instanceof NullPointerException ||
            cause instanceof ClassCastException) {
            return false;
        }

        // Kafka-specific non-recoverable exceptions
        String className = cause.getClass().getName();
        if (className.contains("SerializationException") ||
            className.contains("InvalidTopicException")) {
            return false;
        }

        // Default to recoverable for network/timeout issues
        return true;
    }
}