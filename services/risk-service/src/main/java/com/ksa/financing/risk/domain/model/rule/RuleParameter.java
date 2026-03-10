package com.ksa.financing.risk.domain.model.rule;

public record RuleParameter(
    String key,
    String value,
    String dataType,
    String description,
    String descriptionAr
) {}
