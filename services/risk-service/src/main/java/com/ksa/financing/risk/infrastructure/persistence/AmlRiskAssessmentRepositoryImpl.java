package com.ksa.financing.risk.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.risk.domain.model.aml.AmlRiskLevel;
import com.ksa.financing.risk.domain.model.aml.AmlRiskScore;
import com.ksa.financing.risk.domain.model.aml.AmlScoringInput;
import com.ksa.financing.risk.domain.model.aml.AmlCategoryScoreBreakdown;
import com.ksa.financing.risk.domain.port.out.AmlRiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class AmlRiskAssessmentRepositoryImpl implements AmlRiskAssessmentRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(AmlRiskScore score, AmlScoringInput input) {
        String breakdownJson;
        String inputDataJson;
        try {
            breakdownJson = objectMapper.writeValueAsString(score.breakdown());
            inputDataJson = objectMapper.writeValueAsString(Map.of(
                    "nationality", Optional.ofNullable(input.nationality()).orElse(""),
                    "cityName", Optional.ofNullable(input.cityName()).orElse(""),
                    "occupationCode", Optional.ofNullable(input.occupationCode()).orElse(""),
                    "monthlyIncome", Optional.ofNullable(input.monthlyIncome()).map(Object::toString).orElse(""),
                    "sourceOfIncome", Optional.ofNullable(input.sourceOfIncome()).orElse(""),
                    "productRiskTier", Optional.ofNullable(input.productRiskTier()).orElse(""),
                    "isPep", input.isPep(),
                    "isOnInternalList", input.isOnInternalList()
            ));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize AML score breakdown", e);
            breakdownJson = "[]";
            inputDataJson = "{}";
        }

        jdbcTemplate.update(
                """
                INSERT INTO aml_risk_assessments
                    (id, tenant_id, customer_id, national_id_hash, total_score, risk_level,
                     dominant_override, dominant_category, score_breakdown, input_data,
                     assessed_at, idempotency_key)
                VALUES (?::uuid, ?::uuid, ?, ?, ?, ?::aml_risk_level, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
                """,
                score.assessmentId().toString(),
                input.tenantId(),
                input.customerId(),
                input.nationalIdHash(),
                score.totalScore(),
                score.riskLevel().name(),
                score.dominantOverride(),
                score.dominantCategory(),
                breakdownJson,
                inputDataJson,
                Timestamp.from(score.assessedAt()),
                input.idempotencyKey()
        );

        log.debug("Saved AML risk assessment: id={}", score.assessmentId());
    }

    @Override
    public Optional<AmlRiskScore> findByIdempotencyKey(String tenantId, String idempotencyKey) {
        var results = jdbcTemplate.query(
                """
                SELECT id, total_score, risk_level, dominant_override, dominant_category,
                       score_breakdown, assessed_at
                FROM aml_risk_assessments
                WHERE tenant_id = ?::uuid AND idempotency_key = ?
                LIMIT 1
                """,
                (rs, rowNum) -> mapToAmlRiskScore(rs),
                tenantId, idempotencyKey
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public Optional<AmlRiskScore> findLatestByNationalIdHash(String tenantId, String nationalIdHash) {
        var results = jdbcTemplate.query(
                """
                SELECT id, total_score, risk_level, dominant_override, dominant_category,
                       score_breakdown, assessed_at
                FROM aml_risk_assessments
                WHERE tenant_id = ?::uuid AND national_id_hash = ?
                ORDER BY assessed_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> mapToAmlRiskScore(rs),
                tenantId, nationalIdHash
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<AmlRiskScore> findAllByCustomerId(String customerId) {
        return jdbcTemplate.query(
                """
                SELECT id, total_score, risk_level, dominant_override, dominant_category,
                       score_breakdown, assessed_at
                FROM aml_risk_assessments
                WHERE customer_id = ?
                ORDER BY assessed_at DESC
                """,
                (rs, rowNum) -> mapToAmlRiskScore(rs),
                customerId
        );
    }

    public List<java.util.Map<String, Object>> findAllWithInputDataByCustomerId(String customerId) {
        return jdbcTemplate.query(
                """
                SELECT id, total_score, risk_level, dominant_override, dominant_category,
                       score_breakdown, input_data, assessed_at
                FROM aml_risk_assessments
                WHERE customer_id = ?
                ORDER BY assessed_at DESC
                """,
                (rs, rowNum) -> {
                    AmlRiskScore score = mapToAmlRiskScore(rs);
                    java.util.Map<String, Object> inputData = new java.util.HashMap<>();
                    try {
                        String inputDataJson = rs.getString("input_data");
                        if (inputDataJson != null) {
                            inputData = objectMapper.readValue(inputDataJson,
                                    objectMapper.getTypeFactory().constructMapType(java.util.Map.class, String.class, Object.class));
                        }
                    } catch (Exception e) {
                        log.warn("Failed to deserialize input_data", e);
                    }
                    return java.util.Map.of("score", score, "inputData", inputData);
                },
                customerId
        );
    }

    private AmlRiskScore mapToAmlRiskScore(java.sql.ResultSet rs) throws java.sql.SQLException {
        List<AmlCategoryScoreBreakdown> breakdown;
        try {
            breakdown = objectMapper.readValue(
                    rs.getString("score_breakdown"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AmlCategoryScoreBreakdown.class)
            );
        } catch (Exception e) {
            log.warn("Failed to deserialize score breakdown", e);
            breakdown = List.of();
        }

        return new AmlRiskScore(
                UUID.fromString(rs.getString("id")),
                rs.getBigDecimal("total_score"),
                AmlRiskLevel.valueOf(rs.getString("risk_level")),
                rs.getBoolean("dominant_override"),
                rs.getString("dominant_category"),
                breakdown,
                rs.getTimestamp("assessed_at").toInstant()
        );
    }
}
