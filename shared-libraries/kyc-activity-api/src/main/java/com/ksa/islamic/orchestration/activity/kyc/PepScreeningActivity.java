package com.ksa.islamic.orchestration.activity.kyc;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for PEP (Politically Exposed Person) screening.
 *
 * Performs enhanced screening with confidence-based match handling:
 * - Confidence > 95%: BLOCK (hard block)
 * - Confidence 70-95%: HOLD (manual review required)
 * - Confidence < 70%: FLAG (approve with monitoring)
 * - PEP detected with confidence > 60%: EDD_REQUIRED (Enhanced Due Diligence form)
 * - No match: CLEAR
 */
@ActivityInterface
public interface PepScreeningActivity {

    @ActivityMethod
    PepScreeningResult screenPep(PepScreeningInput input);

    record PepScreeningInput(
        String fullName,
        String nationalId,
        String nationality,
        String dateOfBirth,
        String tenantId
    ) {}

    record PepScreeningResult(
        String decision,        // CLEAR, FLAG, HOLD, BLOCK, EDD_REQUIRED
        boolean pepDetected,
        double confidenceScore, // 0.0 - 1.0
        int matchCount,
        String matchDetails     // JSON string with match info
    ) {}
}
