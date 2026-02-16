package com.ksa.islamic.orchestration.saga;

/**
 * Interface for SAGA compensation actions
 *
 * Defines how to reverse or compensate for a completed action
 * when a subsequent step in the saga fails.
 *
 * @param <T> The type of the result from the forward action
 */
@FunctionalInterface
public interface CompensationAction<T> {

    /**
     * Compensate for a previously executed action
     *
     * @param actionResult The result from the forward action that needs to be compensated
     */
    void compensate(T actionResult);

    /**
     * Creates a no-op compensation (for non-compensatable actions)
     */
    static <T> CompensationAction<T> noOp() {
        return actionResult -> {
            // No compensation needed
        };
    }

    /**
     * Chains multiple compensation actions
     */
    default CompensationAction<T> andThen(CompensationAction<T> after) {
        return actionResult -> {
            this.compensate(actionResult);
            after.compensate(actionResult);
        };
    }
}