package com.ksa.financing.product.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only domain representation of a product document.
 */
public record ProductDocument(
    UUID id,
    String nameEn,
    String nameAr,
    String documentType,
    String fileUrl,
    Long fileSizeBytes,
    String fileVersion,
    String createdByName,
    String status,
    boolean required,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt
) {}
