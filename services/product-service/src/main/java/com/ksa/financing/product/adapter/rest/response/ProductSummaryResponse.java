package com.ksa.financing.product.adapter.rest.response;

import java.time.Instant;
import java.util.UUID;

public record ProductSummaryResponse(
    UUID id,
    String productCode,
    String nameEn,
    String nameAr,
    String productType,
    String status,
    UUID masterCategoryId,
    String masterCategoryNameEn,
    String masterCategoryNameAr,
    UUID subCategoryId,
    String subCategoryNameEn,
    String subCategoryNameAr,
    String notificationEmail,
    String countryNameEn,
    String countryNameAr,
    int wizardStep,
    boolean wizardCompleted,
    Instant createdAt
) {}
