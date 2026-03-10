package com.ksa.islamic.orchestration.activity.risk;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Check 1: NID Format Validation.
 *
 * Validates that the National ID is exactly 10 digits, starts with 1 (Saudi citizen)
 * or 2 (resident/Iqama), and passes Luhn checksum validation.
 */
@ActivityInterface
public interface NidFormatValidationActivity {

    @ActivityMethod(name = "NidFormatValidation")
    NidValidationResult validate(NidValidationInput input);

    record NidValidationInput(
        String nationalId
    ) {}

    record NidValidationResult(
        boolean valid,
        String idType,
        String failureReason
    ) {}
}
