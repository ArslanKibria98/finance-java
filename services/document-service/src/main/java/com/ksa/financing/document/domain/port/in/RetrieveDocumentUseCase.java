package com.ksa.financing.document.domain.port.in;

import com.ksa.financing.document.domain.model.Document;

import java.util.UUID;

public interface RetrieveDocumentUseCase {

    /** Returns metadata + decrypted plaintext bytes. */
    RetrievedDocument retrieve(UUID tenantId, UUID documentId);

    record RetrievedDocument(Document metadata, byte[] plainBytes) {}
}
