package com.ksa.financing.ledger.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.ledger.application.dto.AccountResponse;
import com.ksa.financing.ledger.application.dto.CreateAccountRequest;
import com.ksa.financing.ledger.application.dto.UpdateAccountRequest;
import com.ksa.financing.ledger.application.mapper.JournalEntryMapper;
import com.ksa.financing.ledger.domain.model.AccountId;
import com.ksa.financing.ledger.domain.port.in.ManageAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

/**
 * REST controller for Chart of Accounts management.
 * All endpoints protected by Casbin ABAC via @SecuredEndpoint.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "GL Accounts", description = "Chart of Accounts (COA) management")
public class AccountController {

    private final ManageAccountUseCase manageAccountUseCase;
    private final JournalEntryMapper mapper;

    @SecuredEndpoint(obj = "ledger.accounts", act = "create")
    @PostMapping
    @Operation(summary = "Create a GL account", description = "Add a new account to the Chart of Accounts")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "409", description = "Account code already exists")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Creating GL account: code={} type={} tenant={}", request.accountCode(), request.accountType(), tenantId);

        var command = new ManageAccountUseCase.CreateAccountCommand(
                tenantId,
                request.accountCode(),
                request.accountName(),
                request.accountNameAr(),
                request.accountType(),
                request.parentAccountCode(),
                request.isHeader(),
                request.iban()
        );

        var account = manageAccountUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toAccountResponse(account));
    }

    @SecuredEndpoint(obj = "ledger.accounts", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get GL account by ID")
    public ResponseEntity<AccountResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var account = manageAccountUseCase.getById(tenantId, AccountId.of(id));
        return ResponseEntity.ok(mapper.toAccountResponse(account));
    }

    @SecuredEndpoint(obj = "ledger.accounts", act = "read")
    @GetMapping("/by-code/{code}")
    @Operation(summary = "Get GL account by code")
    public ResponseEntity<AccountResponse> getByCode(
            @PathVariable String code,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var account = manageAccountUseCase.getByCode(tenantId, code);
        return ResponseEntity.ok(mapper.toAccountResponse(account));
    }

    @SecuredEndpoint(obj = "ledger.accounts", act = "read")
    @GetMapping
    @Operation(summary = "List all GL accounts", description = "Returns full Chart of Accounts for the tenant")
    public ResponseEntity<List<AccountResponse>> listAccounts(
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var accounts = manageAccountUseCase.listByTenant(tenantId);
        return ResponseEntity.ok(accounts.stream().map(mapper::toAccountResponse).toList());
    }

    @SecuredEndpoint(obj = "ledger.accounts", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update GL account name")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var command = new ManageAccountUseCase.UpdateAccountCommand(request.accountName(), request.accountNameAr());
        var account = manageAccountUseCase.update(tenantId, AccountId.of(id), command);
        return ResponseEntity.ok(mapper.toAccountResponse(account));
    }

    @SecuredEndpoint(obj = "ledger.accounts", act = "manage")
    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a GL account")
    public ResponseEntity<Void> deactivateAccount(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        manageAccountUseCase.deactivate(tenantId, AccountId.of(id));
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private UUID extractTenantId(Jwt jwt) {
        String tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }
}
