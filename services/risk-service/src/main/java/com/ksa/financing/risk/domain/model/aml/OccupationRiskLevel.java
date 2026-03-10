package com.ksa.financing.risk.domain.model.aml;

/**
 * Occupation risk classification. PEP is a special category that
 * triggers the Dominant factor override to auto HIGH risk.
 */
public enum OccupationRiskLevel {
    PEP,
    HIGH,
    MEDIUM,
    LOW
}
