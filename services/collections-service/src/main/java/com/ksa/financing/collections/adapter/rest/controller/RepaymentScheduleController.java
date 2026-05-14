package com.ksa.financing.collections.adapter.rest.controller;

import com.ksa.financing.collections.adapter.rest.request.CreateRepaymentScheduleRequest;
import com.ksa.financing.collections.adapter.rest.response.InstallmentResponse;
import com.ksa.financing.collections.adapter.rest.response.RepaymentScheduleResponse;
import com.ksa.financing.collections.application.service.DelinquencyRulesResolver;
import com.ksa.financing.collections.application.service.EarlySettlementMatcher;
import com.ksa.financing.collections.domain.model.DelinquencyRule;
import com.ksa.financing.collections.domain.model.DelinquencyType;
import com.ksa.financing.collections.domain.model.Installment;
import com.ksa.financing.collections.domain.model.PenaltyWaiverRequest;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.port.in.ManageRepaymentScheduleUseCase;
import com.ksa.financing.collections.domain.port.in.ManageRepaymentScheduleUseCase.CreateScheduleCommand;
import com.ksa.financing.collections.domain.port.out.PenaltyWaiverRequestRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repayment-schedules")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Repayment Schedules", description = "Installment repayment schedule management")
public class RepaymentScheduleController {

    private final ManageRepaymentScheduleUseCase scheduleUseCase;
    private final DelinquencyRulesResolver rulesResolver;
    private final EarlySettlementMatcher matcher;
    private final PenaltyWaiverRequestRepository penaltyWaiverRequestRepository;

    @PostMapping
    @Operation(summary = "Create a repayment schedule for a loan")
    @SecuredEndpoint(obj = "repayment-schedules", act = "create")
    public ResponseEntity<RepaymentScheduleResponse> createSchedule(
            @Valid @RequestBody CreateRepaymentScheduleRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID userId = UUID.fromString(jwt.getSubject());

        BigDecimal resolvedTotalFee = request.totalFee() != null
                ? request.totalFee()
                : request.installments().stream()
                        .map(i -> i.feeAmount() != null ? i.feeAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        var command = new CreateScheduleCommand(
                tenantId,
                request.loanId(),
                request.productId(),
                request.scheduleNumber(),
                request.totalPrincipal(),
                request.totalProfit(),
                resolvedTotalFee,
                request.firstDueDate(),
                request.lastDueDate(),
                request.installments().stream()
                        .map(i -> new CreateScheduleCommand.InstallmentEntry(
                                i.installmentNumber(),
                                i.dueDate(),
                                i.principalAmount(),
                                i.profitAmount(),
                                i.feeAmount()))
                        .toList(),
                userId
        );

        var schedule = scheduleUseCase.createSchedule(command);
        log.info("Repayment schedule created: scheduleId={} loanId={} productId={}",
                schedule.getId().getValue(), request.loanId(), request.productId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tenantId, schedule, true));
    }

    @GetMapping("/{scheduleId}")
    @Operation(summary = "Get a repayment schedule by ID")
    @SecuredEndpoint(obj = "repayment-schedules", act = "read")
    public ResponseEntity<RepaymentScheduleResponse> getSchedule(
            @PathVariable UUID scheduleId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var schedule = scheduleUseCase.getSchedule(tenantId, scheduleId);
        return ResponseEntity.ok(toResponse(tenantId, schedule, true));
    }

    @GetMapping("/by-loan/{loanId}")
    @Operation(summary = "Get repayment schedule by loan ID")
    @SecuredEndpoint(obj = "repayment-schedules", act = "read")
    public ResponseEntity<RepaymentScheduleResponse> getScheduleByLoan(
            @PathVariable UUID loanId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var schedule = scheduleUseCase.getScheduleByLoan(tenantId, loanId);
        return ResponseEntity.ok(toResponse(tenantId, schedule, true));
    }

