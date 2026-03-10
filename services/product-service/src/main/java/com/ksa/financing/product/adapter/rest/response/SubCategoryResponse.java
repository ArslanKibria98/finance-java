package com.ksa.financing.product.adapter.rest.response;

import java.util.UUID;

public record SubCategoryResponse(
    UUID id,
    UUID masterCategoryId,
    String code,
    String nameEn,
    String nameAr,
    int sortOrder
) {}
