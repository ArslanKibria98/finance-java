package com.ksa.financing.product.adapter.rest.controller;

import com.ksa.financing.product.domain.model.CreditScoringFieldDefinition;
import com.ksa.financing.product.domain.port.in.ManageCreditScoringFieldsUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/credit-scoring-fields")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Credit Scoring Fields", description = "Credit scoring field definitions with predefined options")
public class CreditScoringFieldController {

    private final ManageCreditScoringFieldsUseCase manageCreditScoringFieldsUseCase;

    @SecuredEndpoint(obj = "credit-scoring-fields", act = "read")
    @GetMapping
    @Operation(summary = "List credit scoring fields",
            description = "Returns all active credit scoring field definitions with their predefined options for dropdowns")
    public ResponseEntity<List<CreditScoringFieldDefinition>> listFields(
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Listing credit scoring fields for tenant: {}", tenantId);

        var fields = manageCreditScoringFieldsUseCase.listFieldDefinitions(tenantId);
        return ResponseEntity.ok(fields);
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
