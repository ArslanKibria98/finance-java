package com.ksa.financing.product.domain.port.out;

import com.ksa.financing.product.domain.model.ProductDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductDocumentRepository {
    List<ProductDocument> findByProductId(UUID tenantId, UUID productId);
    Optional<ProductDocument> findById(UUID tenantId, UUID productId, UUID documentId);
    void saveDocument(UUID tenantId, UUID productId, String nameEn, String nameAr,
                      String documentType, String fileUrl, Long fileSizeBytes,
                      String fileVersion, String createdByName, boolean required);
    void updateDocument(UUID tenantId, UUID productId, UUID documentId,
                        String nameEn, String nameAr, String documentType,
                        String fileUrl, Long fileSizeBytes, String fileVersion,
                        Boolean required, Integer sortOrder);
    void deleteDocument(UUID tenantId, UUID productId, UUID documentId);
    boolean existsDocument(UUID tenantId, UUID productId, UUID documentId);
}
