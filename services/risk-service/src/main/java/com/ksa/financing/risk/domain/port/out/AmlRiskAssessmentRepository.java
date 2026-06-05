package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.aml.AmlRiskLevel;
import com.ksa.financing.risk.domain.model.aml.AmlRiskScore;
import com.ksa.financing.risk.domain.model.aml.AmlScoringInput;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for persisting AML risk assessment results.
 */
public interface AmlRiskAssessmentRepository {

    void save(AmlRiskScore score, AmlScoringInput input);

    Optional<AmlRiskScore> findByIdempotencyKey(String tenantId, String idempotencyKey);

    Optional<AmlRiskScore> findLatestByNationalIdHash(String tenantId, String nationalIdHash);

    List<AmlRiskScore> findAllByCustomerId(String customerId);
}
