package com.ksa.financing.risk.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record BlacklistNidRequest(
    @NotBlank(message = "National ID is required")
    String nationalId,

    String reason,

    UUID blockCodeId
) {}
