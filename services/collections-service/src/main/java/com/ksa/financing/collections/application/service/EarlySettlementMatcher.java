package com.ksa.financing.collections.application.service;

import com.ksa.financing.collections.domain.model.DelinquencyRule;
import com.ksa.financing.collections.domain.model.EarlySettlementConfig;
import com.ksa.financing.collections.domain.model.Installment;
import com.ksa.financing.collections.domain.model.InstallmentStatus;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Single source of truth for "does this installment qualify for early settlement right now?"
 * Every consumer — settlement quote, installment response flag, cross-service eligibility
 * endpoint — must go through this matcher so the answer is consistent.
 *
 * Match rules (derived from the EARLY_SETTLEMENT DelinquencyRule, type=1):
 *   - Flat mode (isCustom=false):
 *       eligible iff  rule.fromDay <= today_DPD <= rule.tillDay
 *   - Custom mode (isCustom=true):
 *       eligible iff  at least one active EarlySettlementConfig row matches the installment
 *       by invoice number AND today_DPD is in that row's [fromDay, tillDay] window.
 *
 * "Satisfied" means: installment is not PAID/WAIVED, rule exists, and all of the above hold.
 */
@Component
public class EarlySettlementMatcher {

    public Optional<Match> match(DelinquencyRule rule, Installment inst, LocalDate today) {
        if (rule == null) return Optional.empty();
        if (inst.getStatus() == InstallmentStatus.PAID || inst.getStatus() == InstallmentStatus.WAIVED) {
            return Optional.empty();
        }
        int dpd = (int) (today.toEpochDay() - inst.getDueDate().toEpochDay());
        
        // --- Principle Based Strategy (Image 3) ---
        if (rule.getSettlementStrategy() == com.ksa.financing.collections.domain.model.EarlySettlementStrategy.PRINCIPLE_BASED) {
            // Principle based settlement is usually a loan-level flat discount.
            // All future installments are considered "eligible" for this process.
            return Optional.of(new Match(
                    BigDecimal.ZERO, // Per-installment discount is 0 (handled at loan level)
                    BigDecimal.ZERO,
                    null,
                    false));
        }

        if (!rule.isCustom()) {
            if (!dpdInWindow(dpd, rule.getFromDay(), rule.getTillDay())) return Optional.empty();
            return Optional.of(new Match(
                    rule.isPercentage() ? rule.getPenaltyPercentage() : null,
                    rule.isPercentage() ? null : rule.getPenaltyAmount(),
                    rule.getTillDay(),
                    rule.isPercentage()));
        }

        return rule.listEarlySettlementConfigs().stream()
                .filter(c -> c.getRecordState() == 1)
                .filter(c -> matchesConfig(c, inst.getInstallmentNumber()))
                .filter(c -> dpdInWindow(dpd, c.getFromDay(), c.getTillDay()))
                .findFirst()
                .map(c -> new Match(
                        c.isPercentage() ? c.getDiscountPercentage() : null,
                        c.isPercentage() ? null : c.getDiscountAmount(),
                        c.getTillDay(),
                        c.isPercentage()));
    }

    /**
     * Loan-level strict check: rule exists AND at least one unpaid installment currently
     * satisfies every condition defined by the rule (invoice scope + DPD window).
     */
    public boolean anyInstallmentEligible(DelinquencyRule rule, RepaymentScheduleAggregate schedule, LocalDate today) {
        if (rule == null || schedule == null) return false;
        return schedule.getInstallments().stream()
                .anyMatch(i -> match(rule, i, today).isPresent());
    }

    private boolean matchesConfig(EarlySettlementConfig cfg, int installmentNumber) {
        if (cfg.isRange()) {
            Integer min = cfg.getMinInvoiceOrder();
            Integer max = cfg.getMaxInvoiceOrder();
            return min != null && max != null && installmentNumber >= min && installmentNumber <= max;
        }
        Integer io = cfg.getInvoiceOrder();
        return io != null && io == installmentNumber;
    }

    /**
     * EARLY_SETTLEMENT DPD window semantics:
     *   - fromDay <= 0 means "no lower bound" — future invoices (DPD < 0) qualify. This matches
     *     the customer intuition of "paying an invoice early before its due date."
     *   - tillDay > 0 is the upper ceiling (e.g. tillDay=30 allows up to 30 DPD past due).
     *   - Both <= 0 collapses to "only on or before due date" (DPD <= 0).
     */
    private boolean dpdInWindow(int dpd, int fromDay, int tillDay) {
        if (tillDay <= 0 && fromDay <= 0) return dpd <= 0;
        if (fromDay <= 0) return dpd <= tillDay;
        return dpd >= fromDay && dpd <= tillDay;
    }

    public record Match(
            BigDecimal discountPercentage,
            BigDecimal discountAmount,
            Integer validUntilDay,
            boolean isPercentage
    ) {}
}
