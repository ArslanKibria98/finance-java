package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.aml.*;
import com.ksa.financing.risk.domain.port.out.AmlReferenceDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class AmlReferenceDataRepositoryImpl implements AmlReferenceDataRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<AmlRiskCategory> findActiveCategories(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, category_code, name_en, name_ar, category_type, weight, sort_order, is_active
                FROM aml_risk_categories
                WHERE tenant_id = ?::uuid AND is_active = true
                ORDER BY sort_order
                """,
                (rs, rowNum) -> new AmlRiskCategory(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("category_code"),
                        rs.getString("name_en"),
                        rs.getString("name_ar"),
                        AmlCategoryType.valueOf(rs.getString("category_type")),
                        rs.getBigDecimal("weight"),
                        rs.getInt("sort_order"),
                        rs.getBoolean("is_active")
                ),
                tenantId
        );
    }

    @Override
    public List<AmlRiskCategoryFactor> findActiveFactorsByCategoryId(UUID categoryId) {
        return jdbcTemplate.query(
                """
                SELECT id, category_id, factor_code, name_en, name_ar,
                       factor_weight_pct, computed_rating, sort_order, is_active
                FROM aml_risk_category_factors
                WHERE category_id = ? AND is_active = true
                ORDER BY sort_order
                """,
                (rs, rowNum) -> new AmlRiskCategoryFactor(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("category_id")),
                        rs.getString("factor_code"),
                        rs.getString("name_en"),
                        rs.getString("name_ar"),
                        rs.getBigDecimal("factor_weight_pct"),
                        rs.getBigDecimal("computed_rating"),
                        rs.getInt("sort_order"),
                        rs.getBoolean("is_active")
                ),
                categoryId
        );
    }

    @Override
    public List<AmlRiskCategoryFactor> findActiveFactorsByCategoryCodes(String tenantId, List<String> categoryCodes) {
        if (categoryCodes.isEmpty()) return List.of();

        String placeholders = String.join(",", categoryCodes.stream().map(c -> "?").toList());
        Object[] params = new Object[categoryCodes.size() + 1];
        params[0] = tenantId;
        for (int i = 0; i < categoryCodes.size(); i++) {
            params[i + 1] = categoryCodes.get(i);
        }

        return jdbcTemplate.query(
                String.format("""
                SELECT f.id, f.category_id, f.factor_code, f.name_en, f.name_ar,
                       f.factor_weight_pct, f.computed_rating, f.sort_order, f.is_active
                FROM aml_risk_category_factors f
                JOIN aml_risk_categories c ON c.id = f.category_id
                WHERE c.tenant_id = ?::uuid AND c.category_code IN (%s)
                  AND f.is_active = true
                ORDER BY f.sort_order
                """, placeholders),
                (rs, rowNum) -> new AmlRiskCategoryFactor(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("category_id")),
                        rs.getString("factor_code"),
                        rs.getString("name_en"),
                        rs.getString("name_ar"),
                        rs.getBigDecimal("factor_weight_pct"),
                        rs.getBigDecimal("computed_rating"),
                        rs.getInt("sort_order"),
                        rs.getBoolean("is_active")
                ),
                params
        );
    }

    @Override
    public List<AmlRiskThreshold> findActiveThresholds(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, risk_level, min_score, max_score, description
                FROM aml_risk_thresholds
                WHERE tenant_id = ?::uuid AND is_active = true
                ORDER BY min_score
                """,
                (rs, rowNum) -> new AmlRiskThreshold(
                        UUID.fromString(rs.getString("id")),
                        AmlRiskLevel.valueOf(rs.getString("risk_level")),
                        rs.getBigDecimal("min_score"),
                        rs.getBigDecimal("max_score"),
                        rs.getString("description")
                ),
                tenantId
        );
    }

    @Override
    public Optional<FatfCategory> findFatfCategoryByCountryCode(String tenantId, String countryCode) {
        if (countryCode == null || countryCode.isBlank()) return Optional.empty();

        var results = jdbcTemplate.query(
                """
                SELECT fatf_category FROM aml_fatf_countries
                WHERE tenant_id = ?::uuid AND country_code = ? AND is_active = true
                LIMIT 1
                """,
                (rs, rowNum) -> FatfCategory.valueOf(rs.getString("fatf_category")),
                tenantId, countryCode.toUpperCase()
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public Optional<AmlRiskLevel> findCityRiskLevel(String tenantId, String cityName) {
        if (cityName == null || cityName.isBlank()) return Optional.empty();

        var results = jdbcTemplate.query(
                """
                SELECT risk_level FROM aml_city_risk_scores
                WHERE tenant_id = ?::uuid AND is_active = true
                  AND (LOWER(name_en) = LOWER(?) OR name_ar = ?)
                LIMIT 1
                """,
                (rs, rowNum) -> AmlRiskLevel.valueOf(rs.getString("risk_level")),
                tenantId, cityName, cityName
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public Optional<OccupationRiskLevel> findOccupationRiskLevel(String tenantId, String occupationCode) {
        if (occupationCode == null || occupationCode.isBlank()) return Optional.empty();

        var results = jdbcTemplate.query(
                """
                SELECT risk_level FROM aml_occupation_risk_levels
                WHERE tenant_id = ?::uuid AND occupation_code = ? AND is_active = true
                LIMIT 1
                """,
                (rs, rowNum) -> OccupationRiskLevel.valueOf(rs.getString("risk_level")),
                tenantId, occupationCode
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public Optional<String> findIncomeRangeFactorCode(String tenantId, BigDecimal monthlyIncome) {
        if (monthlyIncome == null) return Optional.empty();

        var results = jdbcTemplate.query(
                """
                SELECT cf.factor_code
                FROM aml_income_range_scores irs
                JOIN aml_risk_category_factors cf ON cf.tenant_id = irs.tenant_id
                  AND cf.sort_order = irs.sort_order
                JOIN aml_risk_categories c ON c.id = cf.category_id AND c.category_code = 'INCOME_RANGE'
                WHERE irs.tenant_id = ?::uuid AND irs.is_active = true
                  AND ? >= irs.range_min
                  AND (irs.range_max IS NULL OR ? <= irs.range_max)
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getString("factor_code"),
                tenantId, monthlyIncome, monthlyIncome
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }
}
