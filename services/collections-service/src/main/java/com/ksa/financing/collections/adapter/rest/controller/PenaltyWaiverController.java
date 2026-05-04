package com.ksa.financing.collections.adapter.rest.controller;

import com.ksa.financing.collections.adapter.rest.request.WaivePenaltyRequest;
import com.ksa.financing.collections.adapter.rest.response.PenaltyWaiverResponse;
import com.ksa.financing.collections.domain.model.PenaltyWaiver;
import com.ksa.financing.collections.domain.port.in.WaivePenaltyUseCase;
import com.ksa.financing.collections.domain.port.in.WaivePenaltyUseCase.WaivePenaltyCommand;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/penalty-waivers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Penalty Waivers",
        description = "Admin-driven waiver of accrued late-payment penalties. Full or partial waivers supported; "
                + "each action is recorded in the penalty_waivers audit table.")
public class PenaltyWaiverController {

    private final WaivePenaltyUseCase waiverUseCase;

    @PostMapping
    @Operation(summary = "Waive penalty on an installment (amount null ⇒ waive full remaining penalty)")
    @SecuredEndpoint(obj = "penalty-waivers", act = "manage")
    public ResponseEntity<PenaltyWaiverResponse> waive(
            @Valid @RequestBody WaivePenaltyRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID actorId = extractActorId(jwt);

        var cmd = new WaivePenaltyCommand(
                tenantId,
                request.loanId(),
                request.installmentId(),
                request.amount(),
                request.reason(),
                request.approvalReference(),
                actorId);

        var waiver = waiverUseCase.waivePenalty(cmd);
        log.info("Penalty waived: tenant={} loan={} installment={} waiverId={} type={} actor={}",
                tenantId, request.loanId(), request.installmentId(), waiver.getId().getValue(),
                waiver.getWaiverType(), actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(waiver));
    }

    @GetMapping("/{waiverId}")
    @Operation(summary = "Get a penalty waiver by id")
    @SecuredEndpoint(obj = "penalty-waivers", act = "read")
    public ResponseEntity<PenaltyWaiverResponse> get(
            @PathVariable UUID waiverId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(toResponse(waiverUseCase.getWaiver(tenantId, waiverId)));
    }

    @GetMapping
    @Operation(summary = "List penalty waivers — filter by loanId, installmentId, or waivedAt date range")
    @SecuredEndpoint(obj = "penalty-waivers", act = "read")
    public ResponseEntity<List<PenaltyWaiverResponse>> list(
            @RequestParam(required = false) UUID loanId,
            @RequestParam(required = false) UUID installmentId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        List<PenaltyWaiver> waivers;
        if (installmentId != null) {
            waivers = waiverUseCase.listByInstallment(tenantId, installmentId);
        } else if (loanId != null) {
            waivers = waiverUseCase.listByLoan(tenantId, loanId);
        } else if (fromDate != null && toDate != null) {
            waivers = waiverUseCase.listByDateRange(tenantId, fromDate, toDate);
        } else {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                    "Provide one filter: loanId, installmentId, or fromDate+toDate");
        }
        return ResponseEntity.ok(waivers.stream().map(this::toResponse).toList());
    }

    // ─────────────────────────────────────────────

    private UUID extractTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantId);
    }

    private UUID extractActorId(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "No subject (sub) claim found in JWT token");
        }
        return UUID.fromString(subject);
    }

    private PenaltyWaiverResponse toResponse(PenaltyWaiver w) {
        return new PenaltyWaiverResponse(
                w.getId().getValue(),
                w.getTenantId(),
                w.getLoanId(),
                w.getInstallmentId(),
                w.getOriginalPenalty(),
                w.getWaivedAmount(),
                w.getRemainingPenalty(),
                w.getWaiverType().name(),
                w.getReason(),
                w.getApprovalReference(),
                w.getWaivedBy(),
                w.getWaivedAt());
    }
}
