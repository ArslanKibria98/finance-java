package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.product.adapter.rest.request.AddDocumentRequest;
import com.ksa.financing.product.domain.port.in.ManageProductDocumentsUseCase;
import com.ksa.financing.product.domain.port.in.ManageProductDocumentsUseCase.AddDocumentCommand;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/{productId}/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Documents", description = "Document management for product configuration")
public class ProductDocumentController {

    private final ManageProductDocumentsUseCase manageProductDocumentsUseCase;

    @SecuredEndpoint(obj = "product-documents", act = "create")
    @PostMapping
    @Operation(summary = "Add document", description = "Adds a required or optional document to the product configuration")
    public ResponseEntity<Void> addDocument(
            @PathVariable UUID productId,
            @Valid @RequestBody AddDocumentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Adding document '{}' to product id: {} tenantId: {}",
                request.nameEn(), productId, tenantId);

        var command = new AddDocumentCommand(
                request.nameEn(),
                request.nameAr(),
                request.documentType(),
                request.fileUrl(),
                request.fileSizeBytes(),
                request.fileVersion(),
                request.createdByName(),
                request.required()
        );

        manageProductDocumentsUseCase.addDocument(tenantId, productId, command);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @SecuredEndpoint(obj = "product-documents", act = "delete")
    @DeleteMapping("/{documentId}")
    @Operation(summary = "Remove document", description = "Removes a document from the product configuration")
    public ResponseEntity<Void> removeDocument(
            @PathVariable UUID productId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Removing document id: {} from product id: {} tenantId: {}",
                documentId, productId, tenantId);

        manageProductDocumentsUseCase.removeDocument(tenantId, productId, documentId);

        return ResponseEntity.noContent().build();
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }
}
