package com.ksa.financing.document.adapter.rest.request;

import com.ksa.financing.document.domain.model.DocumentKind;
import com.ksa.financing.document.domain.model.DocumentSourceFlow;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StoreDocumentRequest(
        @NotNull UUID tenantId,
        UUID customerId,
        String workflowId,
        @NotNull DocumentKind kind,
        DocumentSourceFlow sourceFlow,
        String documentNumber,
        String faciaReferenceId,
        String contentType,
        @NotBlank String base64Image,
        String idempotencyKey,
        UUID createdBy
) {}
