package com.ksa.financing.product.adapter.rest.response;

import java.time.Instant;
import java.util.UUID;

public record PartnerResponse(
    UUID id,
    UUID tenantId,
    String partnerCode,
    String nameEn,
    String nameAr,
    String email,
    String phone,
    String contactPerson,
    String logoUrl,
    String status,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy,
    int version
) {}
