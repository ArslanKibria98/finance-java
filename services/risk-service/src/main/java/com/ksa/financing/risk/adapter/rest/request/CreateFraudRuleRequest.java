package com.ksa.financing.risk.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFraudRuleRequest(
    @NotBlank @Size(max = 20) String ruleId,
    @NotBlank @Size(max = 200) String scenarioName,
    @Size(max = 200) String scenarioNameAr,
    @NotBlank String category,
    String detectionLogic,
    @NotBlank String defaultAction,
    String blockType,
    String status,
    String parameters,
    int priority
) {}
