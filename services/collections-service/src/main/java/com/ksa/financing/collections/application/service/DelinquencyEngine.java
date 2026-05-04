package com.ksa.financing.collections.application.service;

import com.ksa.financing.collections.application.service.DunningPolicyResolver.ResolvedPolicy;
import com.ksa.financing.collections.domain.model.DelinquencyRule;
import com.ksa.financing.collections.domain.model.DelinquencyType;
import com.ksa.financing.collections.domain.model.DunningStage;
import com.ksa.financing.collections.domain.model.Installment;
import com.ksa.financing.collections.domain.model.InstallmentStatus;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Stateless application service that applies a resolved dunning policy to
 * a loan's repayment schedule. Consulted by:
 *   - Payment processing (to assess fees + stage BEFORE/AFTER a payment)
 *   - Scheduled aging jobs (to transition installment statuses daily)
 *   - Admin simulation endpoints
 *   - Dunning workflow activities
 *
 * This is the single place that turns "DPD on an installment" into concrete
 * policy-driven outcomes: stage, late fee, Simah flag, escalation flags.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DelinquencyEngine {

    private final DunningPolicyResolver policyResolver;
    private final DelinquencyRulesResolver rulesResolver;
    private final WriteOffEligibilityService writeOffEligibilityService;

    /**
     * Assess the current delinquency state of a loan's schedule against the
     * resolved policy as of a given date.
     * This method is READ-ONLY — no mutations to the aggregate.
     */
    public DelinquencyAssessment assess(
            UUID tenantId,
            String productCode,
            RepaymentScheduleAggregate schedule,
            LocalDate asOf) {

        var resolved = policyResolver.resolve(tenantId, productCode);
        int loanDpd = computeMaxDpd(schedule, asOf);
        var stage = resolved.thresholds().stageFor(loanDpd);

        List<InstallmentDelinquency> perInstallment = new ArrayList<>();
        for (Installment inst : schedule.getInstallments()) {
            if (inst.getStatus() == InstallmentStatus.PAID
                    || inst.getStatus() == InstallmentStatus.WAIVED) {
                continue;
            }
            int dpd = daysBetween(inst.getDueDate(), asOf);
            if (dpd < 0) continue; // Skip future installments

            BigDecimal outstanding = inst.getOutstandingAmount();
            BigDecimal lateFee = resolved.lateFee().computeFee(outstanding, dpd);

            perInstallment.add(new InstallmentDelinquency(
                    inst.getId(),
                    inst.getInstallmentNumber(),
                    inst.getDueDate(),
                    dpd,
                    outstanding,
                    resolved.thresholds().stageFor(dpd),
                    lateFee));
        }

        boolean reportToSimah     = resolved.simah().shouldReport(loanDpd);
        boolean simahDefaultFlag  = resolved.simah().shouldMarkDefault(loanDpd);
        boolean assignAgent       = resolved.hasPersistedPolicy()
                && resolved.policy().shouldAssignAgent(loanDpd);
        boolean freezeWallet      = resolved.hasPersistedPolicy()
                && resolved.policy().shouldFreezeWallet(loanDpd);

        Map<String, Object> actions = resolved.hasPersistedPolicy()
                ? resolved.policy().actionsForStage(stage)
                : Map.of();

        var charityFundAccount = resolved.lateFee().charityFundAccount();

        return new DelinquencyAssessment(
                tenantId, schedule.getLoanId(), productCode, asOf,
                loanDpd, stage,
                reportToSimah, simahDefaultFlag,
                assignAgent, freezeWallet,
                charityFundAccount, actions,
                perInstallment,
                resolved.source());
    }

    /**
     * Applies policy-driven status transitions to the schedule (SCHEDULED → DUE → GRACE → OVERDUE).
     * This method MUTATES the passed aggregate; caller is responsible for persisting it.
     */
    public void advanceStages(
            UUID tenantId,
            String productCode,
            RepaymentScheduleAggregate schedule,
            LocalDate asOf) {

        var resolved = policyResolver.resolve(tenantId, productCode);
        int graceDays = resolved.thresholds().gracePeriodDays();

        schedule.markInstallmentsDue(asOf);
        schedule.markInstallmentsOverdue(asOf, graceDays);

        applyDelinquencyPenalties(tenantId, schedule, asOf);

        // Automate write-off eligibility evaluation
        writeOffEligibilityService.evaluate(tenantId, schedule, asOf);

        log.debug("Advanced stages for loan {} (tenant {}) using policy source={} graceDays={}",
                schedule.getLoanId(), tenantId, resolved.source(), graceDays);
    }

    /**
     * Applies delinquency rules (DUE_LOAN type=2 and LATE_PAYMENT type=3) to installments.
     * Sequentially checks which rule applies based on current DPD.
     */
    private void applyDelinquencyPenalties(UUID tenantId, RepaymentScheduleAggregate schedule, LocalDate asOf) {
        UUID productId = schedule.getProductId();
        if (productId == null) return;

        Optional<DelinquencyRule> dueRuleOpt = rulesResolver.rule(tenantId, productId, DelinquencyType.DUE_LOAN);
        Optional<DelinquencyRule> lateRuleOpt = rulesResolver.rule(tenantId, productId, DelinquencyType.LATE_PAYMENT);

        for (Installment inst : schedule.getInstallments()) {
            if (inst.getStatus() == InstallmentStatus.PAID || inst.getStatus() == InstallmentStatus.WAIVED) {
                continue;
            }
            int dpd = daysBetween(inst.getDueDate(), asOf);
            BigDecimal penalty = BigDecimal.ZERO;
            String ruleApplied = "NONE";

            // 1. Check Due Loan (Type 2) - Check this first as requested
            if (dueRuleOpt.isPresent()) {
                var rule = dueRuleOpt.get();
                if (dpd >= rule.getFromDay() && (rule.getTillDay() == 0 || dpd <= rule.getTillDay())) {
                    penalty = computeLatePenalty(rule, inst.getOutstandingPrincipal().add(inst.getOutstandingProfit()));
                    ruleApplied = "DUE_LOAN (Type 2)";
                    log.debug("Applied DUE_LOAN penalty: amount={}, isPercentage={}, inst={}, dpd={}", 
                            penalty, rule.isPercentage(), inst.getInstallmentNumber(), dpd);
                }
            }

            // 2. Check Late Payment (Type 3) - Only apply if no due penalty applied or if it's explicitly overdue
            if (penalty.compareTo(BigDecimal.ZERO) == 0 && dpd > 0 && lateRuleOpt.isPresent()) {
                var rule = lateRuleOpt.get();
                if (dpd >= rule.getFromDay() && (rule.getTillDay() == 0 || dpd <= rule.getTillDay())) {
                    penalty = computeLatePenalty(rule, inst.getOutstandingPrincipal().add(inst.getOutstandingProfit()));
                    ruleApplied = "LATE_PAYMENT (Type 3)";
                    log.debug("Applied LATE_PAYMENT penalty: amount={}, isPercentage={}, inst={}, dpd={}", 
                            penalty, rule.isPercentage(), inst.getInstallmentNumber(), dpd);
                }
            }

            if (penalty.compareTo(BigDecimal.ZERO) > 0 || inst.getLatePenaltyAmount().compareTo(BigDecimal.ZERO) > 0) {
                log.info("Penalty update for loan {}: inst #{} dpd={} rule={} penalty={}", 
                        schedule.getLoanId(), inst.getInstallmentNumber(), dpd, ruleApplied, penalty);
            }

            inst.applyLatePenalty(penalty);
        }
    }

    private BigDecimal computeLatePenalty(DelinquencyRule rule, BigDecimal overdueAmount) {
        if (rule.isPercentage()) {
            BigDecimal pct = rule.getPenaltyPercentage() != null ? rule.getPenaltyPercentage() : BigDecimal.ZERO;
            BigDecimal result = overdueAmount.multiply(pct)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            log.debug("Computed percentage penalty: base={}, pct={}, result={}", overdueAmount, pct, result);
            return result;
        }
        log.debug("Computed fixed penalty: amount={}", rule.getPenaltyAmount());
        return rule.getPenaltyAmount() != null ? rule.getPenaltyAmount() : BigDecimal.ZERO;
    }

    /**
     * Convenience wrapper for callers that only have a productCode hint —
     * re-runs both mutation + assessment in one shot.
     */
    public DelinquencyAssessment tickAndAssess(
            UUID tenantId,
            String productCode,
            RepaymentScheduleAggregate schedule,
            LocalDate asOf) {
        advanceStages(tenantId, productCode, schedule, asOf);
        return assess(tenantId, productCode, schedule, asOf);
    }

    /** Computes total late fee owed for a loan given the current delinquency state. */
    public BigDecimal totalLateFee(DelinquencyAssessment assessment) {
        return assessment.installments().stream()
                .map(InstallmentDelinquency::lateFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private int computeMaxDpd(RepaymentScheduleAggregate schedule, LocalDate asOf) {
        return schedule.getInstallments().stream()
                .filter(i -> i.getStatus() != InstallmentStatus.PAID
                        && i.getStatus() != InstallmentStatus.WAIVED)
                .mapToInt(i -> Math.max(0, daysBetween(i.getDueDate(), asOf)))
                .max()
                .orElse(0);
    }

    private int daysBetween(LocalDate due, LocalDate asOf) {
        return (int) (asOf.toEpochDay() - due.toEpochDay());
    }

    // ─────────────────────────────────────────────
    // Result types
    // ─────────────────────────────────────────────

    public record InstallmentDelinquency(
            UUID installmentId,
            int installmentNumber,
            LocalDate dueDate,
            int dpd,
            BigDecimal outstanding,
            DunningStage stage,
            BigDecimal lateFee
    ) {}

    public record DelinquencyAssessment(
            UUID tenantId,
            UUID loanId,
            String productCode,
            LocalDate asOf,
            int loanDpd,
            DunningStage loanStage,
            boolean reportToSimah,
            boolean simahDefaultFlag,
            boolean assignCollectionsAgent,
            boolean freezeWallet,
            String charityFundAccount,
            Map<String, Object> stageActions,
            List<InstallmentDelinquency> installments,
            DunningPolicyResolver.PolicySource policySource
    ) {}
}
