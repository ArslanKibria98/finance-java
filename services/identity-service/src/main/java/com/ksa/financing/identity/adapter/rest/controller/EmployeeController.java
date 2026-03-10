package com.ksa.financing.identity.adapter.rest.controller;

import com.ksa.financing.identity.adapter.rest.request.CreateEmployeeRequest;
import com.ksa.financing.identity.adapter.rest.request.UpdateEmployeeRequest;
import com.ksa.financing.identity.adapter.rest.response.EmployeeResponse;
import com.ksa.financing.identity.domain.model.Employee;
import com.ksa.financing.identity.domain.port.in.ManageEmployeeUseCase;
import com.ksa.financing.identity.domain.port.in.ManageEmployeeUseCase.CreateEmployeeCommand;
import com.ksa.financing.identity.domain.port.in.ManageEmployeeUseCase.UpdateEmployeeCommand;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employees", description = "Employee management with Keycloak sync")
public class EmployeeController {

    private final ManageEmployeeUseCase manageEmployeeUseCase;

    @SecuredEndpoint(obj = "employees", act = "create")
    @PostMapping
    @Operation(summary = "Create employee", description = "Creates a new employee and syncs to Keycloak")
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Creating employee: email={} tenant={}", request.email(), tenantId);

        var command = new CreateEmployeeCommand(
                tenantId, request.name(), request.email(),
                request.password(), request.phone(),
                request.address(), request.roleId());

        var employee = manageEmployeeUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(employee));
    }

    @SecuredEndpoint(obj = "employees", act = "read")
    @GetMapping
    @Operation(summary = "List employees", description = "Returns all employees for the tenant")
    public ResponseEntity<List<EmployeeResponse>> listEmployees(@AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var employees = manageEmployeeUseCase.listByTenant(tenantId);
        return ResponseEntity.ok(employees.stream().map(this::toResponse).toList());
    }

    @SecuredEndpoint(obj = "employees", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get employee", description = "Returns a single employee by ID")
    public ResponseEntity<EmployeeResponse> getEmployee(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var employee = manageEmployeeUseCase.getById(tenantId, id);
        return ResponseEntity.ok(toResponse(employee));
    }

    @SecuredEndpoint(obj = "employees", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update employee", description = "Updates employee information")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating employee: id={} tenant={}", id, tenantId);

        var command = new UpdateEmployeeCommand(
                request.name(), request.phone(),
                request.address(), request.roleId(), request.active(), request.status());

        var employee = manageEmployeeUseCase.update(tenantId, id, command);
        return ResponseEntity.ok(toResponse(employee));
    }

    @SecuredEndpoint(obj = "employees", act = "delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee", description = "Soft-deletes an employee (sets status to TERMINATED)")
    public ResponseEntity<Void> deleteEmployee(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Deleting employee: id={} tenant={}", id, tenantId);
        manageEmployeeUseCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    private EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(
                e.getId(), e.getTenantId(), e.getKeycloakUserId(),
                e.getName(), e.getEmail(), e.getPhone(),
                e.getAddress(), e.getRoleId(),
                e.getStatus().name(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null || tenantClaim.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }
}
