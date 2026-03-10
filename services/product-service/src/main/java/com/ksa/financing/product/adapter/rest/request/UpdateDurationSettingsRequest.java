package com.ksa.financing.product.adapter.rest.request;

public record UpdateDurationSettingsRequest(
    int requestDurationDays,
    int approvalDurationDays,
    int disbursementDurationDays,
    int repaymentDurationDays
) {}
