package com.ksa.financing.risk.domain.model.review;

public enum ReviewTaskStatus {
    PENDING_MAKER,
    MAKER_RECOMMENDED,
    PENDING_APPROVER,
    APPROVED,
    REJECTED,
    ESCALATED,
    EXPIRED
}
