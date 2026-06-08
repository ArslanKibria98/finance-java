package com.ksa.financing.onboarding.shared.sullis;

import java.io.Serializable;

/**
 * Result of the Sullis selfie phase (upload-selfie → submit verification pipeline).
 *
 * <p>The submit step returns a scorecard + an overall {@code outcome}
 * (APPROVED / DECLINED). {@code faceMatchScore} is normalised to a 0..1 range
 * (Sullis reports 0..100) so it slots into the workflow's existing face-match
 * threshold check the same way the Facia similarity score did.</p>
 *
 * @param approved       {@code true} when {@code outcome == "APPROVED"}
 * @param outcome        raw Sullis outcome (APPROVED / DECLINED / ...)
 * @param faceMatchScore face-match score normalised to 0..1 (null if absent)
 * @param riskScore      Sullis risk score (0..100, null if absent)
 * @param faceSamePerson Sullis same-person flag
 * @param reason         decline reason when not approved
 * @param sessionId      Sullis session id (for reference / audit)
 * @param attemptId      Sullis attempt id — on a DECLINE this is the NEW attempt started
 *                       against the session so the workflow re-uploads the next selfie to it
 * @param maxAttemptsReached {@code true} when a declined selfie tried to start a new attempt
 *                       but the session's shared 3-attempt budget (document + selfie) is used up
 */
public record SullisVerificationResult(
        boolean approved,
        String outcome,
        Double faceMatchScore,
        Integer riskScore,
        Boolean faceSamePerson,
        String reason,
        String sessionId,
        String attemptId,
        boolean maxAttemptsReached
) implements Serializable {
}
