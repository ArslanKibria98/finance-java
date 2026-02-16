package com.ksa.islamic.orchestration.saga;

import lombok.Builder;
import lombok.Data;

import java.util.function.Function;

/**
 * Represents a single step in a SAGA transaction
 *
 * Each step has:
 * - A forward action that performs the business operation
 * - A compensation action that reverses the operation if needed
 * - Metadata about the step for logging and monitoring
 */
@Data
@Builder
public class SagaStep<T, R> {

    /**
     * Unique name for this step
     */
    private final String name;

    /**
     * Description of what this step does
     */
    private final String description;

    /**
     * The forward action to execute
     */
    private final Function<T, R> action;

    /**
     * The compensation action to execute on failure
     * Takes the result of the forward action as input
     */
    private final CompensationAction<R> compensation;

    /**
     * Whether this step is critical (failure stops the saga)
     */
    @Builder.Default
    private final boolean critical = true;

    /**
     * Whether to compensate this step if a later step fails
     */
    @Builder.Default
    private final boolean compensatable = true;

    /**
     * Maximum number of retries for this step
     */
    @Builder.Default
    private final int maxRetries = 3;

    /**
     * Execute the forward action
     */
    public R execute(T input) {
        return action.apply(input);
    }

    /**
     * Execute the compensation action
     */
    public void compensate(R result) {
        if (compensatable && compensation != null) {
            compensation.compensate(result);
        }
    }

    /**
     * Creates a step without compensation (point of no return)
     */
    public static <T, R> SagaStep<T, R> nonCompensatable(
            String name,
            String description,
            Function<T, R> action) {
        return SagaStep.<T, R>builder()
                .name(name)
                .description(description)
                .action(action)
                .compensatable(false)
                .build();
    }

    /**
     * Creates a step that shouldn't stop the saga on failure
     */
    public static <T, R> SagaStep<T, R> nonCritical(
            String name,
            String description,
            Function<T, R> action,
            CompensationAction<R> compensation) {
        return SagaStep.<T, R>builder()
                .name(name)
                .description(description)
                .action(action)
                .compensation(compensation)
                .critical(false)
                .build();
    }
}