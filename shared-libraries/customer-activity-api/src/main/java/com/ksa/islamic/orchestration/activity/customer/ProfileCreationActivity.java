package com.ksa.islamic.orchestration.activity.customer;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for customer profile creation.
 *
 * This is the most critical activity in the onboarding workflow. It calls the
 * Customer Service to create the full customer record, which internally:
 * - Creates the Global Profile (hash-based, zero-PII)
 * - Stores encrypted PII in the PII Vault
 * - Generates a CIF (Customer Information File) number
 * - Returns the customerId, globalUid, and cifNumber for downstream steps
 */
@ActivityInterface
public interface ProfileCreationActivity {

    @ActivityMethod
    ProfileCreationResult createProfile(ProfileCreationInput input);

    record ProfileCreationInput(
        String nationalId,
        String mobileNumber,
        String email,
        String dateOfBirth,
        String fullNameAr,
        String fullNameEn,
        String gender,
        String nationality,
        String addressCity,
        String addressRegion,
        String tenantId,
        String globalUid,
        String customerId,
        String lifecycleStage
    ) {}

    record ProfileCreationResult(
        String customerId,
        String globalUid,
        String cifNumber,
        boolean created
    ) {}
}
