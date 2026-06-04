package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.wallet.application.dto.LimitChangeRequestResponse;
import com.ksa.financing.wallet.application.dto.RequestLimitChangeRequest;
import com.ksa.financing.wallet.application.dto.WalletLimitResponse;
import com.ksa.financing.wallet.domain.port.in.GetWalletLimitUseCase;
import com.ksa.financing.wallet.domain.port.in.RequestWalletLimitChangeUseCase;
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

/**
 * Customer-facing wallet transaction-limit endpoints: read current limits + usage, and
 * raise a limit-change request (which an admin must approve before it takes effect).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet Limits", description = "Wallet transaction-limit read + change requests")
public class WalletLimitController {

    private final GetWalletLimitUseCase getWalletLimitUseCase;
    private final RequestWalletLimitChangeUseCase requestUseCase;

    @SecuredEndpoint(obj = "wallets.limits", act = "read")
    @GetMapping("/{walletId}/limits")
    @Operation(summary = "Get wallet's daily/monthly transaction limits with current usage")
    public ResponseEntity<WalletLimitResponse> getLimits(
            @PathVariable UUID walletId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(
                WalletLimitResponse.from(getWalletLimitUseCase.getLimit(tenantId, walletId)));
    }

    @SecuredEndpoint(obj = "wallets.limits", act = "request")
    @PostMapping("/{walletId}/limit-requests")
    @Operation(summary = "Request a change to the wallet's transaction limits (requires admin approval)")
    public ResponseEntity<LimitChangeRequestResponse> requestChange(
            @PathVariable UUID walletId,
            @Valid @RequestBody RequestLimitChangeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID requestedBy = extractUserId(jwt);
        log.info("Limit-change request wallet={} daily={} monthly={} yearly={} by={}",
                walletId, request.requestedDailyLimit(), request.requestedMonthlyLimit(),
                request.requestedYearlyLimit(), requestedBy);

        var created = requestUseCase.request(new RequestWalletLimitChangeUseCase.RequestCommand(
                tenantId, walletId,
                request.requestedDailyLimit(), request.requestedMonthlyLimit(), request.requestedYearlyLimit(),
                request.reason(), requestedBy));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LimitChangeRequestResponse.from(created));
    }

    @SecuredEndpoint(obj = "wallets.limits", act = "read")
    @GetMapping("/{walletId}/limit-requests")
    @Operation(summary = "List this wallet's limit-change requests")
    public List<LimitChangeRequestResponse> listRequests(
            @PathVariable UUID walletId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return requestUseCase.listForWallet(tenantId, walletId)
                .stream().map(LimitChangeRequestResponse::from).toList();
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
