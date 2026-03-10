package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.credit.*;
import com.ksa.financing.risk.domain.port.out.CreditScoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CreditScoringRepositoryImpl implements CreditScoringRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<CreditScoringFieldDefinition> findActiveFieldDefinitions(String tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, field_key, name_en, name_ar, data_type, is_active, sort_order
                FROM credit_scoring_field_definitions
                WHERE tenant_id = ?::uuid AND is_active = true
                ORDER BY sort_order
                """,
                (rs, rowNum) -> new CreditScoringFieldDefinition(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("tenant_id")),
                        rs.getString("field_key"),
                        rs.getString("name_en"),
                        rs.getString("name_ar"),
                        rs.getString("data_type"),
                        rs.getBoolean("is_active"),
                        rs.getInt("sort_order")
                ),
                tenantId
        );
    }

    @Override
    public List<CreditScoringCriteria> findCriteriaByProductId(UUID tenantId, UUID productId) {
        var criteriaList = jdbcTemplate.query(
                """
                SELECT id, tenant_id, product_id, field_definition_id, custom_name,
                       is_custom, is_enabled, sort_order
                FROM product_credit_scoring_criteria
                WHERE tenant_id = ? AND product_id = ?
                ORDER BY sort_order
                """,
                (rs, rowNum) -> {
                    var criteriaId = UUID.fromString(rs.getString("id"));
                    var fieldDefId = rs.getString("field_definition_id");
                    return new CreditScoringCriteria(
                            criteriaId,
                            UUID.fromString(rs.getString("tenant_id")),
                            UUID.fromString(rs.getString("product_id")),
                            fieldDefId != null ? UUID.fromString(fieldDefId) : null,
                            rs.getString("custom_name"),
                            rs.getBoolean("is_custom"),
                            rs.getBoolean("is_enabled"),
                            rs.getInt("sort_order"),
                            findRulesByCriteriaId(criteriaId)
                    );
                },
                tenantId, productId
        );

        log.debug("Found {} criteria for product={}", criteriaList.size(), productId);
        return criteriaList;
    }

    private List<CreditScoringRule> findRulesByCriteriaId(UUID criteriaId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, criteria_id, operator, value, weight, percentage
                FROM product_credit_scoring_rules
                WHERE criteria_id = ?
                """,
                (rs, rowNum) -> new CreditScoringRule(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("tenant_id")),
                        UUID.fromString(rs.getString("criteria_id")),
                        CreditScoringOperator.valueOf(rs.getString("operator")),
                        rs.getString("value"),
                        rs.getBigDecimal("weight"),
                        rs.getBigDecimal("percentage")
                ),
                criteriaId
        );
    }

    @Override
    public void saveCriteria(UUID tenantId, UUID productId, List<CreditScoringCriteria> criteria) {
        // Replace-all strategy: delete existing then insert new
        deleteCriteriaByProductId(tenantId, productId);

        var now = OffsetDateTime.now(ZoneOffset.UTC);

        for (var criterion : criteria) {
            var criteriaId = UUID.randomUUID();

            jdbcTemplate.update(
                    """
                    INSERT INTO product_credit_scoring_criteria
                        (id, tenant_id, product_id, field_definition_id, custom_name,
                         is_custom, is_enabled, sort_order, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    criteriaId, tenantId, productId,
                    criterion.fieldDefinitionId(),
                    criterion.customName(),
                    criterion.custom(),
                    criterion.enabled(),
                    criterion.sortOrder(),
                    now, now
            );

            for (var rule : criterion.rules()) {
                jdbcTemplate.update(
                        """
                        INSERT INTO product_credit_scoring_rules
                            (id, tenant_id, criteria_id, operator, value, weight, percentage,
                             created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        UUID.randomUUID(), tenantId, criteriaId,
                        rule.operator().name(),
                        rule.value(),
                        rule.weight(),
                        rule.percentage(),
                        now, now
                );
            }
        }

        log.debug("Saved {} criteria with rules for product={}", criteria.size(), productId);
    }

    @Override
    public void deleteCriteriaByProductId(UUID tenantId, UUID productId) {
        // Rules are deleted via ON DELETE CASCADE
        int deleted = jdbcTemplate.update(
                """
                DELETE FROM product_credit_scoring_criteria
                WHERE tenant_id = ? AND product_id = ?
                """,
                tenantId, productId
        );
        log.debug("Deleted {} existing criteria for product={}", deleted, productId);
    }
}
