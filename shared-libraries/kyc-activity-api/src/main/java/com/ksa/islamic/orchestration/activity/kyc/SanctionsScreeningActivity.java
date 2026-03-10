package com.ksa.islamic.orchestration.activity.kyc;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for sanctions and watchlist screening.
 *
 * Performs AML/CFT screening against sanctions lists via the KYC Adapter.
 * If the screening returns a HIT, the customer is NOT cleared and the
 * onboarding workflow will be terminated with a compliance rejection.
 * A CLEAR result allows the workflow to proceed to profile creation.
 */
@ActivityInterface
public interface SanctionsScreeningActivity {

    @ActivityMethod
    SanctionsScreeningResult screenSanctions(SanctionsScreeningInput input);

    record SanctionsScreeningInput(
        String fullName,
        String nationalId,
        String nationality,
        String tenantId
    ) {}

    record SanctionsScreeningResult(
        String result,
        int matchCount,
        boolean cleared
    ) {}
}
