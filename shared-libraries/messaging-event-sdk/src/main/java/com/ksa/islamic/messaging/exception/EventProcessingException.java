package com.ksa.islamic.messaging.exception;

/**
 * Exception thrown when event processing fails
 */
public class EventProcessingException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String eventId;
    private final String eventType;
    private final boolean recoverable;
    private final int retryCount;

    public EventProcessingException(String message) {
        super(message);
        this.eventId = null;
        this.eventType = null;
        this.recoverable = true;
        this.retryCount = 0;
    }

    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.eventId = null;
        this.eventType = null;
        this.recoverable = determineRecoverable(cause);
        this.retryCount = 0;
    }

    public EventProcessingException(String message, Throwable cause, boolean recoverable) {
        super(message, cause);
        this.eventId = null;
        this.eventType = null;
        this.recoverable = recoverable;
        this.retryCount = 0;
    }

    public EventProcessingException(String message, String eventId, String eventType) {
        super(message);
        this.eventId = eventId;
        this.eventType = eventType;
        this.recoverable = true;
        this.retryCount = 0;
    }

    public EventProcessingException(
            String message,
            String eventId,
            String eventType,
            Throwable cause,
            boolean recoverable,
            int retryCount) {
        super(message, cause);
        this.eventId = eventId;
        this.eventType = eventType;
        this.recoverable = recoverable;
        this.retryCount = retryCount;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    public int getRetryCount() {
        return retryCount;
    }

    /**
     * Create a non-recoverable exception
     */
    public static EventProcessingException nonRecoverable(String message) {
        return new EventProcessingException(message, null, false);
    }

    /**
     * Create a non-recoverable exception with cause
     */
    public static EventProcessingException nonRecoverable(String message, Throwable cause) {
        return new EventProcessingException(message, cause, false);
    }

    /**
     * Create a recoverable exception
     */
    public static EventProcessingException recoverable(String message) {
        return new EventProcessingException(message, null, true);
    }

    /**
     * Create a recoverable exception with cause
     */
    public static EventProcessingException recoverable(String message, Throwable cause) {
        return new EventProcessingException(message, cause, true);
    }

    private boolean determineRecoverable(Throwable cause) {
        if (cause == null) {
            return true;
        }

        // Non-recoverable exceptions
        if (cause instanceof IllegalArgumentException ||
            cause instanceof NullPointerException ||
            cause instanceof IllegalStateException ||
            cause instanceof ClassCastException ||
            cause instanceof UnsupportedOperationException) {
            return false;
        }

        // Check for specific business exceptions
        String className = cause.getClass().getName();
        if (className.contains("ValidationException") ||
            className.contains("BusinessRuleException") ||
            className.contains("InvalidStateException")) {
            return false;
        }

        // Default to recoverable for infrastructure issues
        return true;
    }

    /**
     * Check if retry should be attempted based on retry count
     */
    public boolean shouldRetry(int maxRetries) {
        return recoverable && retryCount < maxRetries;
    }
}