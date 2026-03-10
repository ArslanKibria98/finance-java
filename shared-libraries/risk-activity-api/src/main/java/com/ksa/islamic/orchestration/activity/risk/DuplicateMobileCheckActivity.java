package com.ksa.islamic.orchestration.activity.risk;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Check 3: Duplicate Mobile Number.
 *
 * Queries customers table to check if the mobile number is already registered
 * to an active account. One mobile = one active account.
 */
@ActivityInterface
public interface DuplicateMobileCheckActivity {

    @ActivityMethod(name = "DuplicateMobileCheck")
    DuplicateMobileResult check(DuplicateMobileInput input);

    record DuplicateMobileInput(
        String mobileHash,
        String sessionId
    ) {}

    record DuplicateMobileResult(
        boolean duplicate,
        String failureReason
    ) {}
}
