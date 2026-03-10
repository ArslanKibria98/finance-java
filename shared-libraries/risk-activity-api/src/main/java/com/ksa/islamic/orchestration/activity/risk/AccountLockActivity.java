package com.ksa.islamic.orchestration.activity.risk;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Check 8: Account Lock History.
 *
 * Queries account_locks table for NID hash:
 * (a) Was a previous account locked?
 * (b) Why? (fraud, compliance, user-request, delinquency)
 * (c) Is the lock still active?
 * (d) Was the lock lifted with conditions?
 *
 * Active compliance/fraud lock = hard block.
 * Conditional lock = route to re-onboarding flow.
 * No lock or user-requested and lifted = pass.
 */
@ActivityInterface
public interface AccountLockActivity {

    @ActivityMethod(name = "AccountLockCheck")
    AccountLockResult check(AccountLockInput input);

    record AccountLockInput(
        String nidHash
    ) {}

    record AccountLockResult(
        CheckDecision decision,
        boolean hasActiveLock,
        String lockType,
        String lockReason,
        String failureReason
    ) {}
}
