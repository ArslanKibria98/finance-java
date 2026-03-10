package com.ksa.financing.service.template.adapter.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.ksa.financing.domain.model.TenantId;
import com.ksa.financing.domain.model.UserId;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.service.template.adapter.rest.request.CreateExampleRequest;
import com.ksa.financing.service.template.adapter.rest.request.AddEntityRequest;
import com.ksa.financing.service.template.adapter.rest.response.ExampleResponse;
import com.ksa.financing.service.template.application.dto.ExampleAggregateDto;
import com.ksa.financing.service.template.application.mapper.ExampleAggregateMapper;
import com.ksa.financing.service.template.domain.model.ExampleAggregateId;
import com.ksa.financing.service.template.domain.model.ExampleStatus;
import com.ksa.financing.service.template.domain.port.in.ManageExampleUseCase;
import com.ksa.financing.service.template.domain.port.out.ExampleRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Example aggregate operations.
 * This is a driving adapter in Hexagonal Architecture.
 *
 * Key features:
 * - JWT authentication via Keycloak
 * - Tenant isolation from JWT claims
 * - OpenAPI documentation
 * - Proper error handling
 * - Correlation ID propagation
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
@Tag(name = "Example", description = "Example aggregate management endpoints")
public class ExampleController {

    private final ManageExampleUseCase useCase;
    // TODO: Refactor to use use case instead of direct repository access (hexagonal violation)
    private final ExampleRepository repository;
    private final ExampleAggregateMapper mapper;

    @PostMapping
    @Operation(summary = "Create a new example aggregate")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Example created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ExampleResponse> createExample(
            @Valid @RequestBody CreateExampleRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.info("Creating example aggregate. CorrelationId: {}", correlationId);

        // Extract tenant and user from JWT
        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);

        // Create command
        var command = new ManageExampleUseCase.CreateExampleCommand(
                tenantId,
                request.getName(),
                request.getDescription(),
                userId
        );

        // Execute use case
        var aggregate = useCase.createExample(command);

        // Map to response
        var dto = mapper.toDto(aggregate);
        var response = ExampleResponse.from(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("X-Correlation-ID", correlationId)
                .body(response);
    }

    @GetMapping("/{aggregateId}")
    @Operation(summary = "Get an example aggregate by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Example found"),
        @ApiResponse(responseCode = "404", description = "Example not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ExampleResponse> getExample(
            @Parameter(description = "Aggregate ID") @PathVariable String aggregateId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.debug("Getting example aggregate: {}. CorrelationId: {}", aggregateId, correlationId);

        var tenantId = extractTenantId(jwt);

        var query = new ManageExampleUseCase.GetExampleQuery(
                tenantId,
                ExampleAggregateId.of(aggregateId)
        );

        var aggregate = useCase.getExample(query);
        var dto = mapper.toDto(aggregate);
        var response = ExampleResponse.from(dto);

        return ResponseEntity
                .ok()
                .header("X-Correlation-ID", correlationId)
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List all example aggregates for tenant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ExampleResponse>> listExamples(
            @Parameter(description = "Filter by status") @RequestParam(required = false) ExampleStatus status,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.debug("Listing examples. Status filter: {}. CorrelationId: {}", status, correlationId);

        var tenantId = extractTenantId(jwt);

        List<ExampleAggregateDto> dtos;
        if (status != null) {
            var aggregates = repository.findByStatus(tenantId, status);
            dtos = mapper.toDtos(aggregates);
        } else {
            var aggregates = repository.findAllByTenant(tenantId);
            dtos = mapper.toDtos(aggregates);
        }

        var responses = dtos.stream()
                .map(ExampleResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity
                .ok()
                .header("X-Correlation-ID", correlationId)
                .body(responses);
    }

    @PostMapping("/{aggregateId}/activate")
    @Operation(summary = "Activate an example aggregate")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Activated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition"),
        @ApiResponse(responseCode = "404", description = "Example not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> activateExample(
            @PathVariable String aggregateId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.info("Activating example: {}. CorrelationId: {}", aggregateId, correlationId);

        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);

        var command = new ManageExampleUseCase.ActivateExampleCommand(
                tenantId,
                ExampleAggregateId.of(aggregateId),
                userId
        );

        useCase.activateExample(command);

        return ResponseEntity
                .noContent()
                .header("X-Correlation-ID", correlationId)
                .build();
    }

    @PostMapping("/{aggregateId}/entities")
    @Operation(summary = "Add an entity to the example aggregate")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Entity added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Example not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> addEntity(
            @PathVariable String aggregateId,
            @Valid @RequestBody AddEntityRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.info("Adding entity to example: {}. CorrelationId: {}", aggregateId, correlationId);

        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);

        var command = new ManageExampleUseCase.AddEntityCommand(
                tenantId,
                ExampleAggregateId.of(aggregateId),
                request.getName(),
                request.getValue(),
                userId
        );

        useCase.addEntity(command);

        return ResponseEntity
                .noContent()
                .header("X-Correlation-ID", correlationId)
                .build();
    }

    @PostMapping("/{aggregateId}/complete")
    @Operation(summary = "Complete an example aggregate")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Completed successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot complete"),
        @ApiResponse(responseCode = "404", description = "Example not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> completeExample(
            @PathVariable String aggregateId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.info("Completing example: {}. CorrelationId: {}", aggregateId, correlationId);

        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);

        var command = new ManageExampleUseCase.CompleteExampleCommand(
                tenantId,
                ExampleAggregateId.of(aggregateId),
                userId
        );

        useCase.completeExample(command);

        return ResponseEntity
                .noContent()
                .header("X-Correlation-ID", correlationId)
                .build();
    }

    /**
     * Extract tenant ID from JWT claims.
     */
    private TenantId extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return new TenantId(tenantClaim);
    }

    /**
     * Extract user ID from JWT claims.
     */
    private UserId extractUserId(Jwt jwt) {
        var subject = jwt.getSubject();
        if (subject == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No subject claim found in JWT token");
        }
        return new UserId(subject);
    }
}