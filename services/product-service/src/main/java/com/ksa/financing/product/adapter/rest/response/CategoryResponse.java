package com.ksa.financing.product.adapter.rest.response;

import java.util.UUID;

public record CategoryResponse(
    UUID id,
    String code,
    String nameEn,
    String nameAr,
    String descriptionEn,
    String descriptionAr,
    String iconUrl,
    int sortOrder
) {}
