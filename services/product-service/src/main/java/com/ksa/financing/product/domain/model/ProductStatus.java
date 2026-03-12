package com.ksa.financing.product.domain.model;

/**
 * Product lifecycle status — pure domain enum, no framework imports.
 */
public enum ProductStatus {
    DRAFT,
    PENDING_ACTIVATION,
    ACTIVE,
    INACTIVE,
    ACTIVATION_FAILED,
    ARCHIVED
}
