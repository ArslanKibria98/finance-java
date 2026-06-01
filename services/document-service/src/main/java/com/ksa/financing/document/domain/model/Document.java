package com.ksa.financing.document.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity describing a single stored document.
 * Blob bytes never live here — only the storage envelope + crypto material.
 */
public class Document {
    private UUID id;
    private UUID tenantId;
    private UUID customerId;
    private String workflowId;
    private DocumentKind kind;
    private DocumentSourceFlow sourceFlow;
    private String documentNumber;
    private String faciaReferenceId;
    private String objectKey;
    private String contentType;
    private long sizeBytesPlain;
    private long sizeBytesStored;
    private String sha256Plain;
    private String cipherAlgo;
    private byte[] encryptedKey;
    private byte[] iv;
    private DocumentStatus status;
    private String idempotencyKey;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    public DocumentKind getKind() { return kind; }
    public void setKind(DocumentKind kind) { this.kind = kind; }
    public DocumentSourceFlow getSourceFlow() { return sourceFlow; }
    public void setSourceFlow(DocumentSourceFlow sourceFlow) { this.sourceFlow = sourceFlow; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getFaciaReferenceId() { return faciaReferenceId; }
    public void setFaciaReferenceId(String faciaReferenceId) { this.faciaReferenceId = faciaReferenceId; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSizeBytesPlain() { return sizeBytesPlain; }
    public void setSizeBytesPlain(long sizeBytesPlain) { this.sizeBytesPlain = sizeBytesPlain; }
    public long getSizeBytesStored() { return sizeBytesStored; }
    public void setSizeBytesStored(long sizeBytesStored) { this.sizeBytesStored = sizeBytesStored; }
    public String getSha256Plain() { return sha256Plain; }
    public void setSha256Plain(String sha256Plain) { this.sha256Plain = sha256Plain; }
    public String getCipherAlgo() { return cipherAlgo; }
    public void setCipherAlgo(String cipherAlgo) { this.cipherAlgo = cipherAlgo; }
    public byte[] getEncryptedKey() { return encryptedKey; }
    public void setEncryptedKey(byte[] encryptedKey) { this.encryptedKey = encryptedKey; }
    public byte[] getIv() { return iv; }
    public void setIv(byte[] iv) { this.iv = iv; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
