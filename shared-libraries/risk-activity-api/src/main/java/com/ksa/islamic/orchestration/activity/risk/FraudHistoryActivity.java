package com.ksa.islamic.orchestration.activity.risk;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Check 5: Fraud History Flags.
 *
 * Queries fraud_incidents table for NID hash or mobile hash.
 * Checks: (a) Previous fraud cases (confirmed or suspected),
 * (b) Chargeback history, (c) Identity theft reports,
 * (d) Application fraud history.
 *
 * Confirmed fraud = hard block + compliance notification.
 * Suspected/under review = proceed with HIGH_RISK flag.
 */
@ActivityInterface
public interface FraudHistoryActivity {

    @ActivityMethod(name = "FraudHistoryCheck")
    FraudHistoryResult check(FraudHistoryInput input);

    record FraudHistoryInput(
        String nidHash,
        String mobileHash
    ) {}

    record FraudHistoryResult(
        CheckDecision decision,
        boolean confirmedFraud,
        boolean suspectedFraud,
        int incidentCount,
        String failureReason
    ) {}
}
