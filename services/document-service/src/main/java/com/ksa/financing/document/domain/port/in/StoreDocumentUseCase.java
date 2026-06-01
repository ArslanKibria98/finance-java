package com.ksa.financing.document.domain.port.in;

import com.ksa.financing.document.domain.model.Document;
import com.ksa.financing.document.domain.model.DocumentKind;
import com.ksa.financing.document.domain.model.DocumentSourceFlow;

import java.util.UUID;

public interface StoreDocumentUseCase {

    Document store(StoreDocumentCommand command);

    record StoreDocumentCommand(
            UUID tenantId,
            UUID customerId,
            String workflowId,
            DocumentKind kind,
            DocumentSourceFlow sourceFlow,
            String documentNumber,
            String faciaReferenceId,
            String contentType,
            byte[] plainBytes,
            String idempotencyKey,
            UUID createdBy
    ) {}
}
