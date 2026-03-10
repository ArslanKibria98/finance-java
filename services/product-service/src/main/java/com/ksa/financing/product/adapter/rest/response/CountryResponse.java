package com.ksa.financing.product.adapter.rest.response;

import java.util.UUID;

public record CountryResponse(
    UUID id,
    String code,
    String nameEn,
    String nameAr,
    String dialCode,
    String currencyCode,
    boolean gcc,
    boolean active,
    int sortOrder
) {}
