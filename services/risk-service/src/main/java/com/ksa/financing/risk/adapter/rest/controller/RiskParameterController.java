package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.domain.model.parameter.FilledByType;
import com.ksa.financing.risk.domain.model.parameter.ParameterFlagType;
import com.ksa.financing.risk.domain.model.parameter.ParameterInputType;
import com.ksa.financing.risk.domain.model.parameter.RiskParameter;
import com.ksa.financing.risk.domain.model.parameter.RiskType;
import com.ksa.financing.risk.domain.port.in.ManageRiskParameterUseCase;
import com.ksa.financing.risk.domain.port.in.ManageRiskParameterUseCase.CreateRiskParameterCommand;
import com.ksa.financing.risk.domain.port.in.ManageRiskParameterUseCase.UpdateRiskParameterCommand;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk/parameters")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Risk Parameters", description = "APIs for managing risk assessment parameters")
public class RiskParameterController {

    private final ManageRiskParameterUseCase manageRiskParameterUseCase;

    @SecuredEndpoint(obj = "risk.parameters", act = "create")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a risk parameter")
    public RiskParameter create(
            @Valid @RequestBody CreateRiskParameterRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Creating risk parameter for type: {} category: {} tenant: {}", request.riskType(), request.category(), tenantId);
        return manageRiskParameterUseCase.create(tenantId, new CreateRiskParameterCommand(
                request.riskType(),
                request.flow(),
                request.category(),
                request.subCategory(),
                request.questionEn(),
                request.questionAr(),
                request.inputType(),
                request.lovSetId(),
                request.parentParameterId(),
                request.parentTriggerValue(),
                request.categoryWeight(),
                request.operator(),
                request.expectedValue(),
                request.flagType(),
                request.filledBy(),
                request.displayOrder(),
                request.language()
        ));
    }

    @SecuredEndpoint(obj = "risk.parameters", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update a risk parameter")
    public RiskParameter update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRiskParameterRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Updating risk parameter: {} for tenant: {}", id, tenantId);
        return manageRiskParameterUseCase.update(tenantId, id, new UpdateRiskParameterCommand(
                request.questionEn(),
                request.questionAr(),
                request.inputType(),
                request.lovSetId(),
                request.parentParameterId(),
                request.parentTriggerValue(),
                request.categoryWeight(),
                request.operator(),
                request.expectedValue(),
                request.flagType(),
                request.filledBy(),
                request.displayOrder(),
                request.language()
        ));
    }

    @SecuredEndpoint(obj = "risk.parameters", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get risk parameter by ID")
    public RiskParameter getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageRiskParameterUseCase.getById(tenantId, id);
    }

    @SecuredEndpoint(obj = "risk.parameters", act = "read")
    @GetMapping("/type/{riskType}")
    @Operation(summary = "Get all risk parameters by risk type")
    public List<RiskParameter> getByRiskType(
            @PathVariable RiskType riskType,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageRiskParameterUseCase.getByRiskType(tenantId, riskType);
    }

    @SecuredEndpoint(obj = "risk.parameters", act = "read")
    @GetMapping("/type/{riskType}/active")
    @Operation(summary = "Get active risk parameters by risk type")
    public List<RiskParameter> getActiveByRiskType(
            @PathVariable RiskType riskType,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageRiskParameterUseCase.getActiveByRiskType(tenantId, riskType);
    }

    @SecuredEndpoint(obj = "risk.parameters", act = "read")
    @GetMapping("/type/{riskType}/category/{category}")
    @Operation(summary = "Get risk parameters by risk type and category")
    public List<RiskParameter> getByCategory(
            @PathVariable RiskType riskType,
            @PathVariable String category,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageRiskParameterUseCase.getByCategory(tenantId, riskType, category);
    }

    @SecuredEndpoint(obj = "risk.parameters", act = "manage")
    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a risk parameter")
    public void deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Deactivating risk parameter: {} for tenant: {}", id, tenantId);
        manageRiskParameterUseCase.deactivate(tenantId, id);
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

    // === Request Records ===

    record CreateRiskParameterRequest(
            RiskType riskType,
            String flow,
            String category,
            String subCategory,
            String questionEn,
            String questionAr,
            ParameterInputType inputType,
            UUID lovSetId,
            UUID parentParameterId,
            String parentTriggerValue,
            BigDecimal categoryWeight,
            String operator,
            String expectedValue,
            ParameterFlagType flagType,
            FilledByType filledBy,
            int displayOrder,
            String language
    ) {}

    record UpdateRiskParameterRequest(
            String questionEn,
            String questionAr,
            ParameterInputType inputType,
            UUID lovSetId,
            UUID parentParameterId,
            String parentTriggerValue,
            BigDecimal categoryWeight,
            String operator,
            String expectedValue,
            ParameterFlagType flagType,
            FilledByType filledBy,
            Integer displayOrder,
            String language
    ) {}
}
