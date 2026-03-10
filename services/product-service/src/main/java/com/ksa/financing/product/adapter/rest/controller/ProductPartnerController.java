package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.product.adapter.rest.request.AddPartnerAffiliationRequest;
import com.ksa.financing.product.domain.port.in.ManageProductPartnersUseCase;
import com.ksa.financing.product.domain.port.in.ManageProductPartnersUseCase.AddPartnerAffiliationCommand;
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
@RequestMapping("/api/v1/products/{productId}/partners")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Partners", description = "Partner affiliation management for products")
public class ProductPartnerController {

    private final ManageProductPartnersUseCase manageProductPartnersUseCase;

    @SecuredEndpoint(obj = "product-partners", act = "create")
    @PostMapping
    @Operation(summary = "Add partner affiliation", description = "Associates a partner with the product including commission configuration")
    public ResponseEntity<Void> addPartnerAffiliation(
            @PathVariable UUID productId,
            @Valid @RequestBody AddPartnerAffiliationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Adding partner affiliation for product id: {} partnerId: {} tenantId: {}",
                productId, request.partnerId(), tenantId);

        var command = new AddPartnerAffiliationCommand(
                request.partnerId(),
                request.affiliationType(),
                request.commissionPercentage()
        );

        manageProductPartnersUseCase.addPartnerAffiliation(tenantId, productId, command);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @SecuredEndpoint(obj = "product-partners", act = "delete")
    @DeleteMapping("/{partnerId}")
    @Operation(summary = "Remove partner affiliation", description = "Removes a partner association from the product")
    public ResponseEntity<Void> removePartnerAffiliation(
            @PathVariable UUID productId,
            @PathVariable UUID partnerId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Removing partner affiliation for product id: {} partnerId: {} tenantId: {}",
                productId, partnerId, tenantId);

        manageProductPartnersUseCase.removePartnerAffiliation(tenantId, productId, partnerId);

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
