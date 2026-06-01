package com.ksa.financing.risk.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.risk.domain.model.credit.CreditScoringCriteria;
import com.ksa.financing.risk.domain.model.credit.CreditScoringOperator;
import com.ksa.financing.risk.domain.model.credit.CreditScoringRule;
import com.ksa.financing.risk.domain.model.credit.CriteriaEvaluationDetail;
import com.ksa.financing.risk.domain.model.credit.CustomerCreditScoreCurrent;
import com.ksa.financing.risk.domain.model.credit.CustomerCreditScoreSnapshot;
import com.ksa.financing.risk.domain.model.credit.GeneralScoringConfig;
import com.ksa.financing.risk.domain.port.out.GeneralCreditScoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class GeneralCreditScoringRepositoryImpl implements GeneralCreditScoringRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // ==================================================================
    // CRITERIA + RULES
    // ==================================================================

    @Override
    public List<CreditScoringCriteria> findAllCriteria(UUID tenantId) {
        var criteriaList = jdbcTemplate.query(
                """
                SELECT id, tenant_id, field_definition_id, custom_name,
                       is_custom, is_enabled, sort_order
                FROM general_credit_scoring_criteria
                WHERE tenant_id = ?
                ORDER BY sort_order
                """,
                (rs, rowNum) -> {
                    var criteriaId = UUID.fromString(rs.getString("id"));
                    var fieldDefId = rs.getString("field_definition_id");
                    return new CreditScoringCriteria(
                            criteriaId,
                            UUID.fromString(rs.getString("tenant_id")),
                            null,                                              // productId is NULL for general scoring
                            fieldDefId != null ? UUID.fromString(fieldDefId) : null,
                            rs.getString("custom_name"),
                            rs.getBoolean("is_custom"),
                            rs.getBoolean("is_enabled"),
                            rs.getInt("sort_order"),
                            findRulesByCriteriaId(criteriaId)
                    );
                },
                tenantId
        );
        log.debug("Found {} general criteria for tenant={}", criteriaList.size(), tenantId);
        return criteriaList;
    }

    private List<CreditScoringRule> findRulesByCriteriaId(UUID criteriaId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, criteria_id, operator, value, weight, percentage
                FROM general_credit_scoring_rules
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
    public void saveCriteria(UUID tenantId, List<CreditScoringCriteria> criteria) {
        // Replace-all strategy: delete existing then insert new (rules cascade)
        deleteAllCriteria(tenantId);

        var now = OffsetDateTime.now(ZoneOffset.UTC);

        for (var criterion : criteria) {
            var criteriaId = UUID.randomUUID();

            jdbcTemplate.update(
                    """
                    INSERT INTO general_credit_scoring_criteria
                        (id, tenant_id, field_definition_id, custom_name,
                         is_custom, is_enabled, sort_order, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    criteriaId, tenantId,
                    criterion.fieldDefinitionId(),
                    criterion.customName(),
                    criterion.custom(),
                    criterion.enabled(),
                    criterion.sortOrder(),
                    now, now
            );

            if (criterion.rules() != null) {
                for (var rule : criterion.rules()) {
                    jdbcTemplate.update(
                            """
                            INSERT INTO general_credit_scoring_rules
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
        }
        log.debug("Saved {} general criteria for tenant={}", criteria.size(), tenantId);
    }

    @Override
    public void deleteAllCriteria(UUID tenantId) {
        int deleted = jdbcTemplate.update(
                "DELETE FROM general_credit_scoring_criteria WHERE tenant_id = ?",
                tenantId
        );
        log.debug("Deleted {} existing general criteria for tenant={}", deleted, tenantId);
    }

    @Override
    public Optional<CreditScoringCriteria> findCriteriaById(UUID tenantId, UUID id) {
        var list = jdbcTemplate.query(
                """
                SELECT id, tenant_id, field_definition_id, custom_name,
                       is_custom, is_enabled, sort_order
                FROM general_credit_scoring_criteria
                WHERE tenant_id = ? AND id = ?
                """,
                (rs, rowNum) -> {
                    var critId = UUID.fromString(rs.getString("id"));
                    var fieldDefId = rs.getString("field_definition_id");
                    return new CreditScoringCriteria(
                            critId,
                            UUID.fromString(rs.getString("tenant_id")),
                            null,
                            fieldDefId != null ? UUID.fromString(fieldDefId) : null,
                            rs.getString("custom_name"),
                            rs.getBoolean("is_custom"),
                            rs.getBoolean("is_enabled"),
                            rs.getInt("sort_order"),
                            findRulesByCriteriaId(critId)
                    );
                },
                tenantId, id
        );
        return list.stream().findFirst();
    }

    @Override
    public CreditScoringCriteria saveSingleCriteria(UUID tenantId, CreditScoringCriteria criterion) {
        var id = UUID.randomUUID();
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        jdbcTemplate.update(
                """
                INSERT INTO general_credit_scoring_criteria
                    (id, tenant_id, field_definition_id, custom_name,
                     is_custom, is_enabled, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, tenantId,
                criterion.fieldDefinitionId(),
                criterion.customName(),
                criterion.custom(),
                criterion.enabled(),
                criterion.sortOrder(),
                now, now
        );
        insertRules(tenantId, id, criterion.rules(), now);
        log.debug("Inserted single general criterion id={} for tenant={}", id, tenantId);
        return findCriteriaById(tenantId, id).orElseThrow();
    }

    @Override
    public CreditScoringCriteria updateSingleCriteria(UUID tenantId, UUID id, CreditScoringCriteria criterion) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        int updated = jdbcTemplate.update(
                """
                UPDATE general_credit_scoring_criteria
                SET field_definition_id = ?, custom_name = ?, is_custom = ?,
                    is_enabled = ?, sort_order = ?, updated_at = ?
                WHERE tenant_id = ? AND id = ?
                """,
                criterion.fieldDefinitionId(),
                criterion.customName(),
                criterion.custom(),
                criterion.enabled(),
                criterion.sortOrder(),
                now,
                tenantId, id
        );
        if (updated == 0) {
            throw new IllegalArgumentException("General criterion not found: " + id);
        }

        // Replace rules (cascade-safe — delete then insert)
        jdbcTemplate.update("DELETE FROM general_credit_scoring_rules WHERE criteria_id = ?", id);
        insertRules(tenantId, id, criterion.rules(), now);
        log.debug("Updated general criterion id={} for tenant={}", id, tenantId);
        return findCriteriaById(tenantId, id).orElseThrow();
    }

    @Override
    public void deleteSingleCriteria(UUID tenantId, UUID id) {
        int deleted = jdbcTemplate.update(
                "DELETE FROM general_credit_scoring_criteria WHERE tenant_id = ? AND id = ?",
                tenantId, id
        );
        if (deleted == 0) {
            throw new IllegalArgumentException("General criterion not found: " + id);
        }
        log.debug("Deleted general criterion id={} for tenant={}", id, tenantId);
    }

    private void insertRules(UUID tenantId, UUID criteriaId,
                             List<CreditScoringRule> rules, OffsetDateTime ts) {
        if (rules == null) return;
        for (var rule : rules) {
            jdbcTemplate.update(
                    """
                    INSERT INTO general_credit_scoring_rules
                        (id, tenant_id, criteria_id, operator, value, weight, percentage,
                         created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    UUID.randomUUID(), tenantId, criteriaId,
                    rule.operator().name(),
                    rule.value(),
                    rule.weight(),
                    rule.percentage(),
                    ts, ts
            );
        }
    }

    // ==================================================================
    // CONFIG
    // ==================================================================

    @Override
    public Optional<GeneralScoringConfig> findConfig(UUID tenantId) {
        var list = jdbcTemplate.query(
                """
                SELECT id, tenant_id, min_pass_percentage, green_threshold, amber_threshold, is_enabled
                FROM general_credit_scoring_config
                WHERE tenant_id = ?
                """,
                (rs, rowNum) -> new GeneralScoringConfig(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("tenant_id")),
                        rs.getBigDecimal("min_pass_percentage"),
                        rs.getBigDecimal("green_threshold"),
                        rs.getBigDecimal("amber_threshold"),
                        rs.getBoolean("is_enabled")
                ),
                tenantId
        );
        return list.stream().findFirst();
    }

    @Override
    public GeneralScoringConfig saveConfig(GeneralScoringConfig config) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var existing = findConfig(config.tenantId());

        if (existing.isPresent()) {
            jdbcTemplate.update(
                    """
                    UPDATE general_credit_scoring_config
                    SET min_pass_percentage = ?, green_threshold = ?, amber_threshold = ?,
                        is_enabled = ?, updated_at = ?
                    WHERE tenant_id = ?
                    """,
                    config.minPassPercentage(), config.greenThreshold(), config.amberThreshold(),
                    config.enabled(), now, config.tenantId()
            );
            return findConfig(config.tenantId()).orElseThrow();
        }

        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO general_credit_scoring_config
                    (id, tenant_id, min_pass_percentage, green_threshold, amber_threshold,
                     is_enabled, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, config.tenantId(), config.minPassPercentage(),
                config.greenThreshold(), config.amberThreshold(), config.enabled(),
                now, now
        );
        return new GeneralScoringConfig(id, config.tenantId(), config.minPassPercentage(),
                config.greenThreshold(), config.amberThreshold(), config.enabled());
    }

    // ==================================================================
    // SNAPSHOTS + CURRENT
    // ==================================================================

    @Override
    public CustomerCreditScoreSnapshot saveSnapshot(CustomerCreditScoreSnapshot s) {
        UUID id = s.id() != null ? s.id() : UUID.randomUUID();
        var now = s.createdAt() != null ? s.createdAt() : OffsetDateTime.now(ZoneOffset.UTC);

        jdbcTemplate.update(
                """
                INSERT INTO customer_credit_score_snapshots
                    (id, tenant_id, customer_id, workflow_id, snapshot_stage,
                     score_percentage, total_score, max_possible_score,
                     decision, reason_code, matched_criteria, total_criteria,
                     inputs, breakdown, summary, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
                """,
                id, s.tenantId(), s.customerId(), s.workflowId(), s.snapshotStage(),
                s.scorePercentage(), s.totalScore(), s.maxPossibleScore(),
                s.decision(), s.reasonCode(), s.matchedCriteria(), s.totalCriteria(),
                toJson(s.inputs()), toJson(s.breakdown()),
                s.summary(), now
        );

        return new CustomerCreditScoreSnapshot(
                id, s.tenantId(), s.customerId(), s.workflowId(), s.snapshotStage(),
                s.scorePercentage(), s.totalScore(), s.maxPossibleScore(),
                s.decision(), s.reasonCode(), s.matchedCriteria(), s.totalCriteria(),
                s.inputs(), s.breakdown(), s.summary(), now
        );
    }

    @Override
    public List<CustomerCreditScoreSnapshot> findSnapshotsByCustomer(UUID tenantId, UUID customerId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, customer_id, workflow_id, snapshot_stage,
                       score_percentage, total_score, max_possible_score,
                       decision, reason_code, matched_criteria, total_criteria,
                       inputs::text AS inputs_text, breakdown::text AS breakdown_text,
                       summary, created_at
                FROM customer_credit_score_snapshots
                WHERE tenant_id = ? AND customer_id = ?
                ORDER BY created_at DESC
                """,
                (rs, rowNum) -> new CustomerCreditScoreSnapshot(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("tenant_id")),
                        UUID.fromString(rs.getString("customer_id")),
                        rs.getString("workflow_id"),
                        rs.getString("snapshot_stage"),
                        rs.getBigDecimal("score_percentage"),
                        rs.getBigDecimal("total_score"),
                        rs.getBigDecimal("max_possible_score"),
                        rs.getString("decision"),
                        rs.getString("reason_code"),
                        rs.getInt("matched_criteria"),
                        rs.getInt("total_criteria"),
                        fromJsonMap(rs.getString("inputs_text")),
                        fromJsonList(rs.getString("breakdown_text")),
                        rs.getString("summary"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                tenantId, customerId
        );
    }

    @Override
    public Optional<CustomerCreditScoreCurrent> findCurrentByCustomer(UUID tenantId, UUID customerId) {
        var list = jdbcTemplate.query(
                """
                SELECT customer_id, tenant_id, score_percentage, total_score, max_possible_score,
                       decision, reason_code, snapshot_stage, last_snapshot_id, updated_at
                FROM customer_credit_score_current
                WHERE tenant_id = ? AND customer_id = ?
                """,
                (rs, rowNum) -> new CustomerCreditScoreCurrent(
                        UUID.fromString(rs.getString("customer_id")),
                        UUID.fromString(rs.getString("tenant_id")),
                        rs.getBigDecimal("score_percentage"),
                        rs.getBigDecimal("total_score"),
                        rs.getBigDecimal("max_possible_score"),
                        rs.getString("decision"),
                        rs.getString("reason_code"),
                        rs.getString("snapshot_stage"),
                        rs.getString("last_snapshot_id") != null
                                ? UUID.fromString(rs.getString("last_snapshot_id")) : null,
                        rs.getObject("updated_at", OffsetDateTime.class)
                ),
                tenantId, customerId
        );
        return list.stream().findFirst();
    }

    @Override
    public CustomerCreditScoreCurrent upsertCurrent(CustomerCreditScoreCurrent c) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                INSERT INTO customer_credit_score_current
                    (customer_id, tenant_id, score_percentage, total_score, max_possible_score,
                     decision, reason_code, snapshot_stage, last_snapshot_id, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (customer_id) DO UPDATE SET
                    tenant_id = EXCLUDED.tenant_id,
                    score_percentage = EXCLUDED.score_percentage,
                    total_score = EXCLUDED.total_score,
                    max_possible_score = EXCLUDED.max_possible_score,
                    decision = EXCLUDED.decision,
                    reason_code = EXCLUDED.reason_code,
                    snapshot_stage = EXCLUDED.snapshot_stage,
                    last_snapshot_id = EXCLUDED.last_snapshot_id,
                    updated_at = EXCLUDED.updated_at
                """,
                c.customerId(), c.tenantId(), c.scorePercentage(), c.totalScore(),
                c.maxPossibleScore(), c.decision(), c.reasonCode(), c.snapshotStage(),
                c.lastSnapshotId(), now
        );
        return new CustomerCreditScoreCurrent(
                c.customerId(), c.tenantId(), c.scorePercentage(), c.totalScore(),
                c.maxPossibleScore(), c.decision(), c.reasonCode(), c.snapshotStage(),
                c.lastSnapshotId(), now
        );
    }

    // ==================================================================
    // JSON helpers
    // ==================================================================

    private String toJson(Object value) {
        if (value == null) return "null";
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize value to JSON", e);
            return "null";
        }
    }

    private Map<String, String> fromJsonMap(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize JSON map", e);
            return new HashMap<>();
        }
    }

    private List<CriteriaEvaluationDetail> fromJsonList(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<CriteriaEvaluationDetail>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize JSON breakdown list", e);
            return List.of();
        }
    }
}
