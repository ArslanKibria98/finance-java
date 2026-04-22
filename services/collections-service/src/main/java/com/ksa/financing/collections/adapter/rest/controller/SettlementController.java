package com.ksa.financing.collections.adapter.rest.controller;

import com.ksa.financing.collections.adapter.rest.request.InitiateSettlementRequest;
import com.ksa.financing.collections.adapter.rest.response.SettlementQuoteResponse;
import com.ksa.financing.collections.domain.port.in.ManageSettlementUseCase;
import com.ksa.financing.collections.domain.port.in.ManageSettlementUseCase.*;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Settlements", description = "Early and partial settlement with Sharia Ibra")
public class SettlementController {

    private final ManageSettlementUseCase settlementUseCase;

    @GetMapping("/quote/{loanId}")
    @Operation(summary = "Get settlement quote with Ibra calculation (Sharia: 100% unearned profit waiver)")
    @SecuredEndpoint(obj = "settlements", act = "read")
    public ResponseEntity<SettlementQuoteResponse> getSettlementQuote(
            @PathVariable UUID loanId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var quote = settlementUseCase.getSettlementQuote(tenantId, loanId);
        return ResponseEntity.ok(toQuoteResponse(quote));
    }

    @PostMapping
    @Operation(summary = "Initiate an early or partial settlement")
    @SecuredEndpoint(obj = "settlements", act = "create")
    public ResponseEntity<SettlementResponse> initiateSettlement(
            @Valid @RequestBody InitiateSettlementRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID customerId = UUID.fromString(jwt.getSubject());

        var command = new InitiateSettlementCommand(
                tenantId,
                request.loanId(),
                customerId,
                request.settlementType(),
                request.settlementAmount(),
                request.idempotencyKey(),
                customerId
        );

        var response = settlementUseCase.initiateSettlement(command);
        log.info("Settlement initiated: settlementId={} loanId={}", response.settlementId(), request.loanId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{settlementId}/confirm")
    @Operation(summary = "Confirm a settlement after payment completion")
    @SecuredEndpoint(obj = "settlements", act = "update")
    public ResponseEntity<SettlementResponse> confirmSettlement(
            @PathVariable UUID settlementId,
            @RequestParam UUID paymentId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var response = settlementUseCase.confirmSettlement(tenantId, settlementId, paymentId);
        log.info("Settlement confirmed: settlementId={}", settlementId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{settlementId}")
    @Operation(summary = "Get settlement details")
    @SecuredEndpoint(obj = "settlements", act = "read")
    public ResponseEntity<SettlementResponse> getSettlement(
            @PathVariable UUID settlementId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var response = settlementUseCase.getSettlement(tenantId, settlementId);
        return ResponseEntity.ok(response);
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException("COMMON.AUTH.ACCESS_DENIED", "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantId);
    }

    private SettlementQuoteResponse toQuoteResponse(SettlementQuote quote) {
        return new SettlementQuoteResponse(
                quote.loanId(),
                null,
                quote.quoteDate(),
                quote.outstandingPrincipal(),
                quote.outstandingProfit(),
                quote.ibraAmount(),
                quote.ibraAmount(),
                quote.earlySettlementDiscount(),
                quote.outstandingFees(),
                quote.settlementAmount(),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(24)
        );
    }
}
