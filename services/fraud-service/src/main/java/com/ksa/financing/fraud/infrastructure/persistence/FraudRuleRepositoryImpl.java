package com.ksa.financing.fraud.infrastructure.persistence;

import com.ksa.financing.fraud.domain.model.fraud.FraudBlockType;
import com.ksa.financing.fraud.domain.model.fraud.FraudDecision;
import com.ksa.financing.fraud.domain.model.rule.*;
import com.ksa.financing.fraud.domain.port.out.FraudRuleRepository;
import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FraudRuleRepositoryImpl implements FraudRuleRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public PageResponse<FraudRule> findActiveByTenant(UUID tenantId, PageQuery pageQuery) {
        String searchPattern = toLikePattern(pageQuery.search());
        String searchClause = searchPattern == null ? "" :
                " AND (LOWER(rule_id) LIKE ? OR LOWER(scenario_name) LIKE ? OR LOWER(scenario_name_ar) LIKE ? " +
                "      OR LOWER(category::text) LIKE ? OR LOWER(detection_logic) LIKE ? OR LOWER(status::text) LIKE ?)";

        String countSql = "SELECT count(*) FROM fraud_rules WHERE tenant_id = ? AND status = 'ACTIVE'" + searchClause;
        Object[] countArgs = searchPattern == null
                ? new Object[]{tenantId}
                : new Object[]{tenantId, searchPattern, searchPattern, searchPattern, searchPattern, searchPattern, searchPattern};
        long totalElements = Optional.ofNullable(jdbcTemplate.queryForObject(countSql, Long.class, countArgs)).orElse(0L);

        String sql = "SELECT * FROM fraud_rules WHERE tenant_id = ? AND status = 'ACTIVE'" + searchClause + " ORDER BY priority LIMIT ? OFFSET ?";
        Object[] queryArgs = searchPattern == null
                ? new Object[]{tenantId, pageQuery.size(), pageQuery.page() * pageQuery.size()}
                : new Object[]{tenantId, searchPattern, searchPattern, searchPattern, searchPattern, searchPattern, searchPattern,
                               pageQuery.size(), pageQuery.page() * pageQuery.size()};
        List<FraudRule> content = jdbcTemplate.query(sql, ruleMapper(), queryArgs);

        return new PageResponse<>(content, buildMetadata(pageQuery, totalElements));
    }

    @Override
    public PageResponse<FraudRule> findAllByTenant(UUID tenantId, PageQuery pageQuery) {
        String searchPattern = toLikePattern(pageQuery.search());
        String searchClause = searchPattern == null ? "" :
                " AND (LOWER(rule_id) LIKE ? OR LOWER(scenario_name) LIKE ? OR LOWER(scenario_name_ar) LIKE ? " +
                "      OR LOWER(category::text) LIKE ? OR LOWER(detection_logic) LIKE ? OR LOWER(status::text) LIKE ?)";

        String countSql = "SELECT count(*) FROM fraud_rules WHERE tenant_id = ?" + searchClause;
        Object[] countArgs = searchPattern == null
                ? new Object[]{tenantId}
                : new Object[]{tenantId, searchPattern, searchPattern, searchPattern, searchPattern, searchPattern, searchPattern};
        long totalElements = Optional.ofNullable(jdbcTemplate.queryForObject(countSql, Long.class, countArgs)).orElse(0L);

        String sql = "SELECT * FROM fraud_rules WHERE tenant_id = ?" + searchClause + " ORDER BY priority LIMIT ? OFFSET ?";
        Object[] queryArgs = searchPattern == null
                ? new Object[]{tenantId, pageQuery.size(), pageQuery.page() * pageQuery.size()}
                : new Object[]{tenantId, searchPattern, searchPattern, searchPattern, searchPattern, searchPattern, searchPattern,
                               pageQuery.size(), pageQuery.page() * pageQuery.size()};
        List<FraudRule> content = jdbcTemplate.query(sql, ruleMapper(), queryArgs);

        return new PageResponse<>(content, buildMetadata(pageQuery, totalElements));
    }

    private String toLikePattern(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return "%" + raw.trim().toLowerCase() + "%";
    }

    private PageMetadata buildMetadata(PageQuery query, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / query.size());
        return new PageMetadata(
                query.page(),
                query.size(),
                totalElements,
                totalPages,
                query.page() == 0,
                query.page() >= totalPages - 1,
                totalElements == 0
        );
    }

    @Override
    public Optional<FraudRule> findByTenantAndRuleId(UUID tenantId, FraudRuleId ruleId) {
        var results = jdbcTemplate.query(
                "SELECT * FROM fraud_rules WHERE tenant_id = ? AND rule_id = ?",
                ruleMapper(), tenantId, ruleId.name());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public FraudRule save(FraudRule rule) {
        var id = rule.id() != null ? rule.id() : UUID.randomUUID();
        var paramsJson = serializeParameters(rule.parameters());
        jdbcTemplate.update("""
            INSERT INTO fraud_rules (id, tenant_id, rule_id, scenario_name, scenario_name_ar,
                category, detection_logic, default_action, block_type, block_code_id, block_code, 
                status, parameters, priority, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?::fraud_rule_category, ?, ?::fraud_decision_type, ?::fraud_block_type,
                ?, ?, ?::fraud_rule_status, ?::jsonb, ?, ?, ?)
            ON CONFLICT (tenant_id, rule_id) DO UPDATE SET
                scenario_name = EXCLUDED.scenario_name,
                parameters = EXCLUDED.parameters,
                status = EXCLUDED.status,
                updated_at = EXCLUDED.updated_at
            """,
            id, rule.tenantId(), rule.ruleId().name(),
            rule.scenarioName(), rule.scenarioNameAr(),
            rule.category().name(), rule.detectionLogic(),
            rule.defaultAction().name(),
            rule.blockType() != null ? rule.blockType().name() : null,
            rule.blockCodeId(), rule.blockCode(),
            rule.status().name(), paramsJson, rule.priority(),
            Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
        return rule;
    }

    @Override
    public void updateStatus(UUID tenantId, FraudRuleId ruleId, String status) {
        jdbcTemplate.update(
                "UPDATE fraud_rules SET status = ?::fraud_rule_status, updated_at = ? WHERE tenant_id = ? AND rule_id = ?",
                status, Timestamp.from(Instant.now()), tenantId, ruleId.name());
    }

    @Override
    public void updateParameters(UUID tenantId, FraudRuleId ruleId, String parametersJson) {
        jdbcTemplate.update(
                "UPDATE fraud_rules SET parameters = ?::jsonb, updated_at = ? WHERE tenant_id = ? AND rule_id = ?",
                parametersJson, Timestamp.from(Instant.now()), tenantId, ruleId.name());
    }

    private RowMapper<FraudRule> ruleMapper() {
        return (rs, rowNum) -> {
            var params = deserializeParameters(rs.getString("parameters"));
            return new FraudRule(
                    rs.getObject("id", UUID.class),
                    rs.getObject("tenant_id", UUID.class),
                    parseEnum(FraudRuleId.class, rs.getString("rule_id")),
                    rs.getString("scenario_name"),
                    rs.getString("scenario_name_ar"),
                    parseEnum(FraudRuleCategory.class, rs.getString("category")),
                    rs.getString("detection_logic"),
                    parseEnum(FraudDecision.class, rs.getString("default_action")),
                    parseEnum(FraudBlockType.class, rs.getString("block_type")),
                    rs.getObject("block_code_id", UUID.class),
                    rs.getString("block_code"),
                    parseEnum(FraudRuleStatus.class, rs.getString("status")),
                    params,
                    rs.getInt("priority"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime()
            );
        };
    }

    private List<RuleParameter> deserializeParameters(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize rule parameters: {}", e.getMessage());
            return List.of();
        }
    }

    private String serializeParameters(List<RuleParameter> params) {
        if (params == null || params.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.warn("Failed to serialize rule parameters: {}", e.getMessage());
            return "[]";
        }
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> clazz, String value) {
        if (value == null) return null;
        try { return Enum.valueOf(clazz, value); } catch (IllegalArgumentException e) { return null; }
    }
}
