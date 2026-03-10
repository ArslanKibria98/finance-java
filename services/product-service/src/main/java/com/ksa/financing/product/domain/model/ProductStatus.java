package com.ksa.financing.product.domain.model;

/**
 * Product lifecycle status — pure domain enum, no framework imports.
 */
public enum ProductStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
    ARCHIVED
}
