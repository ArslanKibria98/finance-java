package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.aml.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for AML reference data persistence.
 * Provides access to risk categories, factors, thresholds,
 * and all lookup tables (FATF countries, cities, occupations, income ranges).
 */
public interface AmlReferenceDataRepository {

    // Categories & Factors
    List<AmlRiskCategory> findActiveCategories(String tenantId);

    List<AmlRiskCategoryFactor> findActiveFactorsByCategoryId(UUID categoryId);

    List<AmlRiskCategoryFactor> findActiveFactorsByCategoryCodes(String tenantId, List<String> categoryCodes);

    // Thresholds
    List<AmlRiskThreshold> findActiveThresholds(String tenantId);

    // FATF Country lookup
    Optional<FatfCategory> findFatfCategoryByCountryCode(String tenantId, String countryCode);

    // City risk lookup
    Optional<AmlRiskLevel> findCityRiskLevel(String tenantId, String cityName);

    // Occupation risk lookup
    Optional<OccupationRiskLevel> findOccupationRiskLevel(String tenantId, String occupationCode);

    // Income range lookup
    Optional<String> findIncomeRangeFactorCode(String tenantId, BigDecimal monthlyIncome);
}
