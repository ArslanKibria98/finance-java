package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;

public record CreateMasterCategoryRequest(
    @NotBlank String code,
    @NotBlank String nameEn,
    String nameAr,
    String descriptionEn,
    String descriptionAr,
    String iconUrl,
    int sortOrder
) {}
