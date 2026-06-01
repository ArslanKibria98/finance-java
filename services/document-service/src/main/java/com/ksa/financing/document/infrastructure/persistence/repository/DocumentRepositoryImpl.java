package com.ksa.financing.document.infrastructure.persistence.repository;

import com.ksa.financing.document.domain.model.Document;
import com.ksa.financing.document.domain.port.out.DocumentRepository;
import com.ksa.financing.document.infrastructure.persistence.mapper.DocumentPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DocumentRepositoryImpl implements DocumentRepository {

    private final JpaDocumentRepository jpa;
    private final DocumentPersistenceMapper mapper;

    @Override
    public Document save(Document document) {
        return mapper.toDomain(jpa.save(mapper.toEntity(document)));
    }

    @Override
    public Optional<Document> findById(UUID tenantId, UUID id) {
        return jpa.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId).map(mapper::toDomain);
    }

    @Override
    public Optional<Document> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return jpa.findByTenantIdAndIdempotencyKeyAndDeletedAtIsNull(tenantId, idempotencyKey)
                .map(mapper::toDomain);
    }

    @Override
    public List<Document> findByCustomer(UUID tenantId, UUID customerId) {
        return jpa.findByTenantIdAndCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId, customerId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Document> findByWorkflow(String workflowId) {
        return jpa.findByWorkflowIdAndDeletedAtIsNullOrderByCreatedAtDesc(workflowId)
                .stream().map(mapper::toDomain).toList();
    }
}
