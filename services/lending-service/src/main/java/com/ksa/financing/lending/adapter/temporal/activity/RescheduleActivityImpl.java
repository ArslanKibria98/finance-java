package com.ksa.financing.lending.adapter.temporal.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.lending.infrastructure.persistence.entity.AmortizationScheduleJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.entity.LoanRescheduleJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.entity.LoanJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaAmortizationScheduleRepository;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanRescheduleRepository;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanRepository;
import com.ksa.islamic.orchestration.activity.lending.RescheduleActivity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements RescheduleActivity — all steps of the loan rescheduling Temporal workflow.
 *
 * Fineract proxy calls go through ledger-service:
 *   rescheduleInFineract() → POST {ledger-service}/api/v1/fineract-proxy/reschedule-loans
 *   approveFineractReschedule() → POST {ledger-service}/api/v1/fineract-proxy/reschedule-loans/{id}/approve
 *
 * GL entries go through LedgerServiceClient (already implemented for write-offs).
 */
@Slf4j
@Component
public class RescheduleActivityImpl implements RescheduleActivity {

    private final JpaLoanRepository loanRepository;
    private final JpaLoanRescheduleRepository rescheduleRepository;
    private final JpaAmortizationScheduleRepository amortizationRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String ledgerServiceUrl;

