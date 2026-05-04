package com.ksa.financing.collections.application.service;

import com.ksa.financing.collections.domain.model.DelinquencyRule;
import com.ksa.financing.collections.domain.model.DelinquencyType;
import com.ksa.financing.collections.domain.model.Installment;
import com.ksa.financing.collections.domain.model.InstallmentStatus;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Evaluates write-off eligibility for installments against the per-product
 * WRITE_OFFS DelinquencyRule (type=4).
 *
 * Eligibility rule:
 *   - installment must be unpaid (not PAID / WAIVED / WRITTEN_OFF)
 *   - installment DPD must fall within [rule.fromDay, rule.tillDay]
 *     (if tillDay=0, the window is open-ended — i.e. dpd >= fromDay)
 *
 * Missing rule ⇒ no installment is eligible (fail-closed).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WriteOffEligibilityService {

    private final DelinquencyRulesResolver rulesResolver;

    /**
     * Re-evaluates every installment in the schedule and flips the
     * {@code isEligibleForWriteOff} flag. Mutates the aggregate in place —
     * caller is responsible for persisting via {@code scheduleRepository.save}.
     *
     * @return number of installments that are currently eligible.
     */
    public int evaluate(UUID tenantId, RepaymentScheduleAggregate schedule, LocalDate asOf) {
        if (schedule == null) return 0;
        UUID productId = schedule.getProductId();
        if (productId == null) {
            clearAllFlags(schedule);
            return 0;
        }

        Optional<DelinquencyRule> ruleOpt = rulesResolver.rule(tenantId, productId, DelinquencyType.WRITE_OFFS);
        if (ruleOpt.isEmpty()) {
            clearAllFlags(schedule);
            log.debug("No WRITE_OFFS rule for tenant={} product={} — no installments eligible", tenantId, productId);
            return 0;
        }

        DelinquencyRule rule = ruleOpt.get();
        int fromDay = rule.getFromDay();
        int tillDay = rule.getTillDay();
        int eligibleCount = 0;

        for (Installment inst : schedule.getInstallments()) {
            var st = inst.getStatus();
            if (st == InstallmentStatus.PAID
                    || st == InstallmentStatus.WAIVED
                    || st == InstallmentStatus.WRITTEN_OFF) {
                inst.setEligibleForWriteOff(false);
                continue;
            }
            int dpd = daysBetween(inst.getDueDate(), asOf);
            boolean withinWindow;
            if (tillDay <= 0) {
                withinWindow = dpd >= fromDay;
            } else {
                withinWindow = dpd >= fromDay && dpd <= tillDay;
            }
            inst.setEligibleForWriteOff(withinWindow);
            if (withinWindow) eligibleCount++;
        }

        log.debug("Evaluated write-off eligibility for loan={} schedule={} asOf={} rule[{}..{}] eligible={}",
                schedule.getLoanId(), schedule.getId().getValue(), asOf, fromDay, tillDay, eligibleCount);
        return eligibleCount;
    }

    private void clearAllFlags(RepaymentScheduleAggregate schedule) {
        for (Installment inst : schedule.getInstallments()) {
            inst.setEligibleForWriteOff(false);
        }
    }

    private int daysBetween(LocalDate due, LocalDate asOf) {
        return (int) (asOf.toEpochDay() - due.toEpochDay());
    }
}
