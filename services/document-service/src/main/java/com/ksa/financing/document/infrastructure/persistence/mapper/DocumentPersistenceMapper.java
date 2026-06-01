package com.ksa.financing.document.infrastructure.persistence.mapper;

import com.ksa.financing.document.domain.model.Document;
import com.ksa.financing.document.infrastructure.persistence.entity.DocumentJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class DocumentPersistenceMapper {

    public DocumentJpaEntity toEntity(Document d) {
        DocumentJpaEntity e = new DocumentJpaEntity();
        e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setCustomerId(d.getCustomerId());
        e.setWorkflowId(d.getWorkflowId());
        e.setKind(d.getKind());
        e.setSourceFlow(d.getSourceFlow());
        e.setDocumentNumber(d.getDocumentNumber());
        e.setFaciaReferenceId(d.getFaciaReferenceId());
        e.setObjectKey(d.getObjectKey());
        e.setContentType(d.getContentType());
        e.setSizeBytesPlain(d.getSizeBytesPlain());
        e.setSizeBytesStored(d.getSizeBytesStored());
        e.setSha256Plain(d.getSha256Plain());
        e.setCipherAlgo(d.getCipherAlgo());
        e.setEncryptedKey(d.getEncryptedKey());
        e.setIv(d.getIv());
        e.setStatus(d.getStatus());
        e.setIdempotencyKey(d.getIdempotencyKey());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        e.setCreatedBy(d.getCreatedBy());
        e.setVersion(d.getVersion());
        return e;
    }

    public Document toDomain(DocumentJpaEntity e) {
        Document d = new Document();
        d.setId(e.getId());
        d.setTenantId(e.getTenantId());
        d.setCustomerId(e.getCustomerId());
        d.setWorkflowId(e.getWorkflowId());
        d.setKind(e.getKind());
        d.setSourceFlow(e.getSourceFlow());
        d.setDocumentNumber(e.getDocumentNumber());
        d.setFaciaReferenceId(e.getFaciaReferenceId());
        d.setObjectKey(e.getObjectKey());
        d.setContentType(e.getContentType());
        d.setSizeBytesPlain(e.getSizeBytesPlain());
        d.setSizeBytesStored(e.getSizeBytesStored());
        d.setSha256Plain(e.getSha256Plain());
        d.setCipherAlgo(e.getCipherAlgo());
        d.setEncryptedKey(e.getEncryptedKey());
        d.setIv(e.getIv());
        d.setStatus(e.getStatus());
        d.setIdempotencyKey(e.getIdempotencyKey());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        d.setCreatedBy(e.getCreatedBy());
        d.setVersion(e.getVersion());
        return d;
    }
}
