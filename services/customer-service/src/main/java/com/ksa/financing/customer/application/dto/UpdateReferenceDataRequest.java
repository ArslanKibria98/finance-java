package com.ksa.financing.customer.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateReferenceDataRequest(
    @Size(max = 255) String nameEn,
    @Size(max = 255) String nameAr,
    String descriptionEn,
    String descriptionAr,
    Boolean isActive,
    Integer displayOrder
) {}
