package com.ksa.financing.risk.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;

public record BlacklistMobileRequest(
    @NotBlank(message = "Mobile number is required")
    String mobileNumber,

    String reason
) {}
