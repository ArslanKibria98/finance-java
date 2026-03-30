package com.ksa.financing.risk.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFraudRuleRequest(
    @NotBlank @Size(max = 200) String scenarioName,
    @Size(max = 200) String scenarioNameAr,
    @NotBlank String category,
    String detectionLogic,
    @NotBlank String defaultAction,
    String blockType,
    @NotBlank String status,
    String parameters,
    int priority
) {}
