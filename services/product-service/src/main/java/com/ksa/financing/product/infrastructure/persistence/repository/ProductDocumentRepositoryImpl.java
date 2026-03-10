package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.domain.port.out.ProductDocumentRepository;
import com.ksa.financing.product.infrastructure.persistence.entity.ProductDocumentJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
