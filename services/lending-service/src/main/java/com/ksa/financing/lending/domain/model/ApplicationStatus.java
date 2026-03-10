package com.ksa.financing.lending.domain.model;

public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    DOCUMENTS_PENDING,
    UNDER_REVIEW,
    CREDIT_CHECK,
    SHARIA_VALIDATION,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    CANCELLED,
    EXPIRED;

    public boolean canTransitionTo(ApplicationStatus target) {
        return switch (this) {
            case DRAFT -> target == SUBMITTED || target == CANCELLED;
            case SUBMITTED -> target == DOCUMENTS_PENDING || target == UNDER_REVIEW || target == CANCELLED;
            case DOCUMENTS_PENDING -> target == UNDER_REVIEW || target == CANCELLED;
            case UNDER_REVIEW -> target == CREDIT_CHECK || target == REJECTED || target == CANCELLED;
            case CREDIT_CHECK -> target == SHARIA_VALIDATION || target == REJECTED;
            case SHARIA_VALIDATION -> target == PENDING_APPROVAL || target == REJECTED;
            case PENDING_APPROVAL -> target == APPROVED || target == REJECTED;
            case APPROVED, REJECTED, CANCELLED, EXPIRED -> false;
        };
    }

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == CANCELLED || this == EXPIRED;
    }
}
