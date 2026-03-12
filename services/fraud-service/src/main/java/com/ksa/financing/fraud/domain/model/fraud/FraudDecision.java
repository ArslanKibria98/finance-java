package com.ksa.financing.fraud.domain.model.fraud;

public enum FraudDecision {
    ALLOW,
    ALERT,
    HOLD,
    BLOCK;

    /**
     * Returns the higher-priority decision. Priority: BLOCK > HOLD > ALERT > ALLOW.
     */
    public static FraudDecision higher(FraudDecision a, FraudDecision b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
