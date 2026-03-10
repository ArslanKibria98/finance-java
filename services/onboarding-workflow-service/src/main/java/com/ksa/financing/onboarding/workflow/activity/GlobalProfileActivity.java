package com.ksa.financing.onboarding.workflow.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for creating a Global Profile in GPS at onboarding initiation.
 * <p>
 * This is a local activity (runs on the onboarding-task-queue) that makes an HTTP
 * call to the Global Profile Service. It creates the real GPS record early so the
 * globalUid returned to the client from Step 1 is the genuine database-generated UUID.
 */
@ActivityInterface
public interface GlobalProfileActivity {

    @ActivityMethod
    GlobalProfileResult createGlobalProfile(GlobalProfileInput input);

    record GlobalProfileInput(
        String mobileNumber,
        String email,
        String countryCode
    ) {}

    record GlobalProfileResult(
        String globalUid,
        boolean created
    ) {}
}
