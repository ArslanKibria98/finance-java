package com.ksa.financing.risk.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "Request to evaluate customer eligibility against product scoring rules")
public record EvaluateEligibilityRequest(

        @NotNull(message = "Eligibility answers are required")
        @Schema(description = "Customer answers: field_key → value (e.g., salary → 15000, age → 30)")
        Map<String, String> answers
) {}
