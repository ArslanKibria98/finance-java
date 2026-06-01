package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.wallet.domain.model.TransactionPurpose;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.CreateWalletUseCase;
import com.ksa.financing.wallet.domain.port.in.CreditWalletUseCase;
import com.ksa.financing.wallet.domain.port.in.DebitWalletUseCase;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Internal endpoints for service-to-service wallet operations.
 * No JWT required — protected by network policy / API gateway.
 *
 * Used by:
 *  - customer-service → wallet IBAN lookup (profile API)
 *  - lending-service  → credit wallet on loan disbursement
 */
@RestController
@RequestMapping("/internal/wallets")
@RequiredArgsConstructor
@Slf4j
public class InternalWalletController {

    private final WalletRepository walletRepository;
    private final CreditWalletUseCase creditWalletUseCase;
    private final DebitWalletUseCase debitWalletUseCase;
    private final CreateWalletUseCase createWalletUseCase;

    /**
     * Create a wallet for a freshly-onboarded customer.
     * Called by onboarding-workflow-service after the customer record is created.
     */
    @PostMapping
    public ResponseEntity<CreateWalletResponse> createWalletInternal(
            @Valid @RequestBody CreateWalletInternalRequest request) {
        log.info("Internal: Creating wallet for customer={} tenant={} currency={}",
                request.customerId(), request.tenantId(), request.currency());

        Wallet wallet = createWalletUseCase.create(new CreateWalletUseCase.CreateWalletCommand(
                request.tenantId(),
                request.customerId(),
                request.currency() != null ? request.currency() : "SAR",
                request.iban(),
                request.displayName()
        ));

        return ResponseEntity.ok(new CreateWalletResponse(
                wallet.getId(),
                wallet.getCustomerId(),
                wallet.getWalletNumber(),
                wallet.getIban(),
                wallet.getCurrency()
        ));
    }

    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<WalletIbanResponse> getWalletIbanByCustomerId(
            @PathVariable UUID customerId,
            @RequestHeader(value = "X-Tenant-Id") String tenantId) {

        log.info("Internal: Getting wallet IBAN for customer: {} tenant: {}", customerId, tenantId);

        UUID tenantUuid = parseTenant(tenantId);

        return walletRepository.findByCustomerId(tenantUuid, customerId)
                .map(wallet -> ResponseEntity.ok(new WalletIbanResponse(
                        wallet.getId(),
                        wallet.getCustomerId(),
                        wallet.getWalletNumber(),
                        wallet.getIban()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Credit the customer's wallet from an internal source (loan disbursement, refund, etc.).
     * Funds are deposited into the Fineract savings account backing the wallet.
     */
    @PostMapping("/credit-from-loan")
    public ResponseEntity<CreditWalletUseCase.CreditResult> creditFromLoan(
            @Valid @RequestBody CreditWalletRequest request,
            @RequestHeader(value = "X-Tenant-Id") String tenantId) {

        UUID tenantUuid = parseTenant(tenantId);
        log.info("Internal: Crediting wallet for customer={} amount={} ref={} idempotency={}",
                request.customerId(), request.amount(), request.referenceId(), request.idempotencyKey());

        var result = creditWalletUseCase.credit(new CreditWalletUseCase.CreditCommand(
                tenantUuid,
                request.customerId(),
                request.amount(),
                request.purpose() != null ? request.purpose() : TransactionPurpose.LOAN_PROCEEDS,
                request.referenceType(),
                request.referenceId(),
                request.description(),
                request.idempotencyKey()
        ));

        return ResponseEntity.ok(result);
    }

    /**
     * Debit the customer's wallet for an internal operation (loan repayment, fee, etc.).
     * Funds are withdrawn from the Fineract savings account backing the wallet.
     * Returns 422 (INSUFFICIENT_FUNDS) when the wallet balance is below the requested amount.
     */
    @PostMapping("/debit-for-loan")
    public ResponseEntity<DebitWalletUseCase.DebitResult> debitForLoan(
            @Valid @RequestBody DebitWalletRequest request,
            @RequestHeader(value = "X-Tenant-Id") String tenantId) {

        UUID tenantUuid = parseTenant(tenantId);
        log.info("Internal: Debiting wallet for customer={} amount={} ref={} idempotency={}",
                request.customerId(), request.amount(), request.referenceId(), request.idempotencyKey());

        var result = debitWalletUseCase.debit(new DebitWalletUseCase.DebitCommand(
                tenantUuid,
                request.customerId(),
                request.amount(),
                request.purpose() != null ? request.purpose() : TransactionPurpose.INSTALLMENT_PAYMENT,
                request.referenceType(),
                request.referenceId(),
                request.description(),
                request.idempotencyKey()
        ));

        return ResponseEntity.ok(result);
    }

    private UUID parseTenant(String tenantId) {
        try {
            return UUID.fromString(tenantId);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(tenantId.getBytes());
        }
    }

    public record WalletIbanResponse(
            UUID walletId,
            UUID customerId,
            String walletNumber,
            String iban
    ) {}

    public record CreditWalletRequest(
            @NotNull UUID customerId,
            @NotNull @Positive BigDecimal amount,
            TransactionPurpose purpose,
            String referenceType,
            UUID referenceId,
            String description,
            @NotNull String idempotencyKey
    ) {}

    public record DebitWalletRequest(
            @NotNull UUID customerId,
            @NotNull @Positive BigDecimal amount,
            TransactionPurpose purpose,
            String referenceType,
            UUID referenceId,
            String description,
            @NotNull String idempotencyKey
    ) {}

    public record CreateWalletInternalRequest(
            @NotNull UUID tenantId,
            @NotNull UUID customerId,
            String currency,
            String iban,
            String displayName
    ) {}

    public record CreateWalletResponse(
            UUID walletId,
            UUID customerId,
            String walletNumber,
            String iban,
            String currency
    ) {}
}
