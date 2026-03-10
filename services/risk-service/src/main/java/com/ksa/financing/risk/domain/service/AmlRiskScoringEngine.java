package com.ksa.financing.risk.domain.service;

import com.ksa.financing.risk.domain.model.aml.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pure domain service implementing the AML risk scoring algorithm.
 * Based on EastNets AML Risk Schema (Individuals Schema).
 *
 * Algorithm:
 * 1. Check DOMINANT factors first (PEP, Internal List) → auto HIGH if matched
 * 2. Calculate MUTUAL_EXCLUSIVE weighted sum for remaining categories
 * 3. Compare total score against configurable thresholds
 *
 * Zero framework imports — pure business logic.
 */
public class AmlRiskScoringEngine {

    /**
     * Calculate AML risk score for a customer based on their profile data.
     *
     * @param input      customer data collected from KYC/onboarding
     * @param categories configured risk categories with weights
     * @param factors    factor options grouped by category
     * @param thresholds risk level score ranges
     * @param resolvedFactors pre-resolved factor codes per category
     * @return complete AML risk score with breakdown
     */
    public AmlRiskScore calculate(AmlScoringInput input,
                                   List<AmlRiskCategory> categories,
                                   Map<String, List<AmlRiskCategoryFactor>> factors,
                                   List<AmlRiskThreshold> thresholds,
                                   Map<String, String> resolvedFactors) {

        List<AmlCategoryScoreBreakdown> breakdown = new ArrayList<>();

        // Step 1: Check DOMINANT factors (auto HIGH risk override)
        for (AmlRiskCategory category : categories) {
            if (category.categoryType() == AmlCategoryType.DOMINANT && category.active()) {
                String resolvedFactor = resolvedFactors.get(category.categoryCode());
                if (resolvedFactor != null) {
                    var categoryFactors = factors.getOrDefault(category.categoryCode(), List.of());
                    var matchedFactor = categoryFactors.stream()
                            .filter(f -> f.factorCode().equals(resolvedFactor))
                            .findFirst()
                            .orElse(null);

                    if (matchedFactor != null && matchedFactor.factorWeightPct().compareTo(BigDecimal.ZERO) > 0) {
                        breakdown.add(new AmlCategoryScoreBreakdown(
                                category.categoryCode(),
                                category.nameEn(),
                                matchedFactor.factorCode(),
                                category.weight(),
                                matchedFactor.factorWeightPct(),
                                matchedFactor.computedRating()
                        ));
                        return AmlRiskScore.dominantHighRisk(category.categoryCode(), breakdown);
                    }
                }
            }
        }

        // Step 2: Calculate MUTUAL_EXCLUSIVE weighted sum
        BigDecimal totalScore = BigDecimal.ZERO;

        for (AmlRiskCategory category : categories) {
            if (category.categoryType() == AmlCategoryType.MUTUAL_EXCLUSIVE && category.active()) {
                String resolvedFactor = resolvedFactors.get(category.categoryCode());
                var categoryFactors = factors.getOrDefault(category.categoryCode(), List.of());

                BigDecimal rating = BigDecimal.ZERO;
                String matchedFactorCode = "NONE";

                if (resolvedFactor != null) {
                    var matchedFactor = categoryFactors.stream()
                            .filter(f -> f.factorCode().equals(resolvedFactor))
                            .findFirst()
                            .orElse(null);

                    if (matchedFactor != null) {
                        rating = matchedFactor.computedRating();
                        matchedFactorCode = matchedFactor.factorCode();
                    }
                }

                breakdown.add(new AmlCategoryScoreBreakdown(
                        category.categoryCode(),
                        category.nameEn(),
                        matchedFactorCode,
                        category.weight(),
                        rating.compareTo(BigDecimal.ZERO) > 0
                                ? rating.multiply(new BigDecimal("100")).divide(category.weight(), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO,
                        rating
                ));

                totalScore = totalScore.add(rating);
            }
        }

        // Step 3: Determine risk level from thresholds
        AmlRiskLevel level = determineRiskLevel(totalScore, thresholds);

        return AmlRiskScore.fromWeightedSum(
                totalScore.setScale(2, RoundingMode.HALF_UP),
                level,
                breakdown
        );
    }

    /**
     * Resolve which factor applies for each category based on customer input data.
     * This maps raw customer data to category factor codes.
     *
     * @param input           customer scoring input
     * @param nationalityInfo nationality → FATF category mapping
     * @param cityRiskLevel   city → risk level mapping
     * @param occupationRisk  occupation code → risk level mapping
     * @param incomeFactorCode resolved income range factor code
     * @return map of categoryCode → factorCode
     */
    public Map<String, String> resolveFactors(AmlScoringInput input,
                                                FatfCategory nationalityInfo,
                                                AmlRiskLevel cityRiskLevel,
                                                OccupationRiskLevel occupationRisk,
                                                String incomeFactorCode) {

        var resolved = new java.util.LinkedHashMap<String, String>();

        // PEP (Dominant)
        resolved.put("PEP", input.isPep() ? "PEP_YES" : "PEP_NO");

        // Internal List (Dominant)
        resolved.put("INTERNAL_LIST", input.isOnInternalList() ? "INTERNAL_LIST_YES" : "INTERNAL_LIST_NO");

        // Nationality
        if (nationalityInfo != null) {
            switch (nationalityInfo) {
                case HIGH_RISK -> resolved.put("NATIONALITY", "FATF_HIGH_RISK");
                case INCREASED_MONITORING -> resolved.put("NATIONALITY", "REST_OF_WORLD");
                case STANDARD -> {
                    if (isGccCountry(input.nationality())) {
                        resolved.put("NATIONALITY", "SAUDI_GCC");
                    } else {
                        resolved.put("NATIONALITY", "REST_OF_WORLD");
                    }
                }
            }
        } else {
            resolved.put("NATIONALITY", "REST_OF_WORLD");
        }

        // Geographical Location (City)
        if (cityRiskLevel != null) {
            switch (cityRiskLevel) {
                case HIGH -> resolved.put("GEOGRAPHICAL_LOCATION", "HR_REGION");
                case MEDIUM -> resolved.put("GEOGRAPHICAL_LOCATION", "MR_REGION");
                case LOW -> resolved.put("GEOGRAPHICAL_LOCATION", "LR_REGION");
            }
        } else {
            resolved.put("GEOGRAPHICAL_LOCATION", "LR_REGION");
        }

        // Product & Services
        if (input.productRiskTier() != null) {
            switch (input.productRiskTier().toUpperCase()) {
                case "HIGH", "HR" -> resolved.put("PRODUCT_SERVICES", "HR_PRODUCT");
                case "MEDIUM", "MR" -> resolved.put("PRODUCT_SERVICES", "MR_PRODUCT");
                default -> resolved.put("PRODUCT_SERVICES", "LR_PRODUCT");
            }
        } else {
            resolved.put("PRODUCT_SERVICES", "LR_PRODUCT");
        }

        // Occupations
        if (occupationRisk != null) {
            switch (occupationRisk) {
                case PEP -> {
                    resolved.put("OCCUPATIONS", "HR_OCCUPATION");
                    // Also trigger PEP dominant override
                    resolved.put("PEP", "PEP_YES");
                }
                case HIGH -> resolved.put("OCCUPATIONS", "HR_OCCUPATION");
                case MEDIUM -> resolved.put("OCCUPATIONS", "MR_OCCUPATION");
                case LOW -> resolved.put("OCCUPATIONS", "LR_OCCUPATION");
            }
        } else {
            resolved.put("OCCUPATIONS", "LR_OCCUPATION");
        }

        // Income Range
        if (incomeFactorCode != null) {
            resolved.put("INCOME_RANGE", incomeFactorCode);
        } else {
            resolved.put("INCOME_RANGE", "INCOME_0_3000");
        }

        // Source of Income
        if (input.sourceOfIncome() != null) {
            resolved.put("SOURCE_OF_INCOME", input.sourceOfIncome().toUpperCase());
        } else {
            resolved.put("SOURCE_OF_INCOME", "SALARY");
        }

        return resolved;
    }

    private AmlRiskLevel determineRiskLevel(BigDecimal totalScore, List<AmlRiskThreshold> thresholds) {
        for (AmlRiskThreshold threshold : thresholds) {
            if (totalScore.compareTo(threshold.minScore()) >= 0
                    && totalScore.compareTo(threshold.maxScore()) <= 0) {
                return threshold.riskLevel();
            }
        }
        // Default to LOW if no threshold matches
        return AmlRiskLevel.LOW;
    }

    private boolean isGccCountry(String nationality) {
        if (nationality == null) return false;
        var gcc = List.of("SAU", "ARE", "KWT", "BHR", "QAT", "OMN",
                "SA", "AE", "KW", "BH", "QA", "OM");
        return gcc.contains(nationality.toUpperCase());
    }
}
