package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.wallet.application.dto.TopUpRequest;
import com.ksa.financing.wallet.application.dto.TopUpResponse;
import com.ksa.financing.wallet.application.dto.WalletResponse;
import com.ksa.financing.wallet.domain.model.TopUpTransaction;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.CreateWalletUseCase;
import com.ksa.financing.wallet.domain.port.in.GetBalanceUseCase;
import com.ksa.financing.wallet.domain.port.in.TopUpUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Wallet management and top-up operations")
public class WalletController {

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);

    private final CreateWalletUseCase createWalletUseCase;
    private final GetBalanceUseCase getBalanceUseCase;
    private final TopUpUseCase topUpUseCase;

    @SecuredEndpoint(obj = "wallets", act = "read")
    @GetMapping("/by-customer/{customerId}")
    @Operation(summary = "Get wallet by customer ID")
    public ResponseEntity<WalletResponse> getByCustomerId(
            @PathVariable UUID customerId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Getting wallet for customer: {} tenant: {}", customerId, tenantId);

        Wallet wallet = getBalanceUseCase.getByCustomerId(tenantId, customerId);
        return ResponseEntity.ok(toResponse(wallet));
    }

    @SecuredEndpoint(obj = "wallets", act = "read")
    @GetMapping("/{walletId}/balance")
    @Operation(summary = "Get wallet balance by wallet ID")
    public ResponseEntity<WalletResponse> getByWalletId(
            @PathVariable UUID walletId,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Getting balance for wallet: {}", walletId);

        Wallet wallet = getBalanceUseCase.getByWalletId(walletId);
        return ResponseEntity.ok(toResponse(wallet));
    }

    @SecuredEndpoint(obj = "wallets", act = "manage")
    @PostMapping("/{walletId}/top-up")
    @Operation(summary = "Top up wallet balance")
    public ResponseEntity<TopUpResponse> topUp(
            @PathVariable UUID walletId,
            @Valid @RequestBody TopUpRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Processing top-up for wallet: {} amount: {} method: {}", walletId, request.amount(), request.method());

        TopUpTransaction transaction = topUpUseCase.topUp(new TopUpUseCase.TopUpCommand(
                tenantId,
                walletId,
                request.amount(),
                request.method(),
                request.sourceIban(),
                request.idempotencyKey()
        ));

        return ResponseEntity.ok(toTopUpResponse(transaction));
    }

    @SecuredEndpoint(obj = "wallets", act = "create")
    @PostMapping("/create")
    @Operation(summary = "Create wallet for customer (internal endpoint)")
    public ResponseEntity<WalletResponse> createWallet(
            @RequestBody CreateWalletRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Creating wallet for customer: {} tenant: {}", request.customerId(), tenantId);

        Wallet wallet = createWalletUseCase.create(new CreateWalletUseCase.CreateWalletCommand(
                tenantId,
                request.customerId(),
                request.currency()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(wallet));
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getWalletNumber(),
                wallet.getCustomerId(),
                wallet.getAvailableBalance(),
                wallet.getReservedBalance(),
                wallet.getTotalBalance(),
                wallet.getCurrency(),
                wallet.getStatus() != null ? wallet.getStatus().name() : null,
                wallet.isAutoDebitEnabled(),
                wallet.getFineractSavingsAccountId(),
                wallet.isLedgerSynced(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    private TopUpResponse toTopUpResponse(TopUpTransaction txn) {
        return new TopUpResponse(
                txn.getId(),
                txn.getTransactionNumber(),
                txn.getMethod() != null ? txn.getMethod().name() : null,
                txn.getAmount(),
                txn.getFeeAmount(),
                txn.getNetAmount(),
                txn.getStatus() != null ? txn.getStatus().name() : null,
                txn.getInitiatedAt(),
                txn.getCompletedAt()
        );
    }

    public record CreateWalletRequest(
            UUID customerId,
            String currency
    ) {}
}
