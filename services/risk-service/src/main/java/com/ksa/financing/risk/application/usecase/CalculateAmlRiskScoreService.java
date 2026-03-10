package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.risk.domain.model.aml.*;
import com.ksa.financing.risk.domain.port.in.CalculateAmlRiskScoreUseCase;
import com.ksa.financing.risk.domain.port.out.AmlReferenceDataRepository;
import com.ksa.financing.risk.domain.port.out.AmlRiskAssessmentRepository;
import com.ksa.financing.risk.domain.service.AmlRiskScoringEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Use case implementation for AML risk score calculation.
 * Orchestrates reference data lookup, factor resolution, and scoring engine invocation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalculateAmlRiskScoreService implements CalculateAmlRiskScoreUseCase {

    private final AmlReferenceDataRepository referenceDataRepository;
    private final AmlRiskAssessmentRepository assessmentRepository;
    private final AmlRiskScoringEngine scoringEngine;

    @Override
    @Transactional
    public AmlRiskScore calculate(AmlScoringInput input) {
        log.info("Calculating AML risk score for tenant={}, nidHash=...{}",
                input.tenantId(), maskHash(input.nationalIdHash()));

        // Idempotency check
        if (input.idempotencyKey() != null) {
            var existing = assessmentRepository.findByIdempotencyKey(input.tenantId(), input.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Returning cached AML score for idempotencyKey={}", input.idempotencyKey());
                return existing.get();
            }
        }

        String seedTenant = "00000000-0000-0000-0000-000000000001";

        // Load reference data for the tenant (falls back to seed tenant)
        var tenantCategories = referenceDataRepository.findActiveCategories(input.tenantId());
        if (tenantCategories.isEmpty()) {
            log.warn("No AML categories found for tenant={}, falling back to seed tenant", input.tenantId());
            tenantCategories = referenceDataRepository.findActiveCategories(seedTenant);
        }
        final var categories = tenantCategories;

        var tenantThresholds = referenceDataRepository.findActiveThresholds(input.tenantId());
        if (tenantThresholds.isEmpty()) {
            tenantThresholds = referenceDataRepository.findActiveThresholds(seedTenant);
        }
        final var thresholds = tenantThresholds;

        // Load all factors grouped by category code
        var categoryCodes = categories.stream()
                .map(AmlRiskCategory::categoryCode)
                .collect(Collectors.toList());
        var tenantFactors = referenceDataRepository.findActiveFactorsByCategoryCodes(input.tenantId(), categoryCodes);
        if (tenantFactors.isEmpty()) {
            tenantFactors = referenceDataRepository.findActiveFactorsByCategoryCodes(seedTenant, categoryCodes);
        }

        Map<String, List<AmlRiskCategoryFactor>> factorsByCategory = tenantFactors.stream()
                .collect(Collectors.groupingBy(f -> {
                    return categories.stream()
                            .filter(c -> c.id().equals(f.categoryId()))
                            .map(AmlRiskCategory::categoryCode)
                            .findFirst()
                            .orElse("UNKNOWN");
                }));

        // Resolve external lookups
        var nationalityInfo = referenceDataRepository.findFatfCategoryByCountryCode(
                input.tenantId(), input.nationality());
        if (nationalityInfo.isEmpty()) {
            nationalityInfo = referenceDataRepository.findFatfCategoryByCountryCode(
                    seedTenant, input.nationality());
        }

        var cityRiskLevel = referenceDataRepository.findCityRiskLevel(
                input.tenantId(), input.cityName());
        if (cityRiskLevel.isEmpty()) {
            cityRiskLevel = referenceDataRepository.findCityRiskLevel(
                    seedTenant, input.cityName());
        }

        var occupationRisk = referenceDataRepository.findOccupationRiskLevel(
                input.tenantId(), input.occupationCode());
        if (occupationRisk.isEmpty()) {
            occupationRisk = referenceDataRepository.findOccupationRiskLevel(
                    seedTenant, input.occupationCode());
        }

        var incomeFactorCode = referenceDataRepository.findIncomeRangeFactorCode(
                input.tenantId(), input.monthlyIncome());
        if (incomeFactorCode.isEmpty()) {
            incomeFactorCode = referenceDataRepository.findIncomeRangeFactorCode(
                    seedTenant, input.monthlyIncome());
        }

        // Resolve factors
        Map<String, String> resolvedFactors = scoringEngine.resolveFactors(
                input,
                nationalityInfo.orElse(null),
                cityRiskLevel.orElse(null),
                occupationRisk.orElse(null),
                incomeFactorCode.orElse(null)
        );

        log.debug("Resolved AML factors: {}", resolvedFactors);

        // Calculate score
        AmlRiskScore score = scoringEngine.calculate(
                input, categories, factorsByCategory, thresholds, resolvedFactors);

        // Persist result
        assessmentRepository.save(score, input);

        log.info("AML risk score calculated: assessmentId={}, totalScore={}, riskLevel={}, dominantOverride={}",
                score.assessmentId(), score.totalScore(), score.riskLevel(), score.dominantOverride());

        return score;
    }

    private String maskHash(String hash) {
        if (hash == null || hash.length() < 8) return "***";
        return hash.substring(hash.length() - 8);
    }
}
