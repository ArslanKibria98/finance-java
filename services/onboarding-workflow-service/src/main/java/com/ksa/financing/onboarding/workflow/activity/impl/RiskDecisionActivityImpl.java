package com.ksa.financing.onboarding.workflow.activity.impl;

import com.ksa.islamic.orchestration.activity.risk.RiskDecisionActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Risk decision activity implementation for the onboarding workflow.
 *
 * <p>This activity evaluates the overall risk of an onboarding applicant based on
 * PEP screening results, sanctions clearance, and EDD status. It produces a risk
 * score (0-100) and maps it to a decision (APPROVE, APPROVE_FLAG, HOLD, BLOCK).</p>
 *
 * <p>This activity is NOT a Spring bean. It is instantiated manually and registered
 * with the Temporal Worker on the RISK_ASSESSMENT_QUEUE.</p>
 *
 * <p>Scoring model:</p>
 * <ul>
 *   <li>PEP detected: +30 base (reduced to +15 if EDD submitted)</li>
 *   <li>PEP confidence > 0.8: +20</li>
 *   <li>PEP confidence > 0.5: +10</li>
 *   <li>Sanctions not cleared: +50</li>
 *   <li>No EDD when required (PEP detected): +10</li>
 * </ul>
 *
 * <p>Risk levels: LOW (0-25) → APPROVE, MEDIUM (26-50) → APPROVE_FLAG,
 * HIGH (51-75) → HOLD, CRITICAL (76-100) → BLOCK</p>
 */
public class RiskDecisionActivityImpl implements RiskDecisionActivity {

    private static final Logger log = LoggerFactory.getLogger(RiskDecisionActivityImpl.class);

    @Override
    public RiskDecisionResult evaluateRisk(RiskDecisionInput input) {
        log.info("Evaluating risk for customer={}, nationalId={}, pepDecision={}, sanctionsCleared={}, eddStatus={}",
                input.customerId(), input.nationalId(), input.pepDecision(),
                input.sanctionsCleared(), input.eddStatus());

        int score = 0;
        StringBuilder reason = new StringBuilder();

        // Sanctions check — highest weight
        if (!input.sanctionsCleared()) {
            score += 50;
            reason.append("Sanctions not cleared. ");
        }

        // PEP scoring
        boolean isPep = "EDD_REQUIRED".equals(input.pepDecision())
                || "FLAG".equals(input.pepDecision())
                || "HOLD".equals(input.pepDecision());

        if (isPep) {
            boolean eddSubmitted = "SUBMITTED".equals(input.eddStatus())
                    || "APPROVED".equals(input.eddStatus());

            if (eddSubmitted) {
                // EDD mitigates PEP risk
                score += 15;
                reason.append("PEP detected, EDD submitted (mitigated). ");
            } else {
                score += 30;
                reason.append("PEP detected, no EDD submitted. ");
            }

            // Confidence-based scoring
            if (input.pepConfidence() > 0.8) {
                score += 20;
                reason.append("High PEP confidence (").append(input.pepConfidence()).append("). ");
            } else if (input.pepConfidence() > 0.5) {
                score += 10;
                reason.append("Moderate PEP confidence (").append(input.pepConfidence()).append("). ");
            }
        } else if ("CLEAR".equals(input.pepDecision())) {
            // No PEP — low base risk
            score += 10;
            reason.append("PEP clear. ");
        }

        // Cap score at 100
        score = Math.min(score, 100);

        // Map score to risk level and decision
        String riskLevel;
        String decision;

        if (score <= 25) {
            riskLevel = "LOW";
            decision = "APPROVE";
        } else if (score <= 50) {
            riskLevel = "MEDIUM";
            decision = "APPROVE_FLAG";
        } else if (score <= 75) {
            riskLevel = "HIGH";
            decision = "HOLD";
        } else {
            riskLevel = "CRITICAL";
            decision = "BLOCK";
        }

        log.info("Risk decision for customer={}: score={}, level={}, decision={}, reason={}",
                input.customerId(), score, riskLevel, decision, reason);

        return new RiskDecisionResult(riskLevel, score, decision, reason.toString().trim());
    }
}
