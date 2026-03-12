package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Validates customer details by calling customer-service.
 * Used in Step 1 (Basic Information) of the loan application workflow.
 */
@ActivityInterface
public interface CustomerValidationActivity {

    @ActivityMethod
    CustomerValidationResult validateCustomer(CustomerValidationInput input);

    record CustomerValidationInput(
            String tenantId,
            String customerId
    ) {}

    record CustomerValidationResult(
            boolean valid,
            String nationalId,
            String fullName,
            String mobileNumber,
            int age,
            String employmentType,
            int employmentDurationMonths,
            String employerName,
            String rejectionReason      // null if valid
    ) {}
}
