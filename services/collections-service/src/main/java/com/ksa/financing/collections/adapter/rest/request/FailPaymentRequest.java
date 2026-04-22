package com.ksa.financing.collections.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;

public record FailPaymentRequest(
        @NotBlank String failureCode,
        @NotBlank String failureMessage
) {}
