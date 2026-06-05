package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.wallet.application.dto.DecideLimitChangeRequest;
import com.ksa.financing.wallet.application.dto.LimitBoundsResponse;
import com.ksa.financing.wallet.application.dto.LimitChangeRequestResponse;
import com.ksa.financing.wallet.application.dto.SetLimitBoundsRequest;
import com.ksa.financing.wallet.domain.model.LimitRequestStatus;
import com.ksa.financing.wallet.domain.port.in.DecideWalletLimitChangeUseCase;
import com.ksa.financing.wallet.domain.port.in.ManageWalletLimitBoundsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin endpoints: configure platform transaction-limit bounds (min/max) and approve / reject
 * customer limit-change requests.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/wallets")
@RequiredArgsConstructor
@Tag(name = "Admin Wallet Limits", description = "Platform bounds + limit-change approvals")
public class AdminWalletLimitController {

    private final ManageWalletLimitBoundsUseCase boundsUseCase;
    private final DecideWalletLimitChangeUseCase decideUseCase;

    @SecuredEndpoint(obj = "wallet.limit-bounds", act = "read")
    @GetMapping("/limit-bounds")
    @Operation(summary = "Get the platform transaction-limit bounds for the tenant")
    public ResponseEntity<LimitBoundsResponse> getBounds(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(LimitBoundsResponse.from(boundsUseCase.getBounds(tenantId)));
    }

    @SecuredEndpoint(obj = "wallet.limit-bounds", act = "manage")
    @PutMapping("/limit-bounds")
    @Operation(summary = "Set the platform min/max transaction-limit bounds + defaults")
    public ResponseEntity<LimitBoundsResponse> setBounds(
            @Valid @RequestBody SetLimitBoundsRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID updatedBy = extractUserId(jwt);
        var updated = boundsUseCase.updateBounds(new ManageWalletLimitBoundsUseCase.UpdateBoundsCommand(
                tenantId,
                request.minSingleLimit(), request.maxSingleLimit(),
                request.minDailyLimit(), request.maxDailyLimit(),
                request.minMonthlyLimit(), request.maxMonthlyLimit(),
                request.minYearlyLimit(), request.maxYearlyLimit(),
                request.defaultSingleLimit(), request.defaultDailyLimit(),
                request.defaultMonthlyLimit(), request.defaultYearlyLimit(),
                updatedBy));
        return ResponseEntity.ok(LimitBoundsResponse.from(updated));
    }

    @SecuredEndpoint(obj = "wallet.limit-requests", act = "read")
    @GetMapping("/limit-requests")
    @Operation(summary = "List limit-change requests (optionally filtered by status)")
    public List<LimitChangeRequestResponse> listRequests(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        LimitRequestStatus parsed = parseStatus(status);
        return decideUseCase.list(tenantId, parsed)
                .stream().map(LimitChangeRequestResponse::from).toList();
    }

    @SecuredEndpoint(obj = "wallet.limit-requests", act = "approve")
    @PostMapping("/limit-requests/{requestId}/approve")
    @Operation(summary = "Approve a limit-change request — applies the new limits to the wallet")
    public ResponseEntity<LimitChangeRequestResponse> approve(
            @PathVariable UUID requestId,
            @RequestBody(required = false) DecideLimitChangeRequest body,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID decisionBy = extractUserId(jwt);
        String notes = body != null ? body.notes() : null;
        var result = decideUseCase.approve(new DecideWalletLimitChangeUseCase.ApproveCommand(
                tenantId, requestId, decisionBy, notes));
        return ResponseEntity.ok(LimitChangeRequestResponse.from(result));
    }

    @SecuredEndpoint(obj = "wallet.limit-requests", act = "approve")
    @PostMapping("/limit-requests/{requestId}/reject")
    @Operation(summary = "Reject a limit-change request")
    public ResponseEntity<LimitChangeRequestResponse> reject(
            @PathVariable UUID requestId,
            @RequestBody(required = false) DecideLimitChangeRequest body,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID decisionBy = extractUserId(jwt);
        String reason = body != null ? body.rejectionReason() : null;
        String notes = body != null ? body.notes() : null;
        var result = decideUseCase.reject(new DecideWalletLimitChangeUseCase.RejectCommand(
                tenantId, requestId, decisionBy, reason, notes));
        return ResponseEntity.ok(LimitChangeRequestResponse.from(result));
    }

    private LimitRequestStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return LimitRequestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("WALLET.LIMIT_REQUEST.INVALID_STATUS",
                    "Invalid status filter: " + status);
        }
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }

    private UUID extractUserId(Jwt jwt) {
        String sub = jwt.getSubject();
        try {
            return sub != null ? UUID.fromString(sub) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
