package com.ksa.financing.customer.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateReferenceDataRequest(
    @Size(max = 255) String nameEn,
    @Size(max = 255) String nameAr,
    String descriptionEn,
    String descriptionAr,
    Boolean isActive,
    Integer displayOrder
) {}
