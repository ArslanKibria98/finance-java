package com.ksa.financing.risk.domain.model.aml;

/**
 * AML risk classification levels based on EastNets scoring model.
 * HIGH (42+): Trigger EDD
 * MEDIUM (31-41): Flag for review
 * LOW (0-30): Auto-proceed
 */
public enum AmlRiskLevel {
    HIGH,
    MEDIUM,
    LOW
}
