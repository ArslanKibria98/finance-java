package com.ksa.financing.collections.application.service;

import com.ksa.financing.collections.domain.model.Installment;
import com.ksa.financing.collections.domain.model.InstallmentStatus;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.port.out.EventPublisher;
import com.ksa.financing.collections.domain.port.out.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Daily job that publishes InstallmentDueSoon events for installments
 * due within the next N days (default 3). Used for due reminders.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DueSoonDetectionService {

    private final RepaymentScheduleRepository scheduleRepository;
    private final EventPublisher eventPublisher;

    @Value("${collections.due-soon.days-ahead:3}")
    private int daysAhead;

    /** Daily cron: runs at 07:00 server time (after overdue detection). */
    @Scheduled(cron = "${collections.due-soon.cron:0 0 7 * * *}")
    @Transactional
    public void runDailyScan() {
        scan(LocalDate.now());
    }

    @Transactional
    public int scan(LocalDate asOf) {
        log.info("DueSoonDetectionService scan started: asOf={} daysAhead={}", asOf, daysAhead);

        var schedules = scheduleRepository.findAllActive();
        int dueSoonCount = 0;
        List<Object> events = new ArrayList<>();

        LocalDate cutoff = asOf.plusDays(daysAhead);

        for (var schedule : schedules) {
            for (Installment inst : schedule.getInstallments()) {
                if (inst.getStatus() == InstallmentStatus.PAID
                        || inst.getStatus() == InstallmentStatus.WAIVED) continue;

                LocalDate dueDate = inst.getDueDate();
                if (dueDate == null) continue;

                if (!dueDate.isBefore(asOf) && !dueDate.isAfter(cutoff)) {
                    int daysUntilDue = (int) ChronoUnit.DAYS.between(asOf, dueDate);
                    events.add(new RepaymentScheduleAggregate.InstallmentDueSoon(
                            schedule.getId(),
                            schedule.getTenantId(),
                            schedule.getLoanId(),
                            inst.getId(),
                            inst.getInstallmentNumber(),
                            inst.getTotalAmount(),
                            dueDate,
                            daysUntilDue
                    ));
                    dueSoonCount++;
                }
            }
        }

        if (!events.isEmpty()) {
            eventPublisher.publishAll(events);
        }

        log.info("DueSoonDetectionService scan completed: schedulesScanned={} dueSoonFound={}",
                schedules.size(), dueSoonCount);
        return dueSoonCount;
    }
}
