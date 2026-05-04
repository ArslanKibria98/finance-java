package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.wallet.application.dto.InitiateTransferRequest;
import com.ksa.financing.wallet.application.dto.RecipientLookupResponse;
import com.ksa.financing.wallet.application.dto.TransferByMobileRequest;
import com.ksa.financing.wallet.application.dto.TransferResponse;
import com.ksa.financing.wallet.domain.model.WalletTransfer;
import com.ksa.financing.wallet.domain.port.in.GetTransferUseCase;
import com.ksa.financing.wallet.domain.port.in.InitiateTransferByMobileUseCase;
import com.ksa.financing.wallet.domain.port.in.InitiateTransferUseCase;
import com.ksa.financing.wallet.domain.port.in.LookupRecipientUseCase;
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
@RequestMapping("/api/v1/wallets/transfers")
@RequiredArgsConstructor
@Tag(name = "Wallet Transfers", description = "Wallet-to-wallet P2P transfer operations")
public class WalletTransferController {

    private final InitiateTransferUseCase initiateTransferUseCase;
    private final InitiateTransferByMobileUseCase initiateTransferByMobileUseCase;
    private final LookupRecipientUseCase lookupRecipientUseCase;
    private final GetTransferUseCase getTransferUseCase;

    @SecuredEndpoint(obj = "wallet.transfers", act = "create")
    @PostMapping
    @Operation(summary = "Initiate wallet-to-wallet transfer")
    public ResponseEntity<TransferResponse> initiate(
            @Valid @RequestBody InitiateTransferRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        UUID tenantId = extractTenantId(jwt);
        UUID userId = extractUserId(jwt);
        String idempotencyKey = idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()
                ? idempotencyKeyHeader
                : UUID.randomUUID().toString();

        log.info("Initiate transfer src={} dst={} amount={} key={} user={}",
                request.sourceWalletId(), request.destinationWalletId(),
                request.amount(), idempotencyKey, userId);

        WalletTransfer transfer = initiateTransferUseCase.initiate(
                new InitiateTransferUseCase.InitiateTransferCommand(
                        tenantId,
                        request.sourceWalletId(),
                        request.destinationWalletId(),
                        request.destinationWalletNumber(),
                        request.amount(),
                        request.currency(),
                        request.purposeNote(),
                        request.channel(),
                        idempotencyKey,
                        userId,
                        httpRequest.getRemoteAddr(),
                        httpRequest.getHeader("X-Device-Id")));

        return ResponseEntity
                .status("COMPLETED".equals(transfer.getStatus().name()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(toResponse(transfer));
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "read")
    @GetMapping("/{transferId}")
    @Operation(summary = "Get transfer by ID")
    public ResponseEntity<TransferResponse> getById(
            @PathVariable UUID transferId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        WalletTransfer transfer = getTransferUseCase.getById(tenantId, transferId);
        return ResponseEntity.ok(toResponse(transfer));
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "list")
    @GetMapping("/by-wallet/{walletId}")
    @Operation(summary = "List transfers for a wallet (sent + received)")
    public ResponseEntity<List<TransferResponse>> listByWallet(
            @PathVariable UUID walletId,
            @AuthenticationPrincipal Jwt jwt) {
        extractTenantId(jwt);
        List<WalletTransfer> list = getTransferUseCase.listByWallet(walletId);
        return ResponseEntity.ok(list.stream().map(this::toResponse).toList());
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "lookup")
    @GetMapping("/recipient")
    @Operation(summary = "Verify recipient by mobile number before initiating a transfer")
    public ResponseEntity<RecipientLookupResponse> lookupRecipient(
            @RequestParam("mobile") String mobile,
            @AuthenticationPrincipal Jwt jwt) {
        extractTenantId(jwt);
        var view = lookupRecipientUseCase.lookup(mobile);
        if (view.isEmpty()) {
            return ResponseEntity.ok(new RecipientLookupResponse(
                    false, null, null, null, null, null, null, null, false, "RECIPIENT_NOT_FOUND"));
        }
        var v = view.get();
        return ResponseEntity.ok(new RecipientLookupResponse(
                true,
                v.walletId(),
                v.walletNumber(),
                v.maskedName(),
                v.maskedMobile(),
                v.currency(),
                v.walletStatus() != null ? v.walletStatus().name() : null,
                v.userStatus(),
                v.canReceive(),
                v.reason()));
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "create")
    @PostMapping("/by-mobile")
    @Operation(summary = "Initiate transfer using only the receiver's mobile number (sender from JWT)")
    public ResponseEntity<TransferResponse> initiateByMobile(
            @Valid @RequestBody TransferByMobileRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        UUID senderKeycloakId = extractKeycloakUserId(jwt);
        String idempotencyKey = idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()
                ? idempotencyKeyHeader
                : UUID.randomUUID().toString();

        log.info("Initiate transfer-by-mobile sender={} receiverMobile=****{} amount={}",
                senderKeycloakId,
                request.receiverMobile() != null && request.receiverMobile().length() >= 4
                        ? request.receiverMobile().substring(request.receiverMobile().length() - 4)
                        : "?",
                request.amount());

        WalletTransfer transfer = initiateTransferByMobileUseCase.initiateByMobile(
                new InitiateTransferByMobileUseCase.InitiateByMobileCommand(
                        senderKeycloakId,
                        request.receiverMobile(),
                        request.amount(),
                        request.currency(),
                        request.purposeNote(),
                        idempotencyKey,
                        httpRequest.getRemoteAddr(),
                        httpRequest.getHeader("X-Device-Id")));

        return ResponseEntity
                .status("COMPLETED".equals(transfer.getStatus().name()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(toResponse(transfer));
    }

    private UUID extractKeycloakUserId(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "JWT missing subject (sub) claim");
        }
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "JWT subject is not a valid UUID");
        }
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

    private TransferResponse toResponse(WalletTransfer t) {
        return new TransferResponse(
                t.getId(),
                t.getTransferNumber(),
                t.getSourceWalletId(),
                t.getDestinationWalletId(),
                t.getAmount(),
                t.getFeeAmount(),
                t.getTotalDebit(),
                t.getCurrency(),
                t.getStatus() != null ? t.getStatus().name() : null,
                t.getChannel() != null ? t.getChannel().name() : null,
                t.getPurposeNote(),
                t.getDebitMovementId(),
                t.getCreditMovementId(),
                t.getErrorCode(),
                t.getErrorMessage(),
                t.getInitiatedAt(),
                t.getCompletedAt());
    }
}
