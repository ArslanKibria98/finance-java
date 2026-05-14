package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.wallet.application.dto.InitiateWithdrawalRequest;
import com.ksa.financing.wallet.application.dto.WithdrawalResponse;
import com.ksa.financing.wallet.domain.model.WalletWithdrawal;
import com.ksa.financing.wallet.domain.port.in.GetWithdrawalUseCase;
import com.ksa.financing.wallet.domain.port.in.InitiateWithdrawalUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/wallets/withdrawals")
@RequiredArgsConstructor
@Tag(name = "Wallet Withdrawals", description = "Outbound cash-out from wallet to bank IBAN")
public class WalletWithdrawalController {

    private final InitiateWithdrawalUseCase initiateUseCase;
    private final GetWithdrawalUseCase getUseCase;

    @SecuredEndpoint(obj = "wallet.withdrawals", act = "create")
    @PostMapping
    @Operation(summary = "Initiate a wallet-to-bank withdrawal (cash-out)")
    public ResponseEntity<WithdrawalResponse> initiate(
            @Valid @RequestBody InitiateWithdrawalRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        UUID tenantId = extractTenantId(jwt);
        UUID userId = extractUserId(jwt);
        String idempotencyKey = idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()
                ? idempotencyKeyHeader
                : UUID.randomUUID().toString();

        log.info("Initiate withdrawal walletId={} amount={} key={} user={}",
                request.sourceWalletId(), request.amount(), idempotencyKey, userId);

        WalletWithdrawal result = initiateUseCase.initiate(
                new InitiateWithdrawalUseCase.InitiateWithdrawalCommand(
                        tenantId,
                        request.sourceWalletId(),
                        request.beneficiaryId(),
                        request.destinationIban(),
                        request.beneficiaryName(),
                        request.destinationBankCode(),
                        request.destinationBankName(),
                        request.amount(),
                        request.currency(),
                        request.channel(),
                        request.purposeNote(),
                        request.purposeCode(),
                        request.chargeBearer(),
                        request.serviceLevel(),
                        idempotencyKey,
                        userId,
                        httpRequest.getRemoteAddr(),
                        httpRequest.getHeader("X-Device-Id")));

        return ResponseEntity
                .status("COMPLETED".equals(result.getStatus().name()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(toResponse(result));
    }

    @SecuredEndpoint(obj = "wallet.withdrawals", act = "read")
    @GetMapping("/{withdrawalId}")
    @Operation(summary = "Get withdrawal by ID")
    public ResponseEntity<WithdrawalResponse> getById(
            @PathVariable UUID withdrawalId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(toResponse(getUseCase.getById(tenantId, withdrawalId)));
    }

    @SecuredEndpoint(obj = "wallet.withdrawals", act = "list")
    @GetMapping("/by-wallet/{walletId}")
    @Operation(summary = "List withdrawals for a wallet. Supports query params: page, size, search (withdrawalNumber, destinationIban, destinationBankName, destinationCountry, beneficiaryName, channel, status).")
    public PageResponse<WithdrawalResponse> listByWallet(
            @PathVariable UUID walletId,
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {
        extractTenantId(jwt);
        var page = getUseCase.listByWallet(walletId, query);
        return page.map(this::toResponse);
    }

    private static boolean contains(String f, String term) {
        return f != null && f.toLowerCase().contains(term);
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
