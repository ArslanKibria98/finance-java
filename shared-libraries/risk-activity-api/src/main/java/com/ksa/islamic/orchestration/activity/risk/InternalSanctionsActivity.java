package com.ksa.islamic.orchestration.activity.risk;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Check 9: Internal Sanctions List.
 *
 * Checks NID against platform's internal copy of local sanctions/terror lists
 * (updated daily via batch sync). This is a preliminary check before the full
 * external screening in Phase 2.
 *
 * Match = hard block + silent rejection + immediate compliance team notification
 * + SAR preparation triggered. Mandatory retention: 10 years.
 */
@ActivityInterface
public interface InternalSanctionsActivity {

    @ActivityMethod(name = "InternalSanctionsCheck")
    InternalSanctionsResult check(InternalSanctionsInput input);

    record InternalSanctionsInput(
        String nidHash
    ) {}

    record InternalSanctionsResult(
        CheckDecision decision,
        boolean sanctionsMatch,
        String matchConfidence,
        String failureReason
    ) {}
}
