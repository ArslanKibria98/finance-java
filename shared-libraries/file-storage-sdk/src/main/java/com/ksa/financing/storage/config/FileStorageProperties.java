package com.ksa.financing.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for file storage (MinIO/S3).
 * All values come from environment variables — zero hardcoding.
 *
 * application.yml:
 *   minio:
 *     endpoint: ${MINIO_ENDPOINT:http://minio:9000}
 *     access-key: ${MINIO_ACCESS_KEY:minioadmin}
 *     secret-key: ${MINIO_SECRET_KEY:minioadmin}
 *     bucket: ${MINIO_BUCKET:ksa-financing-documents}
 *     public-url: ${MINIO_PUBLIC_URL:http://localhost:9000}
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class FileStorageProperties {

    /** MinIO server URL (internal Docker: http://minio:9000) */
    private String endpoint = "http://minio:9000";

    /** MinIO access key */
    private String accessKey = "minioadmin";

    /** MinIO secret key */
    private String secretKey = "minioadmin";

    /** Bucket name for this platform */
    private String bucket = "ksa-financing-documents";

    /**
     * Public-facing base URL for file access.
     * Can be Kong gateway URL or direct MinIO URL.
     * Used to generate full URLs returned to clients.
     */
    private String publicUrl = "http://localhost:9000";

    /**
     * Presigned URL expiry in hours (default 24 hours).
     * After this time the URL becomes invalid — client must request a fresh one.
     */
    private int presignedUrlExpiryHours = 24;
}
