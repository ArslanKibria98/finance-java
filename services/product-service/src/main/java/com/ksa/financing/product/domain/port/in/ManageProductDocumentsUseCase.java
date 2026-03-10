package com.ksa.financing.product.domain.port.in;

import java.util.UUID;

public interface ManageProductDocumentsUseCase {

    void addDocument(UUID tenantId, UUID productId, AddDocumentCommand command);
    void removeDocument(UUID tenantId, UUID productId, UUID documentId);

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
}
