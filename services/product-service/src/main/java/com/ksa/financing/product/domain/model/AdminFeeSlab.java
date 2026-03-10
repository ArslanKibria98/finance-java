package com.ksa.financing.product.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain value object representing an admin fee slab for a product.
 * Pure domain — zero framework imports.
 */
public record AdminFeeSlab(
    UUID id,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    BigDecimal profitPercentage,
    BigDecimal processingFee,
    BigDecimal adminFee,
    String partnerScope,
    String status,
    int sortOrder,
    Integer minTenure,
    Integer maxTenure
) {}
