package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request payload for PUT /api/v1/products/{id}/settings/duration-settings.
 * All fields are optional — null is treated as 0 (no wait / immediate).
 * Upper bounds guard against accidental huge values that would stall workflows.
 */
public record UpdateDurationSettingsRequest(

    @Min(value = 0, message = "Request duration days must be >= 0")
    @Max(value = 3650, message = "Request duration days must be <= 3650")
    Integer requestDurationDays,

    @Min(value = 0, message = "Approval duration days must be >= 0")
    @Max(value = 3650, message = "Approval duration days must be <= 3650")
    Integer approvalDurationDays,

    @Min(value = 0, message = "Disbursement duration hours must be >= 0")
    @Max(value = 8760, message = "Disbursement duration hours must be <= 8760 (1 year)")
    Integer disbursementDurationHours,

    @Min(value = 0, message = "Repayment duration days must be >= 0")
    @Max(value = 36500, message = "Repayment duration days must be <= 36500")
    Integer repaymentDurationDays
) {
    /** Returns 0 instead of null for any missing field. */
    public int requestDurationDaysOrZero()       { return requestDurationDays       != null ? requestDurationDays       : 0; }
    public int approvalDurationDaysOrZero()      { return approvalDurationDays      != null ? approvalDurationDays      : 0; }
    public int disbursementDurationHoursOrZero() { return disbursementDurationHours != null ? disbursementDurationHours : 0; }
    public int repaymentDurationDaysOrZero()     { return repaymentDurationDays     != null ? repaymentDurationDays     : 0; }
}
