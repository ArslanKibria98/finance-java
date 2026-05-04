package com.ksa.financing.collections.application.service;

import com.ksa.financing.collections.domain.model.Installment;
import com.ksa.financing.collections.domain.model.InstallmentStatus;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.port.out.EventPublisher;
import com.ksa.financing.collections.domain.port.out.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Daily job that advances installment stages, recomputes DPD,
 * and publishes a LoanOverdue event per schedule that has overdue installments.
 * Downstream ledger-service consumes the event and posts a provision journal entry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OverdueDetectionService {

    private final RepaymentScheduleRepository scheduleRepository;
    private final DelinquencyEngine delinquencyEngine;
    private final EventPublisher eventPublisher;

    /** Daily cron: runs at 06:00 server time. Override via kafka/ops env. */
    @Scheduled(cron = "${collections.overdue.cron:0 0 6 * * *}")
    @Transactional
    public void runDailyScan() {
        scan(LocalDate.now());
    }

    /** Manual entrypoint — used by operator endpoints and tests. */
    @Transactional
    public int scan(LocalDate asOf) {
        log.info("OverdueDetectionService scan started: asOf={}", asOf);
        var schedules = scheduleRepository.findAllActive();
        int overdueCount = 0;

        for (var schedule : schedules) {
            try {
                if (advanceAndEmit(schedule, asOf)) {
                    overdueCount++;
                }
            } catch (Exception e) {
                log.error("Overdue scan failed for schedule={} loan={}: {}",
                        schedule.getId(), schedule.getLoanId(), e.getMessage(), e);
            }
        }

        log.info("OverdueDetectionService scan completed: schedulesScanned={} overdueFound={}",
                schedules.size(), overdueCount);
        return overdueCount;
    }

    private boolean advanceAndEmit(RepaymentScheduleAggregate schedule, LocalDate asOf) {
        delinquencyEngine.advanceStages(schedule.getTenantId(), null, schedule, asOf);
        scheduleRepository.save(schedule);

        var stats = computeOverdueStats(schedule, asOf);
        if (stats.overdueInstallments() == 0
                || stats.overdueAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        var event = new RepaymentScheduleAggregate.LoanOverdue(
                schedule.getId(),
                schedule.getTenantId(),
                schedule.getLoanId(),
                stats.maxDpd(),
                stats.overdueInstallments(),
                stats.overdueAmount(),
                asOf
        );
        eventPublisher.publishAll(List.<Object>of(event));
        log.info("Published LoanOverdue: loan={} dpd={} overdueAmount={}",
                schedule.getLoanId(), stats.maxDpd(), stats.overdueAmount());
        return true;
    }

    private OverdueStats computeOverdueStats(RepaymentScheduleAggregate schedule, LocalDate asOf) {
        int maxDpd = 0;
        int overdueInstallments = 0;
        BigDecimal overdueAmount = BigDecimal.ZERO;

        for (Installment inst : schedule.getInstallments()) {
            if (inst.getStatus() != InstallmentStatus.OVERDUE) continue;
            int dpd = (int) (asOf.toEpochDay() - inst.getDueDate().toEpochDay());
            if (dpd > maxDpd) maxDpd = dpd;
            overdueInstallments++;
            overdueAmount = overdueAmount.add(inst.getOutstandingAmount());
        }
        return new OverdueStats(maxDpd, overdueInstallments, overdueAmount);
    }

    private record OverdueStats(int maxDpd, int overdueInstallments, BigDecimal overdueAmount) {}
}
