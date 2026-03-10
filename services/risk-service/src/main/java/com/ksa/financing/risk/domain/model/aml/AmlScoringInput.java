package com.ksa.financing.risk.domain.model.aml;

import java.math.BigDecimal;

/**
 * Input data required for AML risk score calculation.
 * Collected from customer onboarding profile, Nafath verification, and KYC data.
 */
public record AmlScoringInput(
    String nationalIdHash,
    String nationality,
    String cityName,
    String occupationCode,
    BigDecimal monthlyIncome,
    String sourceOfIncome,
    String productRiskTier,
    boolean isPep,
    boolean isOnInternalList,
    String tenantId,
    String customerId,
    String idempotencyKey
) {}
