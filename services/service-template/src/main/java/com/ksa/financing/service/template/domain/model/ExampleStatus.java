package com.ksa.financing.service.template.domain.model;

/**
 * Enumeration representing the status of an ExampleAggregate.
 * Demonstrates state machine pattern in domain model.
 */
public enum ExampleStatus {
    DRAFT,
    ACTIVE,
    SUSPENDED,
    COMPLETED,
    CANCELLED;

    /**
     * Check if transition to target status is allowed.
     */
    public boolean canTransitionTo(ExampleStatus target) {
        return switch (this) {
            case DRAFT -> target == ACTIVE || target == CANCELLED;
            case ACTIVE -> target == SUSPENDED || target == COMPLETED || target == CANCELLED;
            case SUSPENDED -> target == ACTIVE || target == CANCELLED;
            case COMPLETED, CANCELLED -> false; // Terminal states
        };
    }
}