package com.ksa.financing.product.adapter.rest.response;

import java.time.Instant;
import java.util.UUID;

public record TemplateTypeResponse(
    UUID id,
    String nameEn,
    String nameAr,
    String category,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
