package com.ksa.financing.risk.domain.model.aml;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Result of an AML risk scoring assessment.
 * Contains the total score, risk level, and full breakdown per category.
 */
public record AmlRiskScore(
    UUID assessmentId,
    BigDecimal totalScore,
    AmlRiskLevel riskLevel,
    boolean dominantOverride,
    String dominantCategory,
    List<AmlCategoryScoreBreakdown> breakdown,
    Instant assessedAt
) {

    /**
     * @deprecated Use {@link #dominantOverride(BigDecimal, String, List)} instead.
     * Old behaviour forced totalScore=999 which contradicted the AML schema
     * (DOMINANT factors override RISK LEVEL only, not the underlying weighted
     * sum). Kept for backward compatibility.
     */
    @Deprecated
    public static AmlRiskScore dominantHighRisk(String dominantCategoryCode, List<AmlCategoryScoreBreakdown> breakdown) {
        return new AmlRiskScore(
            UUID.randomUUID(),
            new BigDecimal("999"),
            AmlRiskLevel.HIGH,
            true,
            dominantCategoryCode,
            breakdown,
            Instant.now()
        );
    }

    /**
     * Factory for DOMINANT-override scoring (per AML schema spec):
     *  - {@code totalScore} preserves the real mutual-exclusive weighted sum
     *  - {@code riskLevel} is forced to HIGH
     *  - {@code dominantOverride=true} so the UI / consumers can flag it
     */
    public static AmlRiskScore dominantOverride(BigDecimal totalScore,
                                                 String dominantCategoryCode,
                                                 List<AmlCategoryScoreBreakdown> breakdown) {
        return new AmlRiskScore(
            UUID.randomUUID(),
            totalScore,
            AmlRiskLevel.HIGH,
            true,
            dominantCategoryCode,
            breakdown,
            Instant.now()
        );
    }

    /**
     * Factory for a weighted-sum scoring result.
     */
    public static AmlRiskScore fromWeightedSum(BigDecimal totalScore, AmlRiskLevel level,
                                                 List<AmlCategoryScoreBreakdown> breakdown) {
        return new AmlRiskScore(
            UUID.randomUUID(),
            totalScore,
            level,
            false,
            null,
            breakdown,
            Instant.now()
        );
    }
}
