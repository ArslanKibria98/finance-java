package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMasterCategoryRequest(
    @NotBlank String nameEn,
    String nameAr,
    String descriptionEn,
    String descriptionAr,
    String iconUrl,
    int sortOrder,
    boolean active
) {}
