package com.ksa.islamic.orchestration.activity.risk;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;

/**
 * Check 10: Preliminary Internal Risk Score.
 *
 * Composite score from all Phase 1 checks. Each check contributes a risk delta:
 * (a) Watchlist match = +40 points
 * (b) Velocity anomaly = +25 points
 * (c) New device + new NID + new mobile = +15 points (normal for new customer)
 * (d) Device associated with 2 NIDs = +30 points
 *
 * Thresholds: 0-30 = LOW, 31-60 = MEDIUM, 61-80 = HIGH, 81-100 = CRITICAL.
 *
 * LOW/MEDIUM = proceed to Phase 2.
 * HIGH = proceed with ENHANCED_MONITORING flag.
 * CRITICAL = block + compliance case opened.
 */
@ActivityInterface
public interface RiskScoreActivity {

    @ActivityMethod(name = "RiskScoreCalculation")
    RiskScoreResult calculateScore(RiskScoreInput input);

    record RiskScoreInput(
        List<CheckStepResult> previousCheckResults,
        boolean watchlistMatch,
        boolean velocityAnomaly,
        boolean newDevice,
        boolean newNid,
        boolean newMobile,
        int deviceNidCount
    ) {}

    record RiskScoreResult(
        int totalScore,
        RiskLevel riskLevel,
        CheckDecision decision,
        List<String> contributingFactors,
        String failureReason
    ) {}
}
