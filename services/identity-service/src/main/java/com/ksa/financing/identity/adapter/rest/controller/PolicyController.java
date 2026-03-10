package com.ksa.financing.identity.adapter.rest.controller;

import com.ksa.financing.identity.adapter.rest.request.AuthorizationCheckRequest;
import com.ksa.financing.identity.adapter.rest.request.PolicyRequest;
import com.ksa.financing.identity.adapter.rest.request.RoleGroupingRequest;
import com.ksa.financing.identity.adapter.rest.response.AuthorizationResponse;
import com.ksa.financing.identity.adapter.rest.response.PolicyResponse;
import com.ksa.financing.identity.domain.port.in.CheckAuthorizationUseCase;
import com.ksa.financing.identity.domain.port.in.CheckAuthorizationUseCase.AuthorizationRequest;
import com.ksa.financing.identity.domain.port.in.ManagePolicyUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Policies", description = "Casbin policy rule management and authorization checks")
public class PolicyController {

    private final ManagePolicyUseCase managePolicyUseCase;
    private final CheckAuthorizationUseCase checkAuthorizationUseCase;

    @SecuredEndpoint(obj = "policies", act = "create")
    @PostMapping
    @Operation(summary = "Add policy", description = "Adds a new Casbin policy rule (role, resource, action)")
    public ResponseEntity<Void> addPolicy(@Valid @RequestBody PolicyRequest request) {
        log.info("Adding policy: role={}, resource={}, action={}", request.role(), request.resource(), request.action());
        managePolicyUseCase.addPolicy(request.role(), request.resource(), request.action());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @SecuredEndpoint(obj = "policies", act = "delete")
    @DeleteMapping
    @Operation(summary = "Remove policy", description = "Removes a Casbin policy rule")
    public ResponseEntity<Void> removePolicy(@Valid @RequestBody PolicyRequest request) {
        log.info("Removing policy: role={}, resource={}, action={}", request.role(), request.resource(), request.action());
        managePolicyUseCase.removePolicy(request.role(), request.resource(), request.action());
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "policies", act = "read")
    @GetMapping
    @Operation(summary = "List all policies", description = "Returns all Casbin policy rules")
    public ResponseEntity<List<PolicyResponse>> listPolicies() {
        var policies = managePolicyUseCase.listPolicies();
        var response = policies.stream()
                .map(p -> new PolicyResponse(p.role(), p.resource(), p.action()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "policies", act = "read")
    @GetMapping("/role/{role}")
    @Operation(summary = "List policies for role", description = "Returns Casbin policy rules for a specific role")
    public ResponseEntity<List<PolicyResponse>> listPoliciesForRole(@PathVariable String role) {
        var policies = managePolicyUseCase.listPoliciesForRole(role);
        var response = policies.stream()
                .map(p -> new PolicyResponse(p.role(), p.resource(), p.action()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "policies", act = "create")
    @PostMapping("/role-grouping")
    @Operation(summary = "Add role grouping", description = "Assigns a role to a user in Casbin (g = user, role)")
    public ResponseEntity<Void> addRoleGrouping(@Valid @RequestBody RoleGroupingRequest request) {
        log.info("Adding role grouping: user={}, role={}", request.user(), request.role());
        managePolicyUseCase.addRoleGrouping(request.user(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @SecuredEndpoint(obj = "policies", act = "delete")
    @DeleteMapping("/role-grouping")
    @Operation(summary = "Remove role grouping", description = "Removes a user's role assignment in Casbin")
    public ResponseEntity<Void> removeRoleGrouping(@Valid @RequestBody RoleGroupingRequest request) {
        log.info("Removing role grouping: user={}, role={}", request.user(), request.role());
        managePolicyUseCase.removeRoleGrouping(request.user(), request.role());
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "policies", act = "read")
    @GetMapping("/role-grouping/{user}")
    @Operation(summary = "Get roles for user", description = "Returns Casbin roles assigned to a user")
    public ResponseEntity<List<String>> getRolesForUser(@PathVariable String user) {
        var roles = managePolicyUseCase.getRolesForUser(user);
        return ResponseEntity.ok(roles);
    }

    @SecuredEndpoint(obj = "policies", act = "authorize")
    @PostMapping("/authorize")
    @Operation(summary = "Check authorization", description = "Checks if a subject is authorized for a resource and action")
    public ResponseEntity<AuthorizationResponse> checkAuthorization(
            @Valid @RequestBody AuthorizationCheckRequest request) {

        var result = checkAuthorizationUseCase.check(
                new AuthorizationRequest(request.subject(), request.resource(), request.action()));

        return ResponseEntity.ok(new AuthorizationResponse(
                result.allowed(), result.subject(), result.resource(), result.action()));
    }

    @SecuredEndpoint(obj = "policies", act = "manage")
    @PostMapping("/reload")
    @Operation(summary = "Reload policies", description = "Reloads all Casbin policies from the database")
    public ResponseEntity<Void> reloadPolicies() {
        log.info("Reloading Casbin policies from database");
        managePolicyUseCase.reloadPolicies();
        return ResponseEntity.ok().build();
    }
}
