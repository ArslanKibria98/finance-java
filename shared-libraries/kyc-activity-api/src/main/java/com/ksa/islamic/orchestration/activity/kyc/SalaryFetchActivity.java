package com.ksa.islamic.orchestration.activity.kyc;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for fetching salary information from GOSI (General Organization
 * for Social Insurance).
 *
 * Retrieves employment and salary details via the KYC Adapter's GOSI integration.
 * This step is CONDITIONAL -- it only runs if employment/salary information is
 * required for the product being applied for. On failure, the workflow continues
 * as this is a non-critical enrichment step.
 */
@ActivityInterface
public interface SalaryFetchActivity {

    @ActivityMethod
    SalaryFetchResult fetchSalary(SalaryFetchInput input);

    record SalaryFetchInput(
        String nationalId,
        String employerCrNumber,
        String tenantId
    ) {}

    record SalaryFetchResult(
        boolean fetched,
        String employerName,
        double basicSalary,
        double grossSalary,
        double netSalary,
        String currency
    ) {}
}
