package com.ksa.financing.storage.model;

import java.io.InputStream;
import java.util.UUID;

/**
 * Request to upload a file to MinIO.
 * tenantId   → used for path isolation (multi-tenant)
 * ownerId    → entity the file belongs to (customerId, loanId, etc.)
 * category   → determines sub-folder in MinIO
 */
public record FileUploadRequest(
        UUID tenantId,
        UUID ownerId,
        FileCategory category,
        String originalFilename,
        String contentType,
        InputStream content,
        long sizeBytes
) {}
