package com.ksa.islamic.orchestration.activity.customer;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for updating an existing customer profile with additional information.
 *
 * After the customer submits additional details (employment, banking, contact info) during
 * the onboarding flow, this activity orchestrates up to 3 operations:
 * <ol>
 *   <li>Update customer -- email, city, region</li>
 *   <li>Add employment details (if provided)</li>
 *   <li>Add bank account (if provided)</li>
 * </ol>
 *
 * The activity succeeds if at least the customer update succeeds. Employment and bank account
 * creation failures are logged as warnings but do not fail the activity.
 */
@ActivityInterface
public interface UpdateCustomerActivity {

    @ActivityMethod
    UpdateCustomerResult updateWithAdditionalInfo(UpdateCustomerInput input);

    @ActivityMethod
    UpdateKycStatusResult updateKycStatus(UpdateKycStatusInput input);

    @ActivityMethod
    UpdateRiskGradeResult updateRiskGrade(UpdateRiskGradeInput input);

    @ActivityMethod
    SubmitPepFromOnboardingResult submitPepFromOnboarding(SubmitPepFromOnboardingInput input);

    record UpdateRiskGradeInput(
        String customerId,
        int riskScore,
        String tenantId
    ) {}

    record UpdateRiskGradeResult(
        boolean updated,
        String riskGrade
    ) {}

    record SubmitPepFromOnboardingInput(
        String customerId,
        String tenantId,
        boolean isPep,
        String sourceOfFunds,
        String estimatedNetWorth,
        String sourceOfIncome
    ) {}

    record SubmitPepFromOnboardingResult(
        boolean submitted
    ) {}

    record UpdateKycStatusInput(
        String customerId,
        String kycStatus,
        String tenantId
    ) {}

    record UpdateKycStatusResult(
        boolean updated
    ) {}

    record UpdateCustomerInput(
        String customerId,
        String email,
        String city,
        String region,
        String employerName,
        String employerCrNumber,
        String employmentType,
        String jobTitle,
        Double basicSalary,
        Double housingAllowance,
        Double grossSalary,
        Double netSalary,
        String currency,
        String bankName,
        String bankCode,
        String iban,
        String accountHolderName,
        String tenantId
    ) {}

    record UpdateCustomerResult(
        boolean updated
    ) {}
}
