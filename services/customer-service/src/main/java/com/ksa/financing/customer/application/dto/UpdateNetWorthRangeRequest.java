package com.ksa.financing.customer.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateNetWorthRangeRequest(
    @Size(max = 255) String nameEn,
    @Size(max = 255) String nameAr,
    String descriptionEn,
    String descriptionAr,
    BigDecimal minValue,
    BigDecimal maxValue,
    Boolean isActive,
    Integer displayOrder
) {}
