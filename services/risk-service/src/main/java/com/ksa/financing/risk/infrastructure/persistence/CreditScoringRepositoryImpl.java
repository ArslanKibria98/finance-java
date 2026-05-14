package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SortDirection;
import com.ksa.financing.infra.pagination.SortField;
import com.ksa.financing.risk.domain.model.credit.*;
import com.ksa.financing.risk.domain.port.out.CreditScoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CreditScoringRepositoryImpl implements CreditScoringRepository {

    private static final Set<String> SORT_ALLOWED_COLUMNS = Set.of(
            "sort_order", "field_key", "name_en", "name_ar", "data_type", "is_active");

    private static final Map<String, String> SORT_FIELD_TO_COLUMN = Map.of(
            "fieldKey", "field_key",
            "nameEn", "name_en",
            "nameAr", "name_ar",
            "dataType", "data_type",
            "sortOrder", "sort_order",
            "active", "is_active",
            "isActive", "is_active");

    private final JdbcTemplate jdbcTemplate;

    @Override
    public PageResponse<CreditScoringFieldDefinition> findActiveFieldDefinitions(String tenantId, PageQuery pageQuery) {
        String pat = toLikePattern(pageQuery.search());
        String searchClause = pat == null ? ""
                : " AND (LOWER(field_key) LIKE ? OR LOWER(name_en) LIKE ? OR LOWER(name_ar) LIKE ? OR LOWER(data_type) LIKE ?)";

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credit_scoring_field_definitions WHERE tenant_id = ?::uuid AND is_active = true" + searchClause,
                Long.class,
                pat == null ? new Object[]{tenantId} : new Object[]{tenantId, pat, pat, pat, pat});
        long totalElements = total == null ? 0L : total;

        int page = pageQuery.page();
        int size = pageQuery.size();

        if (totalElements == 0) {
            return PageResponse.empty(page, size);
        }

        int offset = page * size;
        String orderBy = buildOrderByClause(pageQuery);

        Object[] qArgs = pat == null
                ? new Object[]{tenantId, size, offset}
                : new Object[]{tenantId, pat, pat, pat, pat, size, offset};
        List<CreditScoringFieldDefinition> content = jdbcTemplate.query(
                "SELECT id, tenant_id, field_key, name_en, name_ar, data_type, is_active, sort_order "
                        + "FROM credit_scoring_field_definitions "
                        + "WHERE tenant_id = ?::uuid AND is_active = true" + searchClause + " "
                        + orderBy + " LIMIT ? OFFSET ?",
                fieldDefinitionRowMapper(),
                qArgs
        );

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        var metadata = new PageMetadata(
                page, size, totalElements, totalPages,
                page == 0, page >= totalPages - 1, content.isEmpty());
        return new PageResponse<>(content, metadata);
    }

    private static String toLikePattern(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return "%" + raw.trim().toLowerCase() + "%";
    }

    private String buildOrderByClause(PageQuery pageQuery) {
        if (pageQuery.sort() == null || pageQuery.sort().isEmpty()) {
            return "ORDER BY sort_order";
        }
        List<String> clauses = new ArrayList<>();
        for (SortField sf : pageQuery.sort()) {
            String col = SORT_FIELD_TO_COLUMN.getOrDefault(sf.field(), sf.field());
            if (!SORT_ALLOWED_COLUMNS.contains(col)) continue;
            String dir = sf.direction() == SortDirection.ASC ? "ASC" : "DESC";
            clauses.add(col + " " + dir);
        }
        return clauses.isEmpty() ? "ORDER BY sort_order" : "ORDER BY " + String.join(", ", clauses);
    }

    @Override
    public List<CreditScoringFieldDefinition> findAllFieldDefinitions(UUID tenantId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, field_key, name_en, name_ar, data_type, is_active, sort_order
                FROM credit_scoring_field_definitions
                WHERE tenant_id = ?
                ORDER BY sort_order
                """,
                fieldDefinitionRowMapper(),
                tenantId
        );
    }

    @Override
    public Optional<CreditScoringFieldDefinition> findFieldDefinitionById(UUID tenantId, UUID id) {
        var results = jdbcTemplate.query(
                """
                SELECT id, tenant_id, field_key, name_en, name_ar, data_type, is_active, sort_order
                FROM credit_scoring_field_definitions
                WHERE tenant_id = ? AND id = ?
                """,
                fieldDefinitionRowMapper(),
                tenantId, id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public CreditScoringFieldDefinition saveFieldDefinition(CreditScoringFieldDefinition fieldDefinition) {
        var id = UUID.randomUUID();
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        jdbcTemplate.update(
                """
                INSERT INTO credit_scoring_field_definitions
                    (id, tenant_id, field_key, name_en, name_ar, data_type, is_active, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, fieldDefinition.tenantId(), fieldDefinition.fieldKey(),
                fieldDefinition.nameEn(), fieldDefinition.nameAr(),
                fieldDefinition.dataType(), fieldDefinition.active(),
                fieldDefinition.sortOrder(), now, now
        );

        return new CreditScoringFieldDefinition(
                id, fieldDefinition.tenantId(), fieldDefinition.fieldKey(),
                fieldDefinition.nameEn(), fieldDefinition.nameAr(),
                fieldDefinition.dataType(), fieldDefinition.active(),
                fieldDefinition.sortOrder()
        );
    }

    @Override
    public CreditScoringFieldDefinition updateFieldDefinition(CreditScoringFieldDefinition fieldDefinition) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        jdbcTemplate.update(
                """
                UPDATE credit_scoring_field_definitions
                SET field_key = ?, name_en = ?, name_ar = ?, data_type = ?,
                    is_active = ?, sort_order = ?, updated_at = ?
                WHERE tenant_id = ? AND id = ?
                """,
                fieldDefinition.fieldKey(), fieldDefinition.nameEn(), fieldDefinition.nameAr(),
                fieldDefinition.dataType(), fieldDefinition.active(), fieldDefinition.sortOrder(),
                now, fieldDefinition.tenantId(), fieldDefinition.id()
        );

        return fieldDefinition;
    }

    @Override
    public void deleteFieldDefinition(UUID tenantId, UUID id) {
        jdbcTemplate.update(
                """
                DELETE FROM credit_scoring_field_definitions
                WHERE tenant_id = ? AND id = ?
                """,
                tenantId, id
        );
    }

    @Override
    public boolean fieldKeyExists(UUID tenantId, String fieldKey, UUID excludeId) {
        String sql;
        Object[] params;

        if (excludeId != null) {
            sql = """
                SELECT COUNT(*) FROM credit_scoring_field_definitions
                WHERE tenant_id = ? AND field_key = ? AND id != ?
                """;
            params = new Object[]{tenantId, fieldKey, excludeId};
        } else {
            sql = """
                SELECT COUNT(*) FROM credit_scoring_field_definitions
                WHERE tenant_id = ? AND field_key = ?
                """;
            params = new Object[]{tenantId, fieldKey};
        }

        var count = jdbcTemplate.queryForObject(sql, Integer.class, params);
        return count != null && count > 0;
    }

    private org.springframework.jdbc.core.RowMapper<CreditScoringFieldDefinition> fieldDefinitionRowMapper() {
        return (rs, rowNum) -> new CreditScoringFieldDefinition(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("tenant_id")),
                rs.getString("field_key"),
                rs.getString("name_en"),
                rs.getString("name_ar"),
                rs.getString("data_type"),
                rs.getBoolean("is_active"),
                rs.getInt("sort_order")
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

    @Override
    public List<CreditScoringFieldDefinition> findFieldDefinitionsByProductId(UUID tenantId, UUID productId) {
        return jdbcTemplate.query(
                """
                SELECT DISTINCT fd.id, fd.tenant_id, fd.field_key, fd.name_en, fd.name_ar,
                       fd.data_type, fd.is_active, fd.sort_order
                FROM credit_scoring_field_definitions fd
                JOIN product_credit_scoring_criteria c ON c.field_definition_id = fd.id
                WHERE c.tenant_id = ? AND c.product_id = ? AND c.is_enabled = true
                ORDER BY c.sort_order, fd.sort_order
                """,
                fieldDefinitionRowMapper(),
                tenantId, productId
        );
    }
}