    public RescheduleActivityImpl(
            JpaLoanRepository loanRepository,
            JpaLoanRescheduleRepository rescheduleRepository,
            JpaAmortizationScheduleRepository amortizationRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.ledger-service-url}") String ledgerServiceUrl) {
        this.loanRepository = loanRepository;
        this.rescheduleRepository = rescheduleRepository;
        this.amortizationRepository = amortizationRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.ledgerServiceUrl = ledgerServiceUrl;
    }

    // ══════════════════════════════════════════════════════════════
    // VALIDATE ELIGIBILITY
    // ══════════════════════════════════════════════════════════════

    @Override
    public EligibilityResult validateEligibility(EligibilityInput input) {
        log.info("Validating reschedule eligibility: loanId={} type={}", input.loanId(), input.rescheduleType());

        UUID loanId = UUID.fromString(input.loanId());
        UUID tenantId = UUID.fromString(input.tenantId());

        // Load loan
        var loanOpt = loanRepository.findById(loanId);
        if (loanOpt.isEmpty()) {
            return ineligible("Loan not found: " + input.loanId());
        }

        var loan = loanOpt.get();

        // Must be ACTIVE
        if (!"ACTIVE".equals(loan.getStatus())) {
            return ineligible("Loan is not ACTIVE. Current status: " + loan.getStatus());
        }

        // Calculate loan age in months
        int loanAgeMonths = 0;
        if (loan.getDisbursementDate() != null) {
            loanAgeMonths = Period.between(loan.getDisbursementDate(), LocalDate.now()).getMonths()
                    + Period.between(loan.getDisbursementDate(), LocalDate.now()).getYears() * 12;
        }

        // Minimum loan age check
        int minAge = minLoanAge(input.rescheduleType());
        if (loanAgeMonths < minAge) {
            return ineligible("Loan must be at least " + minAge + " months old. Current age: " + loanAgeMonths + " months");
        }

        // SKIP_PAYMENT: max 2 skips, min 6 months between skips
        if ("SKIP_PAYMENT".equals(input.rescheduleType())) {
            long skipCount = rescheduleRepository
                    .findByTenantIdAndLoanIdAndStatus(tenantId, loanId, "APPROVED")
                    .stream()
                    .filter(r -> "SKIP_PAYMENT".equals(r.getRescheduleType()))
                    .count();

            if (skipCount >= 2) {
                return ineligible("Maximum 2 skip payments already used (Blueprint 17 § 4.1)");
            }

            // Check last skip was at least 6 months ago
            var lastSkip = rescheduleRepository
                    .findByTenantIdAndLoanIdAndStatus(tenantId, loanId, "APPROVED")
                    .stream()
                    .filter(r -> "SKIP_PAYMENT".equals(r.getRescheduleType()))
                    .max((a, b) -> a.getAppliedAt().compareTo(b.getAppliedAt()));

            if (lastSkip.isPresent()) {
                int monthsSinceLastSkip = Period.between(
                        lastSkip.get().getAppliedAt().toLocalDate(), LocalDate.now()
                ).getMonths() + Period.between(
                        lastSkip.get().getAppliedAt().toLocalDate(), LocalDate.now()
                ).getYears() * 12;

                if (monthsSinceLastSkip < 6) {
                    return ineligible("Must wait 6 months between skip payments. Last skip was " + monthsSinceLastSkip + " months ago");
                }
            }
        }

        // TENURE_EXTENSION: max 12 months, max total 72 months
        if ("TENURE_EXTENSION".equals(input.rescheduleType())) {
            if (input.extensionMonths() == null || input.extensionMonths() < 1 || input.extensionMonths() > 12) {
                return ineligible("Extension must be between 1 and 12 months (Blueprint 17 § 4.1)");
            }
            int currentTenure = loan.getTenureMonths() != null ? loan.getTenureMonths() : 0;
            if (currentTenure + input.extensionMonths() > 72) {
                return ineligible("Total tenure cannot exceed 72 months. Current: " + currentTenure + ", extension: " + input.extensionMonths());
            }
        }

        // PAYMENT_HOLIDAY: max 3 months
        if ("PAYMENT_HOLIDAY".equals(input.rescheduleType())) {
            if (input.holidayMonths() == null || input.holidayMonths() < 1 || input.holidayMonths() > 3) {
                return ineligible("Payment holiday must be 1-3 months (Blueprint 17 § 4.1)");
            }
        }

        // Check no active reschedule of same type already pending/approved
        boolean hasActive = rescheduleRepository
                .existsByTenantIdAndLoanIdAndRescheduleTypeAndStatusIn(
                        tenantId, loanId, input.rescheduleType(),
                        List.of("PENDING", "PROCESSING"));

        if (hasActive) {
            return ineligible("An active " + input.rescheduleType() + " request already exists for this loan");
        }

        int currentTenure = loan.getTenureMonths() != null ? loan.getTenureMonths() : 0;
        BigDecimal installment = loan.getInstallmentAmount() != null ? loan.getInstallmentAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = loan.getPrincipalAmount() != null ? loan.getPrincipalAmount() : BigDecimal.ZERO;

        log.info("Eligibility check passed: loanId={} age={}mo type={}", input.loanId(), loanAgeMonths, input.rescheduleType());

        return new EligibilityResult(true, null, currentTenure, installment, outstanding, 
                loanAgeMonths, 0, 0, loan.getMaturityDate());
    }

    // ══════════════════════════════════════════════════════════════
    // SAVE PENDING REQUEST
    // ══════════════════════════════════════════════════════════════

    @Override
    public String saveRescheduleRequest(SaveRescheduleInput input) {
        log.info("Saving reschedule request: loanId={} type={}", input.loanId(), input.rescheduleType());

        // Idempotency check — also upgrades SUBMITTED → PENDING (pre-save pattern)
        if (input.idempotencyKey() != null) {
            var existing = rescheduleRepository.findByTenantIdAndIdempotencyKey(
                    UUID.fromString(input.tenantId()), input.idempotencyKey());
            if (existing.isPresent()) {
                var rec = existing.get();
                if ("SUBMITTED".equals(rec.getStatus())) {
                    rec.setStatus("PENDING");
                    rec.setOldTenureMonths(input.oldTenureMonths());
                    rec.setOldInstallmentAmount(input.oldInstallmentAmount());
                    rec.setOldMaturityDate(input.oldMaturityDate());
                    rescheduleRepository.save(rec);
                    log.info("Upgraded SUBMITTED→PENDING for reschedule id={}", rec.getId());
                } else {
                    log.info("Idempotent: returning existing reschedule id={}", rec.getId());
                }
                return rec.getId().toString();
            }
        }

        var entity = LoanRescheduleJpaEntity.builder()
                .tenantId(UUID.fromString(input.tenantId()))
                .loanId(UUID.fromString(input.loanId()))
                .loanNumber(input.loanNumber())
                .rescheduleType(input.rescheduleType())
                .status("PENDING")
                .requestedBy(input.requestedBy() != null ? parseUuidSafely(input.requestedBy()) : null)
                .justification(input.justification())
                .requestedSkipMonth(input.requestedSkipMonth() != null ? LocalDate.parse(input.requestedSkipMonth()) : null)
                .extensionMonths(input.extensionMonths())
                .holidayMonths(input.holidayMonths())
                .newProfitRate(input.newProfitRate())
                .writeOffAmount(input.writeOffAmount())
                .profitWaiverAmount(input.profitWaiverAmount())
                .attachmentUrl(input.attachmentUrl())
                .workflowId(input.workflowId())
                .idempotencyKey(input.idempotencyKey())
                .oldTenureMonths(input.oldTenureMonths())
                .oldInstallmentAmount(input.oldInstallmentAmount())
                .oldMaturityDate(input.oldMaturityDate())
                .createdBy(input.requestedBy() != null ? parseUuidSafely(input.requestedBy()) : null)
                .build();

        var saved = rescheduleRepository.save(entity);
        log.info("Reschedule request saved: id={}", saved.getId());
        return saved.getId().toString();
    }

    // ══════════════════════════════════════════════════════════════
    // GENERATE NEW SCHEDULE
    // ══════════════════════════════════════════════════════════════

    @Override
    public ScheduleResult generateNewSchedule(GenerateScheduleInput input) {
        log.info("Generating new schedule: loanId={} type={}", input.loanId(), input.rescheduleType());

        var loan = loanRepository.findById(UUID.fromString(input.loanId()))
                .orElseThrow(() -> new IllegalStateException("Loan not found: " + input.loanId()));

        int currentTenure = loan.getTenureMonths() != null ? loan.getTenureMonths() : 0;
        BigDecimal principal = loan.getPrincipalAmount() != null ? loan.getPrincipalAmount() : BigDecimal.ZERO;
        BigDecimal profitRate = input.newProfitRate() != null ? input.newProfitRate() : loan.getProfitRate();

        return switch (input.rescheduleType()) {
            case "SKIP_PAYMENT" -> {
                // Tenure (installment count) remains same, but maturity extends by 1 month
                int newTenure = currentTenure; 
                BigDecimal newInstallment = loan.getInstallmentAmount() != null 
                        ? loan.getInstallmentAmount() : calculateFlatInstallment(principal, profitRate, currentTenure);
                LocalDate newMaturity = loan.getMaturityDate() != null 
                        ? loan.getMaturityDate().plusMonths(1) 
                        : LocalDate.now().plusMonths(newTenure + 1); // +1 because we skipped a month
                yield new ScheduleResult(newTenure, newInstallment, newMaturity);
            }

            case "TENURE_EXTENSION" -> {
                int extensionMonths = input.extensionMonths() != null ? input.extensionMonths() : 0;
                int newTenure = currentTenure + extensionMonths;
                
                // For Murabaha/Tawarruq, we use the Flat Profit formula from FinanceCalculationService
                BigDecimal newInstallment = calculateFlatInstallment(principal, profitRate, newTenure);
                LocalDate newMaturity = loan.getMaturityDate() != null 
                        ? loan.getMaturityDate().plusMonths(extensionMonths)
                        : LocalDate.now().plusMonths(newTenure);
                yield new ScheduleResult(newTenure, newInstallment, newMaturity);
            }

            case "PAYMENT_HOLIDAY" -> {
                // Payments paused — tenure extends by holiday months, no additional profit (Sharia)
                int holidayMonths = input.holidayMonths() != null ? input.holidayMonths() : 0;
                int newTenure = currentTenure + holidayMonths;
                // Installment stays the same, just pushed out
                BigDecimal sameInstallment = loan.getInstallmentAmount() != null
                        ? loan.getInstallmentAmount() : calculateFlatInstallment(principal, profitRate, currentTenure);
                LocalDate newMaturity = LocalDate.now().plusMonths(newTenure);
                yield new ScheduleResult(newTenure, sameInstallment, newMaturity);
            }

            case "RESTRUCTURING" -> {
                // May have reduced rate + extended tenure; fetch write-off from reschedule entity
                BigDecimal effectivePrincipal = principal;
                if (input.rescheduleId() != null) {
                    var rescheduleOpt = rescheduleRepository.findById(UUID.fromString(input.rescheduleId()));
                    if (rescheduleOpt.isPresent() && rescheduleOpt.get().getWriteOffAmount() != null
                            && rescheduleOpt.get().getWriteOffAmount().compareTo(BigDecimal.ZERO) > 0) {
                        effectivePrincipal = effectivePrincipal.subtract(rescheduleOpt.get().getWriteOffAmount());
                    }
                }
                int newTenure = currentTenure; // tenure unchanged unless specified
                BigDecimal newInstallment = calculateFlatInstallment(effectivePrincipal, profitRate, newTenure);
                LocalDate newMaturity = loan.getMaturityDate() != null ? loan.getMaturityDate() : LocalDate.now().plusMonths(newTenure);
                yield new ScheduleResult(newTenure, newInstallment, newMaturity);
            }

            default -> {
                log.warn("Unknown reschedule type '{}', using current schedule", input.rescheduleType());
                yield new ScheduleResult(currentTenure, loan.getInstallmentAmount(), null);
            }
        };
    }

    // ══════════════════════════════════════════════════════════════
    // FINERACT PROXY (via ledger-service)
    // ══════════════════════════════════════════════════════════════

    @Override
    public FineractRescheduleResult rescheduleInFineract(FineractRescheduleInput input) {
        log.info("Submitting reschedule to Fineract via ledger-service proxy: loanId={} type={}",
                input.loanId(), input.rescheduleType());

        try {
            var headers = buildLedgerProxyHeaders(input.tenantId(), input.idempotencyKey());

            var body = new java.util.HashMap<String, Object>();
            body.put("loanId", input.loanId());
            body.put("rescheduleType", input.rescheduleType());
            body.put("rescheduleFromDate", input.rescheduleFromDate());
            if (input.extensionMonths() != null) body.put("extensionMonths", input.extensionMonths());
            if (input.newProfitRate() != null) body.put("newInterestRate", input.newProfitRate());

            var response = restTemplate.exchange(
                    ledgerServiceUrl + "/api/v1/fineract-proxy/reschedule-loans",
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                var result = objectMapper.readTree(response.getBody());
                Long rescheduleId = result.has("resourceId") ? result.get("resourceId").asLong() : null;
                log.info("Fineract reschedule submitted via ledger-service: fineractRescheduleId={}", rescheduleId);
                return new FineractRescheduleResult(rescheduleId, true);
            }
        } catch (Exception e) {
            log.warn("Fineract reschedule via ledger-service failed (non-blocking): {}", e.getMessage());
        }

        return new FineractRescheduleResult(null, false);
    }

    @Override
    public void approveFineractReschedule(ApproveFineractInput input) {
        log.info("Approving Fineract reschedule via ledger-service proxy: fineractRescheduleId={}",
                input.fineractRescheduleId());

        try {
            var headers = buildLedgerProxyHeaders(input.tenantId(), null);

            var body = Map.of(
                    "approvedOnDate", input.approvedOnDate(),
                    "dateFormat", "yyyy-MM-dd",
                    "locale", "en"
            );

            restTemplate.exchange(
                    ledgerServiceUrl + "/api/v1/fineract-proxy/reschedule-loans/"
                            + input.fineractRescheduleId() + "/approve",
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                    String.class
            );

            log.info("Fineract reschedule approved via ledger-service: id={}", input.fineractRescheduleId());
        } catch (Exception e) {
            log.warn("Fineract reschedule approval via ledger-service failed (non-blocking): {}", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK APPLIED / REJECTED
    // ══════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void markApplied(MarkAppliedInput input) {
        log.info("Marking reschedule as APPROVED: rescheduleId={}", input.rescheduleId());

        var entity = rescheduleRepository.findById(UUID.fromString(input.rescheduleId()))
                .orElseThrow(() -> new IllegalStateException("Reschedule not found: " + input.rescheduleId()));

        entity.setStatus("APPROVED");
        entity.setNewTenureMonths(input.newTenureMonths());
        entity.setNewInstallmentAmount(input.newInstallmentAmount());
        entity.setNewMaturityDate(input.newMaturityDate() != null ? LocalDate.parse(input.newMaturityDate()) : null);
        entity.setFineractRescheduleId(input.fineractRescheduleId());
        entity.setFineractSynced(input.fineractRescheduleId() != null);
        entity.setGlPosted(input.glEntryNumber() != null);
        entity.setAppliedAt(OffsetDateTime.now());

        rescheduleRepository.save(entity);

        // ── Propagate new schedule to loans table ──────────────────────
        var loanOpt = loanRepository.findById(entity.getLoanId());
        if (loanOpt.isPresent()) {
            var loan = loanOpt.get();
            if (input.newTenureMonths() > 0) {
                loan.setTenureMonths(input.newTenureMonths());
            }
            if (input.newInstallmentAmount() != null) {
                loan.setInstallmentAmount(input.newInstallmentAmount());
            }
            if (input.newMaturityDate() != null) {
                loan.setMaturityDate(LocalDate.parse(input.newMaturityDate()));
            }
            // Propagate profit rate if changed
            if (entity.getNewProfitRate() != null) {
                loan.setProfitRate(entity.getNewProfitRate());
            }
            // Propagate write-off to principal if applicable
            if (entity.getWriteOffAmount() != null && entity.getWriteOffAmount().compareTo(BigDecimal.ZERO) > 0) {
                loan.setPrincipalAmount(loan.getPrincipalAmount().subtract(entity.getWriteOffAmount()));
            }
            // Propagate profit waiver if applicable
            if (entity.getProfitWaiverAmount() != null && entity.getProfitWaiverAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal oldProfit = loan.getProfitAmount() != null ? loan.getProfitAmount() : BigDecimal.ZERO;
                loan.setProfitAmount(oldProfit.subtract(entity.getProfitWaiverAmount()));
            }

            loan.setUpdatedAt(java.time.LocalDateTime.now());
            loanRepository.save(loan);
            log.info("Loan updated after reschedule: loanId={} newTenure={} newInstallment={}",
                    entity.getLoanId(), input.newTenureMonths(), input.newInstallmentAmount());
        } else {
            log.warn("Loan not found for reschedule update: loanId={}", entity.getLoanId());
        }

        // ── Update amortization schedule for SKIP_PAYMENT ──────────────
        // Mark the skipped installment. Tenure and amounts remain unchanged.
        if ("SKIP_PAYMENT".equals(entity.getRescheduleType()) && entity.getRequestedSkipMonth() != null) {
            applySkipPaymentToSchedule(entity);
        } else {
            // For other types (Tenure Extension, Restructuring), regenerate the whole schedule
            regenerateAmortizationSchedule(entity, input);
        }

        log.info("Reschedule marked APPROVED: id={}", input.rescheduleId());
    }

    @Override
    public void markRejected(MarkRejectedInput input) {
        log.info("Marking reschedule as REJECTED: rescheduleId={}", input.rescheduleId());

        var entity = rescheduleRepository.findById(UUID.fromString(input.rescheduleId()))
                .orElseThrow(() -> new IllegalStateException("Reschedule not found: " + input.rescheduleId()));

        entity.setStatus("REJECTED");
        entity.setRejectionReason(input.rejectionReason());
        entity.setRejectedAt(OffsetDateTime.now());

        rescheduleRepository.save(entity);
    }

    @Override
    public void markRejectedByIdempotencyKey(MarkRejectedByKeyInput input) {
        log.info("Marking reschedule REJECTED by idempotency key: tenantId={} key={}",
                input.tenantId(), input.idempotencyKey());

        if (input.idempotencyKey() == null) {
            log.warn("No idempotencyKey provided — cannot mark rejected by key");
            return;
        }

        var existing = rescheduleRepository.findByTenantIdAndIdempotencyKey(
                UUID.fromString(input.tenantId()), input.idempotencyKey());

        if (existing.isEmpty()) {
            log.warn("No reschedule found for idempotencyKey={} — nothing to mark rejected", input.idempotencyKey());
            return;
        }

        var entity = existing.get();
        entity.setStatus("REJECTED");
        entity.setRejectionReason(input.rejectionReason());
        entity.setRejectedAt(OffsetDateTime.now());
        rescheduleRepository.save(entity);

        log.info("Reschedule marked REJECTED by key: id={}", entity.getId());
    }

    // ══════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════

    /**
     * Skip Payment: mark the requested month as SKIPPED. The loan tenure and 
     * installments remain unchanged. Subsequent payments continue on their 
     * original schedule.
     */
    protected void applySkipPaymentToSchedule(LoanRescheduleJpaEntity reschedule) {
        UUID loanId = reschedule.getLoanId();
        LocalDate skipMonth = reschedule.getRequestedSkipMonth().withDayOfMonth(1);

        log.info("Applying skip payment to amortization schedule: loanId={} skipMonth={}", loanId, skipMonth);

        // Ensure schedule is materialized if it's currently empty (sync fallback)
        materializeAmortizationIfEmpty(loanId);

        var activeInstallments = amortizationRepository
                .findByLoanIdAndActiveOrderByInstallmentNumberAsc(loanId, true);

        if (activeInstallments.isEmpty()) {
            log.warn("No active amortization schedule found for loanId={} after materialization attempt — skip not applied to schedule", loanId);
            return;
        }

        // 1. Find and DEACTIVATE the installment whose due_date falls in the skip month
        AmortizationScheduleJpaEntity skippedRow = null;
        for (var row : activeInstallments) {
            if (row.getDueDate().getYear() == skipMonth.getYear()
                    && row.getDueDate().getMonthValue() == skipMonth.getMonthValue()
                    && "PENDING".equals(row.getPaymentStatus())) {
                skippedRow = row;
                break;
            }
        }

        if (skippedRow == null) {
            log.warn("No PENDING installment found for skipMonth={} loanId={}", skipMonth, loanId);
            return;
        }

        // Deactivate the skipped installment so it "disappears" from active schedule
        skippedRow.setPaymentStatus("SKIPPED");
        skippedRow.setSkipped(true);
        skippedRow.setActive(false); // Remove from active list as requested
        amortizationRepository.save(skippedRow);
        log.info("Skipped installment deactivated: loanId={} installment={}", loanId, skippedRow.getInstallmentNumber());

        // 2. Add a NEW installment at the end of the schedule
        AmortizationScheduleJpaEntity lastRow = activeInstallments.get(activeInstallments.size() - 1);
        int newInstallmentNumber = lastRow.getInstallmentNumber() + 1;
        LocalDate newDueDate = lastRow.getDueDate().plusMonths(1);

        var newRow = AmortizationScheduleJpaEntity.builder()
                .tenantId(skippedRow.getTenantId())
                .loanId(loanId)
                .scheduleVersion(skippedRow.getScheduleVersion())
                .active(true)
                .installmentNumber(newInstallmentNumber)
                .dueDate(newDueDate)
                .openingPrincipal(lastRow.getClosingPrincipal())
                .principalComponent(skippedRow.getPrincipalComponent())
                .profitComponent(skippedRow.getProfitComponent())
                .totalInstallment(skippedRow.getTotalInstallment())
                .closingPrincipal(lastRow.getClosingPrincipal().subtract(skippedRow.getPrincipalComponent()).max(BigDecimal.ZERO))
                .cumulativePrincipal(lastRow.getCumulativePrincipal().add(skippedRow.getPrincipalComponent()))
                .cumulativeProfit(lastRow.getCumulativeProfit().add(skippedRow.getProfitComponent()))
                .calculationMethod("SKIP_POSTPONED")
                .paymentStatus("PENDING")
                .skipped(false)
                .build();

        amortizationRepository.save(newRow);

        log.info("New installment added at end of schedule: loanId={} installment={} dueDate={}",
                loanId, newInstallmentNumber, newDueDate);
    }

    /**
     * Simple Profit (Flat Rate) formula as per FinanceCalculationService:
     *   Profit = Principal * AnnualRate * (TenureMonths / 12)
     *   Total = Principal + Profit
     *   Installment = Total / TenureMonths
     */
    private BigDecimal calculateFlatInstallment(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) == 0 || tenureMonths == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal rate = annualRate != null ? annualRate : BigDecimal.ZERO;
        if (rate.compareTo(BigDecimal.ONE) >= 0) {
            rate = rate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        }

        // Profit = P * r * (n/12)
        BigDecimal profit = principal
                .multiply(rate)
                .multiply(BigDecimal.valueOf(tenureMonths))
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        BigDecimal total = principal.add(profit);
        
        return total.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
    }

    /**
     * EMI formula (retained only if some products need it, but renamed)
     */
    private BigDecimal calculateAmortizedInstallment(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) == 0 || tenureMonths == 0) {
            return BigDecimal.ZERO;
        }
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }
        BigDecimal r = annualRate;
        if (r.compareTo(BigDecimal.ONE) >= 0) {
            r = r.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        }
        r = r.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        double rD = r.doubleValue();
        double pD = principal.doubleValue();
        double n = tenureMonths;
        double pmt = pD * (rD * Math.pow(1 + rD, n)) / (Math.pow(1 + rD, n) - 1);

        return BigDecimal.valueOf(pmt).setScale(2, RoundingMode.HALF_UP);
    }

    private int minLoanAge(String type) {
        // DEV: minimum age relaxed to 0 for demo/testing
        return 0;
    }

    private EligibilityResult ineligible(String reason) {
        log.warn("Ineligible for reschedule: {}", reason);
        return new EligibilityResult(false, reason, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0, null);
    }

    private HttpHeaders buildLedgerProxyHeaders(String tenantId, String idempotencyKey) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", tenantId);
        headers.set("X-Caller-Service", "lending-service");
        if (idempotencyKey != null) {
            headers.set("X-Idempotency-Key", idempotencyKey);
        }
        return headers;
    }

    private UUID parseUuidSafely(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            log.warn("Could not parse UUID from '{}', using null", value);
            return null;
        }
    }

    /**
     * Populates amortization_schedules with a computed schedule if it's empty for this loan.
     * This handles cases where the loan was created but the schedule wasn't properly synced/materialized.
     */
    private void materializeAmortizationIfEmpty(UUID loanId) {
        var loan = loanRepository.findById(loanId).orElse(null);
        if (loan == null) return;

        var existing = amortizationRepository
                .findByLoanIdAndActiveOrderByInstallmentNumberAsc(loan.getId(), true);
        if (!existing.isEmpty()) return;
        
        if (loan.getTenureMonths() == null || loan.getTenureMonths() <= 0 || loan.getPrincipalAmount() == null) {
            log.warn("Cannot materialize schedule for loanId={}: missing tenure or principal", loanId);
            return;
        }

        log.info("Materializing amortization schedule for loanId={} as it is empty", loanId);

        var startDate = loan.getDisbursementDate() != null
                ? loan.getDisbursementDate().plusMonths(1)
                : LocalDate.now().plusMonths(1);

        var principal = loan.getPrincipalAmount();
        var totalProfit = loan.getProfitAmount() != null ? loan.getProfitAmount() : BigDecimal.ZERO;
        var tenure = loan.getTenureMonths();
        
        var monthlyPrincipal = principal.divide(BigDecimal.valueOf(tenure), 6, RoundingMode.HALF_UP);
        var monthlyProfit = totalProfit.divide(BigDecimal.valueOf(tenure), 6, RoundingMode.HALF_UP);
        var monthlyTotal = monthlyPrincipal.add(monthlyProfit);

        BigDecimal cumulativePrincipal = BigDecimal.ZERO;
        BigDecimal cumulativeProfit = BigDecimal.ZERO;
        BigDecimal closing = principal;
        
        for (int n = 1; n <= tenure; n++) {
            var opening = closing;
            closing = closing.subtract(monthlyPrincipal);
            cumulativePrincipal = cumulativePrincipal.add(monthlyPrincipal);
            cumulativeProfit = cumulativeProfit.add(monthlyProfit);

            var row = AmortizationScheduleJpaEntity.builder()
                    .tenantId(loan.getTenantId())
                    .loanId(loan.getId())
                    .scheduleVersion(1)
                    .active(true)
                    .installmentNumber(n)
                    .dueDate(startDate.plusMonths(n - 1L))
                    .openingPrincipal(opening)
                    .principalComponent(monthlyPrincipal)
                    .profitComponent(monthlyProfit)
                    .totalInstallment(monthlyTotal)
                    .closingPrincipal(closing.max(BigDecimal.ZERO))
                    .cumulativePrincipal(cumulativePrincipal)
                    .cumulativeProfit(cumulativeProfit)
                    .calculationMethod("REDUCING_BALANCE")
                    .paymentStatus("PENDING")
                    .skipped(false)
                    .build();
            amortizationRepository.save(row);
        }
    }

    /**
     * Regenerates the amortization schedule for a loan after restructuring or tenure extension.
     */
    private void regenerateAmortizationSchedule(LoanRescheduleJpaEntity reschedule, MarkAppliedInput input) {
        UUID loanId = reschedule.getLoanId();
        log.info("Regenerating amortization schedule for reschedule: loanId={} type={}", loanId, reschedule.getRescheduleType());

        // 1. Deactivate current schedule
        amortizationRepository.deactivateAllByLoanId(loanId);

        // 2. Fetch updated loan terms
        var loan = loanRepository.findById(loanId).orElseThrow();
        int tenure = input.newTenureMonths();
        
        if (tenure <= 0) {
            log.warn("Cannot regenerate schedule for loanId={}: tenure is 0. Using current loan tenure.", loanId);
            tenure = loan.getTenureMonths() != null ? loan.getTenureMonths() : 0;
        }
        
        if (tenure <= 0) {
            log.error("Failed to regenerate schedule for loanId={}: tenure is still 0", loanId);
            return;
        }

        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal totalProfit = loan.getProfitAmount() != null ? loan.getProfitAmount() : BigDecimal.ZERO;
        
        log.info("Regenerating schedule with principal={} totalProfit={} tenure={}", principal, totalProfit, tenure);

        int holidayMonths = "PAYMENT_HOLIDAY".equals(reschedule.getRescheduleType()) && reschedule.getHolidayMonths() != null 
                ? reschedule.getHolidayMonths() : 0;
        int payingMonths = tenure - holidayMonths;
        if (payingMonths <= 0) payingMonths = tenure; // fallback

        // Distribution based on actual paying months
        BigDecimal monthlyPrincipal = principal.divide(BigDecimal.valueOf(payingMonths), 6, RoundingMode.HALF_UP);
        BigDecimal monthlyProfit = totalProfit.divide(BigDecimal.valueOf(payingMonths), 6, RoundingMode.HALF_UP);
        BigDecimal monthlyTotal = input.newInstallmentAmount() != null ? input.newInstallmentAmount() : monthlyPrincipal.add(monthlyProfit);

        Integer maxVersion = amortizationRepository.findMaxScheduleVersionByLoanId(loanId);
        int newVersion = (maxVersion != null ? maxVersion : 0) + 1;
        
        LocalDate startDate = LocalDate.now().plusMonths(1);
        LocalDate holidayStart = reschedule.getRequestedSkipMonth();
        BigDecimal cumulativePrincipal = BigDecimal.ZERO;
        BigDecimal cumulativeProfit = BigDecimal.ZERO;
        BigDecimal closing = principal;
        for (int n = 1; n <= tenure; n++) {
            LocalDate currentDueDate = startDate.plusMonths(n - 1L);
            boolean isHoliday = false;

            if (holidayMonths > 0) {
                if (holidayStart != null) {
                    long monthsFromStart = java.time.temporal.ChronoUnit.MONTHS.between(
                            holidayStart.withDayOfMonth(1), currentDueDate.withDayOfMonth(1));
                    if (monthsFromStart >= 0 && monthsFromStart < holidayMonths) {
                        isHoliday = true;
                    }
                } else {
                    if (n <= holidayMonths) isHoliday = true;
                }
            }

            BigDecimal currentMonthlyPrincipal = monthlyPrincipal;
            BigDecimal currentMonthlyProfit = monthlyProfit;
            BigDecimal currentMonthlyTotal = monthlyTotal;

            if (isHoliday) {
                currentMonthlyPrincipal = BigDecimal.ZERO;
                currentMonthlyProfit = BigDecimal.ZERO;
                currentMonthlyTotal = BigDecimal.ZERO;
            }

            var opening = closing;
            closing = closing.subtract(currentMonthlyPrincipal);
            cumulativePrincipal = cumulativePrincipal.add(currentMonthlyPrincipal);
            cumulativeProfit = cumulativeProfit.add(currentMonthlyProfit);

            var row = AmortizationScheduleJpaEntity.builder()
                    .tenantId(loan.getTenantId())
                    .loanId(loan.getId())
                    .scheduleVersion(newVersion)
                    .active(true)
                    .installmentNumber(n)
                    .dueDate(startDate.plusMonths(n - 1L))
                    .openingPrincipal(opening)
                    .principalComponent(currentMonthlyPrincipal)
                    .profitComponent(currentMonthlyProfit)
                    .totalInstallment(currentMonthlyTotal)
                    .closingPrincipal(closing.max(BigDecimal.ZERO))
                    .cumulativePrincipal(cumulativePrincipal)
                    .cumulativeProfit(cumulativeProfit)
                    .calculationMethod("RESTRUCTURED")
                    .paymentStatus("PENDING")
                    .skipped(isHoliday)
                    .build();
            amortizationRepository.save(row);
        }
    }
}
