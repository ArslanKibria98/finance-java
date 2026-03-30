package com.ksa.financing.product.adapter.rest.response;

import java.time.Instant;
import java.util.UUID;

public record ProductSummaryResponse(
    UUID id,
    String productCode,
    String nameEn,
    String nameAr,
    String shortDescriptionEn,
    String shortDescriptionAr,
    String productType,
    String targetSegment,
    String shariaStructure,
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
    // Financing limits
    java.math.BigDecimal minAmount,
    java.math.BigDecimal maxAmount,
    int minTenureMonths,
    int maxTenureMonths,
    java.util.List<Integer> allowedTenures,
    java.math.BigDecimal baseProfitRate,
    String rateType,
    String repaymentFrequency,
    String currency,
    String fineractProductId,
    boolean visibleToCustomers,
    boolean visibleToPartners,
    Instant createdAt
) {}
