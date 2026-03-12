package com.ksa.financing.product.domain.port.in;

import com.ksa.financing.product.domain.model.ProductDocument;

import java.util.List;
import java.util.UUID;

public interface ManageProductDocumentsUseCase {

    void addDocument(UUID tenantId, UUID productId, AddDocumentCommand command);
    void updateDocument(UUID tenantId, UUID productId, UUID documentId, UpdateDocumentCommand command);
    void removeDocument(UUID tenantId, UUID productId, UUID documentId);
    ProductDocument getDocument(UUID tenantId, UUID productId, UUID documentId);
    List<ProductDocument> listDocuments(UUID tenantId, UUID productId);

    record AddDocumentCommand(
        String nameEn,
        String nameAr,
        String documentType,
        String fileUrl,
        Long fileSizeBytes,
        String fileVersion,
        String createdByName,
        boolean required
    ) {}

    record UpdateDocumentCommand(
        String nameEn,
        String nameAr,
        String documentType,
        String fileUrl,
        Long fileSizeBytes,
        String fileVersion,
        Boolean required,
        Integer sortOrder
    ) {}
}
