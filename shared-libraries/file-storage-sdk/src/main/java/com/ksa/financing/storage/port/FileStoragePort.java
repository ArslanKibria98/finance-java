package com.ksa.financing.storage.port;

import com.ksa.financing.storage.model.FileUploadRequest;
import com.ksa.financing.storage.model.FileUploadResult;

import java.io.InputStream;
import java.time.Duration;

/**
 * Output port for file storage operations.
 * Implemented by MinioFileStorageAdapter (infrastructure layer).
 * Any service can inject this port to store/retrieve files without knowing the backend.
 */
public interface FileStoragePort {

    /**
     * Upload a file to storage.
     * @return result containing objectKey (for DB) and publicUrl (for response)
     */
    FileUploadResult upload(FileUploadRequest request);

    /**
     * Get the public URL for a stored file.
     * @param objectKey stored key (from FileUploadResult.objectKey)
     */
    String getPublicUrl(String objectKey);

    /**
     * Get a pre-signed (temporary, expiring) URL for secure private file access.
     * @param objectKey stored key
     * @param expiry    how long the URL should be valid
     */
    String getPresignedUrl(String objectKey, Duration expiry);

    /**
     * Stream a file's content directly.
     * @param objectKey stored key
     */
    InputStream download(String objectKey);

    /**
     * Delete a file from storage.
     * @param objectKey stored key
     */
    void delete(String objectKey);

    /**
     * Check if a file exists in storage.
     * @param objectKey stored key
     */
    boolean exists(String objectKey);
}
