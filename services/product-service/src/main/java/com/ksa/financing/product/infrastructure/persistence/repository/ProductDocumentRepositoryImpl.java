package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.domain.model.ProductDocument;
import com.ksa.financing.product.domain.port.out.ProductDocumentRepository;
import com.ksa.financing.product.infrastructure.persistence.entity.ProductDocumentJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Infrastructure implementation of the {@link ProductDocumentRepository} output port.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductDocumentRepositoryImpl implements ProductDocumentRepository {

    private final JpaProductDocumentRepository jpaDocumentRepository;

    @Override
    public List<ProductDocument> findByProductId(UUID tenantId, UUID productId) {
        return jpaDocumentRepository.findByProductIdAndTenantIdOrderBySortOrder(productId, tenantId)
                .stream()
                .map(e -> new ProductDocument(
                        e.getId(),
                        e.getNameEn(),
                        e.getNameAr(),
                        e.getDocumentType(),
                        e.getFileUrl(),
                        e.getFileSizeBytes(),
                        e.getFileVersion(),
                        e.getCreatedByName(),
                        e.getStatus(),
                        e.isRequired(),
                        e.getSortOrder(),
                        e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null,
                        e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant() : null))
                .toList();
    }

    @Override
    public Optional<ProductDocument> findById(UUID tenantId, UUID productId, UUID documentId) {
        return jpaDocumentRepository.findByIdAndProductIdAndTenantId(documentId, productId, tenantId)
                .map(e -> new ProductDocument(
                        e.getId(), e.getNameEn(), e.getNameAr(),
                        e.getDocumentType(), e.getFileUrl(), e.getFileSizeBytes(),
                        e.getFileVersion(), e.getCreatedByName(), e.getStatus(),
                        e.isRequired(), e.getSortOrder(),
                        e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null,
                        e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant() : null));
    }

    @Override
    public void saveDocument(UUID tenantId, UUID productId, String nameEn, String nameAr,
                             String documentType, String fileUrl, Long fileSizeBytes,
                             String fileVersion, String createdByName, boolean required) {
        log.debug("Saving document for productId={}, name={}", productId, nameEn);

        var now = OffsetDateTime.now(ZoneOffset.UTC);

        var entity = new ProductDocumentJpaEntity();
        entity.setTenantId(tenantId);
        entity.setProductId(productId);
        entity.setNameEn(nameEn);
        entity.setNameAr(nameAr);
        entity.setDocumentType(documentType);
        entity.setFileUrl(fileUrl);
        entity.setFileSizeBytes(fileSizeBytes);
        entity.setFileVersion(fileVersion);
        entity.setCreatedByName(createdByName);
        entity.setStatus("ACTIVE");
        entity.setRequired(required);
        entity.setSortOrder(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        jpaDocumentRepository.save(entity);
        log.debug("Document saved for productId={}, id={}", productId, entity.getId());
    }

    @Override
    @Transactional
    public void updateDocument(UUID tenantId, UUID productId, UUID documentId,
                               String nameEn, String nameAr, String documentType,
                               String fileUrl, Long fileSizeBytes, String fileVersion,
                               Boolean required, Integer sortOrder) {
        log.debug("Updating document: id={}, productId={}", documentId, productId);

        var entity = jpaDocumentRepository.findByIdAndProductIdAndTenantId(documentId, productId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (nameEn != null) entity.setNameEn(nameEn);
        if (nameAr != null) entity.setNameAr(nameAr);
        if (documentType != null) entity.setDocumentType(documentType);
        if (fileUrl != null) entity.setFileUrl(fileUrl);
        if (fileSizeBytes != null) entity.setFileSizeBytes(fileSizeBytes);
        if (fileVersion != null) entity.setFileVersion(fileVersion);
        if (required != null) entity.setRequired(required);
        if (sortOrder != null) entity.setSortOrder(sortOrder);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        jpaDocumentRepository.save(entity);
        log.debug("Document updated: id={}", documentId);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID tenantId, UUID productId, UUID documentId) {
        log.debug("Deleting document: id={}, productId={}, tenantId={}", documentId, productId, tenantId);

        jpaDocumentRepository.deleteByIdAndProductIdAndTenantId(documentId, productId, tenantId);
        log.debug("Document deleted: id={}", documentId);
    }

    @Override
    public boolean existsDocument(UUID tenantId, UUID productId, UUID documentId) {
        return jpaDocumentRepository.existsByIdAndProductIdAndTenantId(documentId, productId, tenantId);
    }
}
