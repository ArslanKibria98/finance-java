package com.ksa.islamic.orchestration.exception;

/**
 * Exception for activity execution failures
 *
 * Provides detailed information about activity failures
 * to enable proper error handling and retry logic.
 */
public class ActivityFailureException extends RuntimeException {

    private final String activityType;
    private final String activityId;
    private final int attemptNumber;
    private final boolean retryable;
    private final String errorCode;

    public ActivityFailureException(String message, String activityType, String activityId) {
        super(message);
        this.activityType = activityType;
        this.activityId = activityId;
        this.attemptNumber = 1;
        this.retryable = true;
        this.errorCode = null;
    }

    public ActivityFailureException(String message, String activityType, String activityId, boolean retryable) {
        super(message);
        this.activityType = activityType;
        this.activityId = activityId;
        this.attemptNumber = 1;
        this.retryable = retryable;
        this.errorCode = null;
    }

    public ActivityFailureException(String message, String activityType, String activityId,
                                  int attemptNumber, boolean retryable, String errorCode) {
        super(message);
        this.activityType = activityType;
        this.activityId = activityId;
        this.attemptNumber = attemptNumber;
        this.retryable = retryable;
        this.errorCode = errorCode;
    }

    public ActivityFailureException(String message, String activityType, String activityId,
                                  int attemptNumber, boolean retryable, String errorCode, Throwable cause) {
        super(message, cause);
        this.activityType = activityType;
        this.activityId = activityId;
        this.attemptNumber = attemptNumber;
        this.retryable = retryable;
        this.errorCode = errorCode;
    }

    public String getActivityType() {
        return activityType;
    }

    public String getActivityId() {
        return activityId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Create a non-retryable activity failure
     */
    public static ActivityFailureException nonRetryable(String message, String activityType,
                                                       String activityId, String errorCode) {
        return new ActivityFailureException(message, activityType, activityId, 1, false, errorCode);
    }

    /**
     * Create a business rule violation failure (non-retryable)
     */
    public static ActivityFailureException businessRuleViolation(String message, String activityType, String activityId) {
        return new ActivityFailureException(message, activityType, activityId, 1, false, "BUSINESS_RULE_VIOLATION");
    }

    /**
     * Create a validation failure (non-retryable)
     */
    public static ActivityFailureException validationFailure(String message, String activityType, String activityId) {
        return new ActivityFailureException(message, activityType, activityId, 1, false, "VALIDATION_ERROR");
    }

    /**
     * Create a temporary failure (retryable)
     */
    public static ActivityFailureException temporaryFailure(String message, String activityType,
                                                           String activityId, Throwable cause) {
        return new ActivityFailureException(message, activityType, activityId, 1, true, "TEMPORARY_FAILURE", cause);
    }

    @Override
    public String toString() {
        return String.format("ActivityFailureException{activityType='%s', activityId='%s', attemptNumber=%d, " +
                        "retryable=%s, errorCode='%s', message='%s'}",
                activityType, activityId, attemptNumber, retryable, errorCode, getMessage());
    }
}