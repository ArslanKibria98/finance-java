package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePartnerRequest(
    @NotBlank(message = "Partner code is required")
    String partnerCode,

    @NotBlank(message = "Partner name (English) is required")
    String nameEn,

    String nameAr,
    String email,
    String phone,
    String contactPerson,
    String logoUrl
) {}
