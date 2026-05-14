package com.ksa.islamic.orchestration.activity.kyc;

import java.util.Map;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for Nafath digital identity verification initiation.
 *
 * Initiates the Nafath authentication flow via the KYC Adapter. This activity only
 * STARTS the verification process -- it returns a sessionId and a random number that
 * the user must confirm on their Nafath mobile app. The actual verification result
 * arrives asynchronously via a signal callback to the workflow.
 */
@ActivityInterface
public interface NafathVerificationActivity {

    @ActivityMethod
    NafathInitiationResult initiateNafath(NafathInitiationInput input);

    record NafathInitiationInput(
        String nationalId,
        String tenantId,
        String customerId,
        String applicationId,
        String contextType
    ) {
        /** Backwards-compatible constructor: defaults context to ONBOARDING. */
        public NafathInitiationInput(String nationalId, String tenantId) {
            this(nationalId, tenantId, null, null, "ONBOARDING");
        }
    }

    record NafathInitiationResult(
        String sessionId,
        String transactionId,
        int randomNumber,
        boolean initiated,
        Map<String, Object> nafathVerificationData
    ) {}
}
