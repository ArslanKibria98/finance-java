package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateApplicationStepsRequest(
    @NotEmpty @Valid List<ApplicationStepItem> steps
) {
    public record ApplicationStepItem(
        int stepNumber,
        String titleEn,
        String titleAr,
        String description,
        boolean required,
        int sortOrder
    ) {}
}
