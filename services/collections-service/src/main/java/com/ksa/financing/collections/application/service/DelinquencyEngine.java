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
            if (dpd <= 0) continue;

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

        applyLatePaymentPenalties(tenantId, schedule, asOf);

        log.debug("Advanced stages for loan {} (tenant {}) using policy source={} graceDays={}",
                schedule.getLoanId(), tenantId, resolved.source(), graceDays);
    }

    /**
     * Applies the per-product LATE_PAYMENT DelinquencyRule (type=3) to every
     * overdue installment. Idempotent — overwrites {@code Installment.latePenalty}
     * with the currently-computed value so repeated ticks converge.
     *
     * Sharia: the penalty is collected through the waterfall's fee bucket and
     * routed to the rule's {@code charityFundAccount} during settlement — never
     * to bank revenue.
     */
    private void applyLatePaymentPenalties(UUID tenantId, RepaymentScheduleAggregate schedule, LocalDate asOf) {
        UUID productId = schedule.getProductId();
        if (productId == null) {
            return;  // no product → fall back to DunningPolicy-only flow
        }
        Optional<DelinquencyRule> ruleOpt = rulesResolver.rule(tenantId, productId, DelinquencyType.LATE_PAYMENT);
        if (ruleOpt.isEmpty()) {
            return;
        }
        DelinquencyRule rule = ruleOpt.get();

        for (Installment inst : schedule.getInstallments()) {
            if (inst.getStatus() == InstallmentStatus.PAID || inst.getStatus() == InstallmentStatus.WAIVED) {
                continue;
            }
            int dpd = daysBetween(inst.getDueDate(), asOf);
            if (dpd <= 0) {
                inst.applyLatePenalty(BigDecimal.ZERO);
                continue;
            }
            if (rule.getTillDay() > 0 && (dpd < rule.getFromDay() || dpd > rule.getTillDay())) {
                continue;  // outside the rule's DPD window — leave whatever was set earlier
            }
            BigDecimal penalty = computeLatePenalty(rule, inst.getOutstandingPrincipal().add(inst.getOutstandingProfit()));
            inst.applyLatePenalty(penalty);
        }
    }

    private BigDecimal computeLatePenalty(DelinquencyRule rule, BigDecimal overdueAmount) {
        if (rule.isPercentage()) {
            BigDecimal pct = rule.getPenaltyPercentage() != null ? rule.getPenaltyPercentage() : BigDecimal.ZERO;
            return overdueAmount.multiply(pct)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
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
