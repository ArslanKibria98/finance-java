package com.ksa.financing.storage.model;

/**
 * Result of a successful file upload.
 *
 * objectKey  → save this in DB (e.g. "tenants/abc/profile-pictures/cust-123/photo.jpg")
 * publicUrl  → full URL to access the file (generated at runtime, NOT stored in DB)
 */
public record FileUploadResult(
        String objectKey,
        String publicUrl,
        long sizeBytes,
        String contentType
) {}
