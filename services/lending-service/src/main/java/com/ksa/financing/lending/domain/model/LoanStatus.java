package com.ksa.financing.lending.domain.model;

public enum LoanStatus {
    PENDING_DISBURSEMENT,
    ACTIVE,
    DELINQUENT,
    DEFAULT,
    RESTRUCTURED,
    SETTLED,
    WRITTEN_OFF,
    CLOSED;

    public boolean canTransitionTo(LoanStatus target) {
        return switch (this) {
            case PENDING_DISBURSEMENT -> target == ACTIVE;
            case ACTIVE -> target == DELINQUENT || target == SETTLED || target == RESTRUCTURED;
            case DELINQUENT -> target == ACTIVE || target == DEFAULT;
            case DEFAULT -> target == WRITTEN_OFF;
            case RESTRUCTURED -> target == ACTIVE;
            case SETTLED, WRITTEN_OFF, CLOSED -> false;
        };
    }

    public boolean isTerminal() {
        return this == SETTLED || this == WRITTEN_OFF || this == CLOSED;
    }
}
