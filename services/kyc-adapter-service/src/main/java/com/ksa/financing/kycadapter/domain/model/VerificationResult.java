package com.ksa.financing.kycadapter.domain.model;

public enum VerificationResult {
    VERIFIED,
    NOT_VERIFIED,
    PARTIAL_MATCH,
    DATA_MISMATCH,
    EXPIRED_DOCUMENT,
    PROVIDER_ERROR
}
