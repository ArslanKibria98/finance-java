package com.ksa.financing.document.adapter.rest.response;

import com.ksa.financing.document.domain.model.Document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID documentId,
        UUID tenantId,
        UUID customerId,
        String workflowId,
        String kind,
        String sourceFlow,
        String documentNumber,
        String faciaReferenceId,
        String objectKey,
        String contentType,
        long sizeBytesPlain,
        long sizeBytesStored,
        String sha256Plain,
        String status,
        Instant createdAt,
        String base64Image
) {
    public static DocumentResponse from(Document d) {
        return new DocumentResponse(
                d.getId(), d.getTenantId(), d.getCustomerId(), d.getWorkflowId(),
                d.getKind() != null ? d.getKind().name() : null,
                d.getSourceFlow() != null ? d.getSourceFlow().name() : null,
                d.getDocumentNumber(), d.getFaciaReferenceId(),
                d.getObjectKey(), d.getContentType(),
                d.getSizeBytesPlain(), d.getSizeBytesStored(),
                d.getSha256Plain(),
                d.getStatus() != null ? d.getStatus().name() : null,
                d.getCreatedAt(),
                null);
    }

    public static DocumentResponse withImage(Document d, String base64Image) {
        return new DocumentResponse(
                d.getId(), d.getTenantId(), d.getCustomerId(), d.getWorkflowId(),
                d.getKind() != null ? d.getKind().name() : null,
                d.getSourceFlow() != null ? d.getSourceFlow().name() : null,
                d.getDocumentNumber(), d.getFaciaReferenceId(),
                d.getObjectKey(), d.getContentType(),
                d.getSizeBytesPlain(), d.getSizeBytesStored(),
                d.getSha256Plain(),
                d.getStatus() != null ? d.getStatus().name() : null,
                d.getCreatedAt(),
                base64Image);
    }
}
