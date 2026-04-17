package com.ksa.financing.customer.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateNetWorthRangeRequest(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 255) String nameEn,
    @NotBlank @Size(max = 255) String nameAr,
    String descriptionEn,
    String descriptionAr,
    BigDecimal minValue,
    BigDecimal maxValue,
    int displayOrder
) {}
