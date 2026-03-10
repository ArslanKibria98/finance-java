package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateSubCategoryRequest(
    @NotBlank String nameEn,
    String nameAr,
    int sortOrder,
    boolean active
) {}
