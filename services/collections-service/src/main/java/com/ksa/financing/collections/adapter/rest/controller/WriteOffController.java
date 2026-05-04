package com.ksa.financing.collections.adapter.rest.controller;

import com.ksa.financing.collections.adapter.rest.request.ExecuteWriteOffRequest;
import com.ksa.financing.collections.adapter.rest.request.ReverseWriteOffRequest;
import com.ksa.financing.collections.adapter.rest.response.WriteOffEligibilityResponse;
import com.ksa.financing.collections.adapter.rest.response.WriteOffResponse;
import com.ksa.financing.collections.domain.model.WriteOffRecord;
import com.ksa.financing.collections.domain.model.WriteOffTriggerType;
import com.ksa.financing.collections.domain.port.in.ManageWriteOffUseCase;
import com.ksa.financing.collections.domain.port.in.ManageWriteOffUseCase.ExecuteWriteOffCommand;
import com.ksa.financing.collections.domain.port.in.ManageWriteOffUseCase.ReverseWriteOffCommand;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/write-offs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Write-Offs",
        description = "Delinquency-based write-off workflow. Eligibility derives from the per-product "
                + "WRITE_OFFS DelinquencyRule (type=4). Admins can evaluate, execute, reverse, and report.")
public class WriteOffController {

    private final ManageWriteOffUseCase writeOffUseCase;

    @PostMapping("/evaluate")
    @Operation(summary = "Re-evaluate write-off eligibility for all installments on a loan against the WRITE_OFFS rule")
    @SecuredEndpoint(obj = "write-offs", act = "manage")
    public ResponseEntity<WriteOffEligibilityResponse> evaluate(
            @RequestParam UUID loanId,
            @RequestParam(required = false) LocalDate asOf,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        LocalDate effectiveDate = asOf != null ? asOf : LocalDate.now();
        int eligible = writeOffUseCase.evaluateEligibility(tenantId, loanId, effectiveDate);
        log.info("Write-off eligibility evaluated: tenant={} loan={} asOf={} eligibleCount={}",
                tenantId, loanId, effectiveDate, eligible);
        return ResponseEntity.ok(new WriteOffEligibilityResponse(loanId, eligible, effectiveDate.toString()));
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute write-off on eligible installments (or a specific installment via installmentId)")
    @SecuredEndpoint(obj = "write-offs", act = "manage")
    public ResponseEntity<List<WriteOffResponse>> execute(
            @Valid @RequestBody ExecuteWriteOffRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID actorId = extractActorId(jwt);
        WriteOffTriggerType trigger = parseTrigger(request.triggerType());
        boolean override = Boolean.TRUE.equals(request.override());

        var cmd = new ExecuteWriteOffCommand(
                tenantId,
                request.loanId(),
                request.installmentId(),
                request.invoiceId(),
                request.reason(),
                request.approvalReference(),
                trigger,
                override,
                request.asOf() != null ? request.asOf() : LocalDate.now(),
                actorId);

        var records = writeOffUseCase.executeWriteOff(cmd);
        log.info("Write-off executed: tenant={} loan={} count={} override={} actor={}",
                tenantId, request.loanId(), records.size(), override, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                records.stream().map(this::toResponse).toList());
    }

    @PostMapping("/{writeOffId}/reverse")
    @Operation(summary = "Reverse an active write-off (e.g., late recovery)")
    @SecuredEndpoint(obj = "write-offs", act = "manage")
    public ResponseEntity<WriteOffResponse> reverse(
            @PathVariable UUID writeOffId,
            @Valid @RequestBody ReverseWriteOffRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID actorId = extractActorId(jwt);
        var cmd = new ReverseWriteOffCommand(tenantId, writeOffId, request.reason(), actorId);
        var record = writeOffUseCase.reverseWriteOff(cmd);
        log.info("Write-off reversed: id={} actor={}", writeOffId, actorId);
        return ResponseEntity.ok(toResponse(record));
    }

    @GetMapping("/{writeOffId}")
    @Operation(summary = "Get a write-off record by id")
    @SecuredEndpoint(obj = "write-offs", act = "read")
    public ResponseEntity<WriteOffResponse> get(
            @PathVariable UUID writeOffId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(toResponse(writeOffUseCase.getWriteOff(tenantId, writeOffId)));
    }

    @GetMapping
    @Operation(summary = "List write-off records (paginated)")
    @SecuredEndpoint(obj = "write-offs", act = "read")
    public PageResponse<WriteOffResponse> list(
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        return writeOffUseCase.listByTenant(tenantId, query).map(this::toResponse);
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

    private WriteOffTriggerType parseTrigger(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return WriteOffTriggerType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                    "Unsupported triggerType: " + raw);
        }
    }

    private WriteOffResponse toResponse(WriteOffRecord r) {
        return new WriteOffResponse(
                r.getId().getValue(),
                r.getTenantId(),
                r.getLoanId(),
                r.getScheduleId(),
                r.getInstallmentId(),
                r.getDelinquencyRuleId(),
                r.getPrincipalAmount(),
                r.getProfitAmount(),
                r.getFeeAmount(),
                r.getPenaltyAmount(),
                r.getTotalAmount(),
                r.getDpdAtWriteOff(),
                r.getTriggerType().name(),
                r.getReason(),
                r.getApprovalReference(),
                r.getStatus().name(),
                r.getReversalReason(),
                r.getReversedAt(),
                r.getReversedBy(),
                r.getWriteOffDate(),
                r.getInitiatedBy(),
                r.getCreatedAt());
    }
}
