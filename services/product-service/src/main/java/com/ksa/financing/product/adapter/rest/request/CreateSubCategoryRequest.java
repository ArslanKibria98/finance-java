package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateSubCategoryRequest(
    @NotNull UUID masterCategoryId,
    @NotBlank String code,
    @NotBlank String nameEn,
    String nameAr,
    int sortOrder
) {}
