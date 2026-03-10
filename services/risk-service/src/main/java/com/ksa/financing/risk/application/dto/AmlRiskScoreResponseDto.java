package com.ksa.financing.risk.application.dto;

import com.ksa.financing.risk.domain.model.aml.AmlCategoryScoreBreakdown;
import com.ksa.financing.risk.domain.model.aml.AmlRiskScore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for AML risk score calculation.
 */
public record AmlRiskScoreResponseDto(
    UUID assessmentId,
    BigDecimal totalScore,
    String riskLevel,
    boolean dominantOverride,
    String dominantCategory,
    List<AmlCategoryScoreBreakdown> breakdown,
    Instant assessedAt
) {
    public static AmlRiskScoreResponseDto from(AmlRiskScore score) {
        return new AmlRiskScoreResponseDto(
                score.assessmentId(),
                score.totalScore(),
                score.riskLevel().name(),
                score.dominantOverride(),
                score.dominantCategory(),
                score.breakdown(),
                score.assessedAt()
        );
    }
}
