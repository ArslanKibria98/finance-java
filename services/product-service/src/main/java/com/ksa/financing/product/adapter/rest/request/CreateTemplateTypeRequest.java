package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTemplateTypeRequest(
    @NotBlank @Size(max = 100) String nameEn,
    @Size(max = 100) String nameAr,
    @NotBlank @Size(max = 50) String category
) {}
