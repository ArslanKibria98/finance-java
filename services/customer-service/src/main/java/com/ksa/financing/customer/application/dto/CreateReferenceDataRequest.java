package com.ksa.financing.customer.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReferenceDataRequest(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 255) String nameEn,
    @NotBlank @Size(max = 255) String nameAr,
    String descriptionEn,
    String descriptionAr,
    int displayOrder
) {}
