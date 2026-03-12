package com.ksa.financing.product.adapter.rest.response;

import java.util.UUID;

public record CountryResponse(
    UUID id,
    String code,
    String alpha3Code,
    String nameEn,
    String nameAr
) {}
