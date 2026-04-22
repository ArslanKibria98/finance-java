package com.ksa.financing.storage.adapter;

import com.ksa.financing.storage.config.FileStorageProperties;
import com.ksa.financing.storage.model.FileUploadRequest;
import com.ksa.financing.storage.model.FileUploadResult;
import com.ksa.financing.storage.port.FileStoragePort;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO implementation of FileStoragePort.
 * S3-compatible — switching to AWS S3 only requires changing config, not code.
 *
 * Two clients:
 *   internalClient  → upload/download/delete (uses internal Docker endpoint: minio:9000)
 *   presignedClient → presigned URL generation (uses public URL: 46.62.226.94:9100)
 *                     so generated URLs are directly accessible from outside
 *
 * Object key pattern: tenants/{tenantId}/{category}/{ownerId}/{uuid}.{ext}
 */
@Slf4j
public class MinioFileStorageAdapter implements FileStoragePort {

    private final MinioClient internalClient;   // for actual operations
    private final MinioClient presignedClient;  // for presigned URL generation (public URL)
    private final FileStorageProperties properties;

    public MinioFileStorageAdapter(MinioClient internalClient, FileStorageProperties properties) {
        this.internalClient = internalClient;
        this.properties = properties;
        // Presigned client uses public URL so generated URLs work from outside Docker
        this.presignedClient = MinioClient.builder()
                .endpoint(properties.getPublicUrl())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
        log.info("MinioFileStorageAdapter initialized: internal={} presigned={}",
                properties.getEndpoint(), properties.getPublicUrl());
    }

    @Override
    public FileUploadResult upload(FileUploadRequest request) {
        ensureBucketExists();

        String objectKey = buildObjectKey(request);

        try {
            internalClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .stream(request.content(), request.sizeBytes(), -1)
                            .contentType(request.contentType())
                            .build()
            );

            String publicUrl = buildPublicUrl(objectKey);
            log.info("File uploaded to MinIO: bucket={} key={} size={}",
                    properties.getBucket(), objectKey, request.sizeBytes());

            return new FileUploadResult(objectKey, publicUrl, request.sizeBytes(), request.contentType());

        } catch (Exception e) {
            log.error("Failed to upload file to MinIO: key={} error={}", objectKey, e.getMessage());
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPublicUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return null;
        return buildPublicUrl(objectKey);
    }

    @Override
    public String getPresignedUrl(String objectKey, Duration expiry) {
        try {
            // Uses presignedClient (public URL endpoint) so the generated URL
            // is directly accessible from outside Docker network
            return presignedClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .expiry((int) expiry.getSeconds(), TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: key={} error={}", objectKey, e.getMessage());
            throw new RuntimeException("Presigned URL generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            return internalClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download file from MinIO: key={} error={}", objectKey, e.getMessage());
            throw new RuntimeException("File download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            internalClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
            log.info("File deleted from MinIO: key={}", objectKey);
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: key={} error={}", objectKey, e.getMessage());
            throw new RuntimeException("File deletion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            internalClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) return false;
            throw new RuntimeException("File existence check failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("File existence check failed: " + e.getMessage(), e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Build object key: tenants/{tenantId}/{category}/{ownerId}/{uuid}.{ext}
     */
    private String buildObjectKey(FileUploadRequest request) {
        String ext = extractExtension(request.originalFilename(), request.contentType());
        String uniqueId = UUID.randomUUID().toString();
        return String.format("tenants/%s/%s/%s/%s.%s",
                request.tenantId(),
                request.category().getPath(),
                request.ownerId(),
                uniqueId,
                ext);
    }

    private String buildPublicUrl(String objectKey) {
        return properties.getPublicUrl() + "/" + properties.getBucket() + "/" + objectKey;
    }

    private String extractExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
        if (contentType != null) {
            return switch (contentType) {
                case "image/png"  -> "png";
                case "image/gif"  -> "gif";
                case "image/webp" -> "webp";
                case "application/pdf" -> "pdf";
                default -> "jpg";
            };
        }
        return "bin";
    }

    private void ensureBucketExists() {
        try {
            boolean exists = internalClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucket()).build());
            if (!exists) {
                internalClient.makeBucket(
                        MakeBucketArgs.builder().bucket(properties.getBucket()).build());
                log.info("Created MinIO bucket: {} (private)", properties.getBucket());
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket exists: {}", e.getMessage());
            throw new RuntimeException("MinIO bucket setup failed: " + e.getMessage(), e);
        }
    }

}
