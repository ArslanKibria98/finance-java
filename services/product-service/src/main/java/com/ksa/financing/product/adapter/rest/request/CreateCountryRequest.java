package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCountryRequest(
    @NotBlank @Size(min = 2, max = 3) String code,
    @NotBlank @Size(max = 100) String nameEn,
    @Size(max = 100) String nameAr,
    @Size(max = 10) String dialCode,
    @Size(min = 3, max = 3) String currencyCode,
    boolean gcc,
    int sortOrder
) {}
