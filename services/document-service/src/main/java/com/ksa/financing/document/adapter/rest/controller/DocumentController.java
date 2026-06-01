package com.ksa.financing.document.adapter.rest.controller;

import com.ksa.financing.document.adapter.rest.request.StoreDocumentRequest;
import com.ksa.financing.document.adapter.rest.response.DocumentResponse;
import com.ksa.financing.document.domain.model.DocumentSourceFlow;
import com.ksa.financing.document.domain.port.in.RetrieveDocumentUseCase;
import com.ksa.financing.document.domain.port.in.StoreDocumentUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.UUID;

/**
 * Document service REST controller.
 *
 * <p><b>Internal (/internal/**)</b> — invoked service-to-service from inside the
 * Docker network. Not behind JWT. Trust comes from network isolation.</p>
 *
 * <p><b>Public (/api/v1/documents/**)</b> — JWT-authenticated for direct user
 * downloads / admin lookups.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final StoreDocumentUseCase storeDocumentUseCase;
    private final RetrieveDocumentUseCase retrieveDocumentUseCase;

    @Hidden
    @PostMapping("/internal/documents")
    @Operation(summary = "Store an onboarding document (base64). Internal use only.")
    public ResponseEntity<DocumentResponse> storeInternal(@Valid @RequestBody StoreDocumentRequest req) {
        byte[] bytes = decodeBase64(req.base64Image());
        var saved = storeDocumentUseCase.store(new StoreDocumentUseCase.StoreDocumentCommand(
                req.tenantId(),
                req.customerId(),
                req.workflowId(),
                req.kind(),
                req.sourceFlow() != null ? req.sourceFlow() : DocumentSourceFlow.OTHER,
                req.documentNumber(),
                req.faciaReferenceId(),
                req.contentType(),
                bytes,
                req.idempotencyKey(),
                req.createdBy()));
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(saved));
    }

    @Hidden
    @GetMapping("/internal/documents/{documentId}")
    @Operation(summary = "Retrieve document metadata + decrypted base64. Internal use only.")
    public ResponseEntity<DocumentResponse> retrieveInternal(
            @PathVariable UUID documentId,
            @RequestParam UUID tenantId,
            @RequestParam(defaultValue = "false") boolean includeImage) {
        var result = retrieveDocumentUseCase.retrieve(tenantId, documentId);
        DocumentResponse body = includeImage
                ? DocumentResponse.withImage(result.metadata(), Base64.getEncoder().encodeToString(result.plainBytes()))
                : DocumentResponse.from(result.metadata());
        return ResponseEntity.ok(body);
    }

    private byte[] decodeBase64(String input) {
        if (input == null || input.isBlank()) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "base64Image must not be blank");
        }
        String stripped = input;
        int comma = stripped.indexOf(",");
        if (stripped.startsWith("data:") && comma > 0) {
            // data:image/png;base64,XXXX → keep only XXXX
            stripped = stripped.substring(comma + 1);
        }
        try {
            return Base64.getDecoder().decode(stripped.replaceAll("\\s+", ""));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                    "base64Image is not valid base64: " + e.getMessage());
        }
    }
}
