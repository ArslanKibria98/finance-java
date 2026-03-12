package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCountryRequest(
    @Size(min = 3, max = 3) String alpha3Code,
    @Size(min = 3, max = 3) String numericCode,
    @Size(max = 100) String slug,
    @NotBlank @Size(max = 100) String nameEn,
    @Size(max = 100) String nameAr,
    @Size(max = 100) String nationalityEn,
    @Size(max = 100) String nationalityAr,
    @Size(max = 10) String dialCode,
    @Size(min = 3, max = 3) String currencyCode,
    @Size(max = 100) String currencyNameEn,
    @Size(max = 100) String currencyNameAr,
    @Size(max = 10) String flagEmoji,
    @Size(max = 100) String capitalEn,
    @Size(max = 100) String capitalAr,
    @Size(max = 50) String region,
    @Size(max = 50) String subRegion,
    boolean gcc,
    boolean arabLeague,
    boolean oicMember,
    boolean sanctioned,
    @Size(max = 20) String riskTier,
    boolean ibanRequired,
    Integer ibanLength,
    int sortOrder,
    boolean active
) {}