    @GetMapping("/{scheduleId}/installments")
    @Operation(summary = "Get installments for a schedule")
    @SecuredEndpoint(obj = "repayment-schedules", act = "read")
    public ResponseEntity<List<InstallmentResponse>> getInstallments(
            @PathVariable UUID scheduleId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var schedule = scheduleUseCase.getSchedule(tenantId, scheduleId);
        DelinquencyRule esRule = rulesResolver.rule(tenantId, schedule.getProductId(),
                DelinquencyType.EARLY_SETTLEMENT).orElse(null);
        LocalDate today = LocalDate.now();
        Map<UUID, PenaltyWaiverRequest> pendingWaivers = loadPendingWaivers(tenantId, schedule.getLoanId());
        return ResponseEntity.ok(schedule.getInstallments().stream()
                .map(i -> toInstallmentResponse(i, esRule, today, pendingWaivers))
                .toList());
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new com.ksa.financing.infra.exception.BusinessException(
                    "COMMON.AUTH.ACCESS_DENIED", "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantId);
    }

    private RepaymentScheduleResponse toResponse(UUID tenantId, RepaymentScheduleAggregate schedule, boolean includeInstallments) {
        DelinquencyRule esRule = includeInstallments
                ? rulesResolver.rule(tenantId, schedule.getProductId(), DelinquencyType.EARLY_SETTLEMENT).orElse(null)
                : null;
        LocalDate today = LocalDate.now();
        Map<UUID, PenaltyWaiverRequest> pendingWaivers = includeInstallments
                ? loadPendingWaivers(tenantId, schedule.getLoanId())
                : Map.of();
        var installments = includeInstallments
                ? schedule.getInstallments().stream()
                        .map(i -> toInstallmentResponse(i, esRule, today, pendingWaivers))
                        .toList()
                : List.<InstallmentResponse>of();

        return new RepaymentScheduleResponse(
                schedule.getId().getValue(),
                schedule.getLoanId(),
                schedule.getScheduleNumber(),
                schedule.getTotalPrincipal(),
                schedule.getTotalProfit(),
                schedule.getTotalFee(),
                schedule.getTotalAmount(),
                schedule.getPaidPrincipal(),
                schedule.getPaidProfit(),
                schedule.getPaidTotal(),
                schedule.getFirstDueDate(),
                schedule.getLastDueDate(),
                schedule.isFullyPaid(),
                installments.stream().anyMatch(InstallmentResponse::earlySettlementEligible),
                schedule.getCreatedAt(),
                installments
        );
    }

    private InstallmentResponse toInstallmentResponse(Installment i, DelinquencyRule esRule, LocalDate today,
                                                      Map<UUID, PenaltyWaiverRequest> pendingWaivers) {
        BigDecimal waived = i.getWaivedPenaltyAmount() != null ? i.getWaivedPenaltyAmount() : BigDecimal.ZERO;

        // Final amount the customer owes for this installment AFTER waiver applied.
        // = principal + profit + fee + remainingPenalty (= gross penalty − waived)
        BigDecimal effectiveTotal = i.getTotalAmount().subtract(waived).max(BigDecimal.ZERO);
        BigDecimal effectiveOutstanding = effectiveTotal.subtract(i.getPaidTotal()).max(BigDecimal.ZERO);

        var matchOpt = matcher.match(esRule, i, today);

        BigDecimal totalDiscount = BigDecimal.ZERO;
        if (matchOpt.isPresent()) {
            var m = matchOpt.get();
            if (m.isPercentage()) {
                BigDecimal pct = m.discountPercentage() != null ? m.discountPercentage() : BigDecimal.ZERO;
                totalDiscount = effectiveOutstanding
                        .multiply(pct)
                        .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            } else {
                totalDiscount = m.discountAmount() != null ? m.discountAmount() : BigDecimal.ZERO;
            }
        }
        // payableAmount = what customer pays IF early-settlement is applied right now
        BigDecimal payable = effectiveTotal.subtract(totalDiscount).max(BigDecimal.ZERO);

        PenaltyWaiverRequest pending = pendingWaivers.get(i.getId());

        return new InstallmentResponse(
                i.getId(),
                i.getInstallmentNumber(),
                i.getDueDate(),
                i.getPrincipalAmount(),
                i.getProfitAmount(),
                i.getFeeAmount(),
                i.getRemainingPenalty(),
                effectiveTotal,
                i.getPaidPrincipal(),
                i.getPaidProfit(),
                i.getPaidFee(),
                i.getPaidTotal(),
                effectiveOutstanding,
                i.getStatus(),
                i.getDpd(),
                matchOpt.isPresent(),
                matchOpt.map(m -> m.discountPercentage()).orElse(null),
                matchOpt.map(m -> m.discountAmount()).orElse(null),
                matchOpt.map(m -> m.validUntilDay()).orElse(null),
                totalDiscount,
                payable,
                i.isEligibleForWriteOff(),
                i.getWrittenOffPrincipal(),
                i.getWrittenOffProfit(),
                i.getWrittenOffFee(),
                i.getWrittenOffPenalty(),
                i.getWriteOffDate(),
                i.getWriteOffReason(),
                waived,
                i.getRemainingPenalty(),
                pending != null,
                pending != null ? pending.getRequestedAmount() : null
        );
    }

    private Map<UUID, PenaltyWaiverRequest> loadPendingWaivers(UUID tenantId, UUID loanId) {
        if (loanId == null) return Map.of();
        var requests = penaltyWaiverRequestRepository.findByLoanId(tenantId, loanId);
        Map<UUID, PenaltyWaiverRequest> map = new HashMap<>();
        for (var r : requests) {
            if (r.getStatus() == PenaltyWaiverRequest.WaiverRequestStatus.PENDING && r.getInstallmentId() != null) {
                // first pending wins (UI surfaces a single pending request per installment)
                map.putIfAbsent(r.getInstallmentId(), r);
            }
        }
        return map;
    }
}
