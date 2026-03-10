package com.ksa.islamic.orchestration.activity.kyc;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for mobile number verification via Tahakuk (SAMA mobile registry).
 *
 * Verifies that the provided mobile number is registered to the given national ID
 * through the KYC Adapter's Tahakuk integration. This is the first step in the
 * customer onboarding workflow and acts as a basic identity confirmation.
 */
@ActivityInterface
public interface MobileVerificationActivity {

    @ActivityMethod
    MobileVerificationResult verifyMobile(MobileVerificationInput input);

    record MobileVerificationInput(
        String nationalId,
        String mobileNumber,
        String tenantId
    ) {}

    record MobileVerificationResult(
        boolean verified,
        String sessionId,
        String carrier
    ) {}
}
