package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.wallet.application.dto.ReleaseHeldWithdrawalRequest;
import com.ksa.financing.wallet.application.dto.WithdrawalResponse;
import com.ksa.financing.wallet.domain.model.WalletWithdrawal;
import com.ksa.financing.wallet.domain.port.in.ReleaseHeldWithdrawalUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin / compliance-officer endpoints for withdrawal review.
 * <p>
 * Currently only the AML-hold release endpoint. Future: manual reversal,
 * dual-approval flows.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/wallets/withdrawals")
@RequiredArgsConstructor
@Tag(name = "Admin Withdrawals", description = "Compliance officer / admin actions on withdrawals")
public class AdminWithdrawalController {

    private final ReleaseHeldWithdrawalUseCase releaseUseCase;

    @SecuredEndpoint(obj = "wallet.withdrawals.aml-review", act = "approve")
    @PostMapping("/{withdrawalId}/release")
    @Operation(summary = "Release a HELD_AML withdrawal — resumes SAGA, debits + submits to bank")
    public ResponseEntity<WithdrawalResponse> release(
            @PathVariable UUID withdrawalId,
            @Valid @RequestBody ReleaseHeldWithdrawalRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID reviewerId = extractUserId(jwt);
        log.info("AML release withdrawalId={} reviewer={} reason={}",
                withdrawalId, reviewerId, request.reason());

        WalletWithdrawal released = releaseUseCase.release(
                new ReleaseHeldWithdrawalUseCase.ReleaseCommand(
                        tenantId, withdrawalId, reviewerId, request.reason()));

        return ResponseEntity.ok(toResponse(released));
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
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

    private WithdrawalResponse toResponse(WalletWithdrawal w) {
        return new WithdrawalResponse(
                w.getId(),
                w.getWithdrawalNumber(),
                w.getSourceWalletId(),
                w.getDestinationIban(),
                w.getDestinationBankName(),
                w.getDestinationCountry(),
                w.getBeneficiaryName(),
                w.getAmount(),
                w.getFeeAmount(),
                w.getTotalDebit(),
                w.getCurrency(),
                w.getChannel() != null ? w.getChannel().name() : null,
                w.getStatus() != null ? w.getStatus().name() : null,
                w.getPurposeNote(),
                w.getPurposeCode() != null ? w.getPurposeCode().name() : null,
                w.getChargeBearer() != null ? w.getChargeBearer().name() : null,
                w.getServiceLevel() != null ? w.getServiceLevel().name() : null,
                w.getEndToEndId(),
                w.getUetr(),
                w.getScreeningRef(),
                w.getScreeningDecision(),
                w.getScreeningScore(),
                w.isEddRequired(),
                w.getFineractDebitTxnId(),
                w.getBankReference(),
                w.getSarieReference(),
                w.getErrorCode(),
                w.getErrorMessage(),
                w.getInitiatedAt(),
                w.getScreenedAt(),
                w.getDebitedAt(),
                w.getBankSubmittedAt(),
                w.getCompletedAt());
    }
}
