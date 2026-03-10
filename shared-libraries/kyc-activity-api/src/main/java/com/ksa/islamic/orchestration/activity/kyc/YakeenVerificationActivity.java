package com.ksa.islamic.orchestration.activity.kyc;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for Yakeen identity verification (NIC/NICA integration).
 *
 * Verifies the customer's identity and retrieves demographic data from the
 * Yakeen system via the KYC Adapter. Returns full name (Arabic/English),
 * gender, nationality, and address details that will be used to create
 * the customer profile in subsequent workflow steps.
 */
@ActivityInterface
public interface YakeenVerificationActivity {

    @ActivityMethod
    YakeenVerificationResult verifyIdentity(YakeenVerificationInput input);

    record YakeenVerificationInput(
        String nationalId,
        String dateOfBirth,
        String tenantId
    ) {}

    record YakeenVerificationResult(
        boolean verified,
        String fullNameAr,
        String fullNameEn,
        String gender,
        String nationality,
        String addressCity,
        String addressRegion
    ) {}
}
