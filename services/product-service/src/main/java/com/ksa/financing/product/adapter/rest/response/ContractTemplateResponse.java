package com.ksa.financing.product.adapter.rest.response;

import java.time.Instant;
import java.util.UUID;

public record ContractTemplateResponse(
    UUID id,
    String nameEn,
    String nameAr,
    UUID productId,
    String productNameEn,
    String productNameAr,
    UUID typeId,
    String typeNameEn,
    String typeNameAr,
    String language,
    String message,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
