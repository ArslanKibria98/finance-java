package com.ksa.financing.document.domain.port.out;

import com.ksa.financing.document.domain.model.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {
    Document save(Document document);
    Optional<Document> findById(UUID tenantId, UUID id);
    Optional<Document> findByIdempotencyKey(UUID tenantId, String idempotencyKey);
    List<Document> findByCustomer(UUID tenantId, UUID customerId);
    List<Document> findByWorkflow(String workflowId);
}
