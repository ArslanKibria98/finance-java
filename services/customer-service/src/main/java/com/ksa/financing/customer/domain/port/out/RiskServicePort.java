package com.ksa.financing.customer.domain.port.out;

import java.util.List;
import java.util.Map;

/**
 * Output port for fetching risk assessment data from Risk Service.
 */
public interface RiskServicePort {

    /**
     * Get all risk assessment sessions for an entity (by NID or reference).
     */
    List<Map<String, Object>> getAssessmentsByEntity(String entityReference, String accessToken);

    /**
     * Get current entity status (account status, compliance status).
     */
    Map<String, Object> getEntityStatus(String entityReference, String accessToken);

    /**
     * Get entity status history.
     */
    List<Map<String, Object>> getEntityStatusHistory(String entityReference, String accessToken);

    /**
     * Get score breakdown for a specific assessment session.
     */
    Map<String, Object> getScoreBreakdown(String sessionId, String accessToken);

    /**
     * Get answers for a specific assessment session.
     */
    List<Map<String, Object>> getAssessmentAnswers(String sessionId, String accessToken);
}
