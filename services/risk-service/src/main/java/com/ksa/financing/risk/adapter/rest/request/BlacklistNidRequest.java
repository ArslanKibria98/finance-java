package com.ksa.financing.risk.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;

public record BlacklistNidRequest(
    @NotBlank(message = "National ID is required")
    String nationalId,

    String reason
) {}
