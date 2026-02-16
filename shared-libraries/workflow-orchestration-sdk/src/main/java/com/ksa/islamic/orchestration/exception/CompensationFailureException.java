package com.ksa.islamic.orchestration.exception;

/**
 * Exception for SAGA compensation failures
 *
 * Thrown when a compensation action fails during SAGA rollback.
 * This is a critical error that may require manual intervention.
 */
public class CompensationFailureException extends RuntimeException {

    private final String sagaName;
    private final String stepName;
    private final String originalFailure;
    private final boolean requiresManualIntervention;

    public CompensationFailureException(String message, String sagaName, String stepName) {
        super(message);
        this.sagaName = sagaName;
        this.stepName = stepName;
        this.originalFailure = null;
        this.requiresManualIntervention = true;
    }

    public CompensationFailureException(String message, String sagaName, String stepName, String originalFailure) {
        super(message);
        this.sagaName = sagaName;
        this.stepName = stepName;
        this.originalFailure = originalFailure;
        this.requiresManualIntervention = true;
    }

    public CompensationFailureException(String message, String sagaName, String stepName,
                                       String originalFailure, Throwable cause) {
        super(message, cause);
        this.sagaName = sagaName;
        this.stepName = stepName;
        this.originalFailure = originalFailure;
        this.requiresManualIntervention = true;
    }

    public CompensationFailureException(String message, String sagaName, String stepName,
                                       String originalFailure, boolean requiresManualIntervention, Throwable cause) {
        super(message, cause);
        this.sagaName = sagaName;
        this.stepName = stepName;
        this.originalFailure = originalFailure;
        this.requiresManualIntervention = requiresManualIntervention;
    }

    public String getSagaName() {
        return sagaName;
    }

    public String getStepName() {
        return stepName;
    }

    public String getOriginalFailure() {
        return originalFailure;
    }

    public boolean requiresManualIntervention() {
        return requiresManualIntervention;
    }

    /**
     * Create a critical compensation failure requiring manual intervention
     */
    public static CompensationFailureException critical(String sagaName, String stepName,
                                                       String originalFailure, Throwable cause) {
        String message = String.format("Critical compensation failure in SAGA '%s' at step '%s'. " +
                        "Manual intervention required. Original failure: %s",
                sagaName, stepName, originalFailure);
        return new CompensationFailureException(message, sagaName, stepName, originalFailure, true, cause);
    }

    /**
     * Create a compensation failure that was partially successful
     */
    public static CompensationFailureException partial(String sagaName, String stepName, String details) {
        String message = String.format("Partial compensation in SAGA '%s' at step '%s': %s",
                sagaName, stepName, details);
        return new CompensationFailureException(message, sagaName, stepName, details, false, null);
    }

    @Override
    public String toString() {
        return String.format("CompensationFailureException{sagaName='%s', stepName='%s', " +
                        "originalFailure='%s', requiresManualIntervention=%s, message='%s'}",
                sagaName, stepName, originalFailure, requiresManualIntervention, getMessage());
    }
}