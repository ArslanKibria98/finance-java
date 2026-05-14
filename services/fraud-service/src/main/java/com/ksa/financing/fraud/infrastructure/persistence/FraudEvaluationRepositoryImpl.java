package com.ksa.financing.fraud.infrastructure.persistence;

import com.ksa.financing.fraud.domain.model.fraud.*;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.FraudEvaluationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FraudEvaluationRepositoryImpl implements FraudEvaluationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public FraudEvaluationResult save(FraudEvaluationResult result) {
        var triggeredJson = serializeTriggeredRules(result.triggeredRules());
        jdbcTemplate.update("""
            INSERT INTO fraud_evaluations (id, tenant_id, event_id, fraud_event_id, customer_id,
                decision, block_type, block_code_id, block_code, composite_risk_score, risk_level, 
                triggered_rules, block_reason, block_duration_hours, customer_message, 
                evaluation_time_ms, evaluated_at)
            VALUES (?, ?, ?, ?, ?, ?::fraud_decision_type, ?::fraud_block_type, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
            ON CONFLICT (tenant_id, event_id) DO NOTHING
            """,
            result.id(), result.tenantId(), result.eventId(), result.fraudEventId(),
            result.customerId(),
            result.decision().name(),
            result.blockType() != null ? result.blockType().name() : null,
            result.blockCodeId(), result.blockCode(),
            result.compositeRiskScore(), result.riskLevel(),
            triggeredJson,
            result.blockReason(), result.blockDurationHours(),
            result.customerMessage(), result.evaluationTimeMs(),
            Timestamp.valueOf(result.evaluatedAt()));
        return result;
    }

    @Override
    public Optional<FraudEvaluationResult> findByTenantAndEventId(UUID tenantId, String eventId) {
        var results = jdbcTemplate.query(
                "SELECT * FROM fraud_evaluations WHERE tenant_id = ? AND event_id = ?",
                evalMapper(), tenantId, eventId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    private RowMapper<FraudEvaluationResult> evalMapper() {
        return (rs, rowNum) -> new FraudEvaluationResult(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("event_id"),
                rs.getObject("fraud_event_id", UUID.class),
                rs.getString("customer_id"),
                parseEnum(FraudDecision.class, rs.getString("decision")),
                parseEnum(FraudBlockType.class, rs.getString("block_type")),
                rs.getObject("block_code_id", UUID.class),
                rs.getString("block_code"),
                rs.getInt("composite_risk_score"),
                rs.getString("risk_level"),
                deserializeTriggeredRules(rs.getString("triggered_rules")),
                rs.getString("block_reason"),
                rs.getObject("block_duration_hours", Integer.class),
                rs.getString("customer_message"),
                rs.getLong("evaluation_time_ms"),
                rs.getTimestamp("evaluated_at").toLocalDateTime()
        );
    }

    private String serializeTriggeredRules(List<RuleEvaluationResult> rules) {
        if (rules == null || rules.isEmpty()) return "[]";
        try {
            var simplified = rules.stream().map(r -> Map.of(
                    "ruleId", r.ruleId().name(),
                    "triggered", r.triggered(),
                    "decision", r.decision() != null ? r.decision().name() : "",
                    "blockType", r.blockType() != null ? r.blockType().name() : "",
                    "blockCodeId", r.blockCodeId() != null ? r.blockCodeId().toString() : "",
                    "blockCode", r.blockCode() != null ? r.blockCode() : "",
                    "detail", r.detail() != null ? r.detail() : "",
                    "scoreContribution", r.scoreContribution()
            )).toList();
            return objectMapper.writeValueAsString(simplified);
        } catch (Exception e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<RuleEvaluationResult> deserializeTriggeredRules(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) return List.of();
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            return raw.stream().map(m -> new RuleEvaluationResult(
                    parseEnum(FraudRuleId.class, (String) m.get("ruleId")),
                    Boolean.TRUE.equals(m.get("triggered")),
                    parseEnum(FraudDecision.class, (String) m.get("decision")),
                    parseEnum(FraudBlockType.class, (String) m.get("blockType")),
                    m.get("blockCodeId") != null && !((String) m.get("blockCodeId")).isBlank() 
                            ? UUID.fromString((String) m.get("blockCodeId")) : null,
                    (String) m.get("blockCode"),
                    (String) m.get("detail"),
                    m.get("scoreContribution") instanceof Number n ? n.intValue() : 0
            )).toList();
        } catch (Exception e) {
            log.warn("Failed to deserialize triggered rules: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public EvaluationStats getEvaluationStats(UUID tenantId, String customerId) {
        var results = jdbcTemplate.queryForMap("""
            SELECT
                COUNT(*) AS total_count,
                COUNT(*) FILTER (WHERE decision = 'BLOCK') AS blocked_count,
                COUNT(*) FILTER (WHERE decision = 'HOLD') AS held_count,
                COUNT(*) FILTER (WHERE decision = 'ALERT') AS alerted_count
            FROM fraud_evaluations
            WHERE tenant_id = ? AND customer_id = ?
            """, tenantId, customerId);

        return new EvaluationStats(
                ((Number) results.get("total_count")).intValue(),
                ((Number) results.get("blocked_count")).intValue(),
                ((Number) results.get("held_count")).intValue(),
                ((Number) results.get("alerted_count")).intValue()
        );
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> clazz, String value) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(clazz, value); } catch (IllegalArgumentException e) { return null; }
    }
}
