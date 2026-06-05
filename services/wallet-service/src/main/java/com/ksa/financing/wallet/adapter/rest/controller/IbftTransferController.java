package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.wallet.adapter.rest.support.AuthWalletResolver;
import com.ksa.financing.wallet.application.dto.IbftDtos.IbftResponse;
import com.ksa.financing.wallet.application.dto.IbftDtos.InitiateIbftRequest;
import com.ksa.financing.wallet.domain.model.IbftTransaction;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.InitiateIbftUseCase;
import com.ksa.financing.wallet.domain.port.out.IbftTransactionRepository;
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

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/ibft/transfers")
@RequiredArgsConstructor
@Tag(name = "IBFT Transfers", description = "Inter-bank funds transfers via Scotia EFT (HOLD-based)")
public class IbftTransferController {

    private final InitiateIbftUseCase initiateIbftUseCase;
    private final IbftTransactionRepository transactionRepository;
    private final AuthWalletResolver auth;

    @SecuredEndpoint(obj = "ibft.transfers", act = "create")
    @PostMapping
    @Operation(summary = "Initiate an IBFT transfer to a beneficiary (debtor = authenticated user)")
    public ResponseEntity<IbftResponse> initiate(
            @Valid @RequestBody InitiateIbftRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        Wallet wallet = auth.wallet(jwt);
        String idempotencyKey = idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()
                ? idempotencyKeyHeader : UUID.randomUUID().toString();

        IbftTransaction tx = initiateIbftUseCase.initiate(new InitiateIbftUseCase.InitiateIbftCommand(
                wallet.getTenantId(), wallet.getCustomerId(), wallet.getId(),
                request.beneficiaryId(),
                request.institutionNumber(), request.transit(), request.accountNumber(),
                request.beneficiaryName(), request.bankName(),
                request.amount(), request.currency(), request.purposeNote(),
                idempotencyKey, auth.userId(jwt), httpRequest.getRemoteAddr(), httpRequest.getHeader("X-Device-Id")));

        HttpStatus status = "FAILED".equals(tx.getStatus().name()) ? HttpStatus.UNPROCESSABLE_ENTITY
                : ("SUBMITTED".equals(tx.getStatus().name()) ? HttpStatus.ACCEPTED : HttpStatus.OK);
        return ResponseEntity.status(status).body(IbftResponse.from(tx));
    }

    @SecuredEndpoint(obj = "ibft.transfers", act = "read")
    @GetMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    @Operation(summary = "Get an IBFT transfer")
    public ResponseEntity<IbftResponse> get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = auth.tenantId(jwt);
        IbftTransaction tx = transactionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> NotFoundException.forEntity("IbftTransaction", id.toString()));
        return ResponseEntity.ok(IbftResponse.from(tx));
    }

    @SecuredEndpoint(obj = "ibft.transfers", act = "list")
    @GetMapping("/by-wallet/{walletId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    @Operation(summary = "List IBFT transfers for a wallet")
    public PageResponse<IbftResponse> listByWallet(@PathVariable UUID walletId, PageQuery query,
                                                   @AuthenticationPrincipal Jwt jwt) {
        auth.tenantId(jwt);
        return transactionRepository.findAllByWallet(walletId, query).map(IbftResponse::from);
    }
}
