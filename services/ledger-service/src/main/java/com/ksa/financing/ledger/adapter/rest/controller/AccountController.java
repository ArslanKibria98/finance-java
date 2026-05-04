package com.ksa.financing.ledger.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.ledger.application.dto.CreateAccountRequest;
import com.ksa.financing.ledger.application.dto.UpdateAccountRequest;
import com.ksa.financing.ledger.application.dto.AccountResponse;
import com.ksa.financing.ledger.application.mapper.AccountMapper;
import com.ksa.financing.ledger.domain.model.AccountId;
import com.ksa.financing.ledger.domain.port.in.ManageAccountUseCase;
import com.ksa.financing.ledger.domain.port.in.ManageAccountUseCase.CreateAccountCommand;
import com.ksa.financing.ledger.domain.port.in.ManageAccountUseCase.UpdateAccountCommand;
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

import java.util.UUID;

/**
 * Controller for managing GL Accounts (Chart of Accounts).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "GL Accounts", description = "Endpoints for managing the General Ledger Chart of Accounts")
public class AccountController {

    private final ManageAccountUseCase manageAccountUseCase;
    private final AccountMapper mapper;

    @SecuredEndpoint(obj = "ledger.accounts", act = "create")
    @PostMapping
    @Operation(summary = "Create a new GL account")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var command = new CreateAccountCommand(
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
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(account));
    }

    @SecuredEndpoint(obj = "ledger.accounts", act = "update")
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing GL account")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var command = new UpdateAccountCommand(
                request.accountName(),
                request.accountNameAr()
        );

        var account = manageAccountUseCase.update(tenantId, AccountId.of(id), command);
        return ResponseEntity.ok(mapper.toResponse(account));
    }

    @SecuredEndpoint(obj = "ledger.accounts", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get GL account by ID")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var account = manageAccountUseCase.getById(tenantId, AccountId.of(id));
        return ResponseEntity.ok(mapper.toResponse(account));
    }

    @SecuredEndpoint(obj = "ledger.accounts", act = "read")
    @GetMapping("/code/{code}")
    @Operation(summary = "Get GL account by account code")
    public ResponseEntity<AccountResponse> getAccountByCode(
            @PathVariable String code,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var account = manageAccountUseCase.getByCode(tenantId, code);
        return ResponseEntity.ok(mapper.toResponse(account));
    }

    @SecuredEndpoint(obj = "ledger.accounts", act = "read")
    @GetMapping
    @Operation(summary = "List all GL accounts for the tenant (paginated)")
    public PageResponse<AccountResponse> listAccounts(
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var page = manageAccountUseCase.listByTenant(tenantId, query);
        return page.map(mapper::toResponse);
    }

    @SecuredEndpoint(obj = "ledger.accounts", act = "manage")
    @DeleteMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a GL account")
    public ResponseEntity<Void> deactivateAccount(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        manageAccountUseCase.deactivate(tenantId, AccountId.of(id));
        return ResponseEntity.noContent().build();
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
