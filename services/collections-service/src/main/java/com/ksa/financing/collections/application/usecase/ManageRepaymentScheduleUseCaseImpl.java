package com.ksa.financing.collections.application.usecase;

import com.ksa.financing.collections.domain.model.Installment;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.model.RepaymentScheduleId;
import com.ksa.financing.collections.domain.port.in.ManageRepaymentScheduleUseCase;
import com.ksa.financing.collections.domain.port.out.EventPublisher;
import com.ksa.financing.collections.domain.port.out.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageRepaymentScheduleUseCaseImpl implements ManageRepaymentScheduleUseCase {

    private final RepaymentScheduleRepository scheduleRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public RepaymentScheduleAggregate createSchedule(CreateScheduleCommand command) {
        log.info("Creating repayment schedule for loan: {} tenant: {}", command.loanId(), command.tenantId());

        if (scheduleRepository.existsByScheduleNumber(command.tenantId(), command.scheduleNumber())) {
            throw new IllegalStateException("Schedule number already exists: " + command.scheduleNumber());
        }

        List<Installment> installments = command.installments().stream()
                .map(entry -> Installment.create(
                        command.tenantId(),
                        null,
                        command.loanId(),
                        entry.installmentNumber(),
                        entry.dueDate(),
                        entry.principalAmount(),
                        entry.profitAmount(),
                        entry.feeAmount()))
                .collect(Collectors.toList());

        var schedule = RepaymentScheduleAggregate.create(
                command.tenantId(),
                command.loanId(),
                command.productId(),
                command.scheduleNumber(),
                command.totalPrincipal(),
                command.totalProfit(),
                command.totalFee(),
                command.firstDueDate(),
                command.lastDueDate(),
                installments,
                command.createdBy());

        var saved = scheduleRepository.save(schedule);
        eventPublisher.publishAll(saved.getUncommittedEvents());
        saved.markEventsAsCommitted();

        log.info("Repayment schedule created: {} for loan: {}", saved.getId(), command.loanId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public RepaymentScheduleAggregate getSchedule(UUID tenantId, UUID scheduleId) {
        return scheduleRepository.findById(tenantId, RepaymentScheduleId.of(scheduleId))
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
    }

    @Override
    @Transactional(readOnly = true)
    public RepaymentScheduleAggregate getScheduleByLoan(UUID tenantId, UUID loanId) {
        return scheduleRepository.findActiveByLoanId(tenantId, loanId)
                .orElseThrow(() -> new IllegalArgumentException("Active schedule not found for loan: " + loanId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Installment> getInstallments(UUID tenantId, UUID scheduleId) {
        var schedule = getSchedule(tenantId, scheduleId);
        return schedule.getInstallments();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Installment> getOverdueInstallments(UUID tenantId) {
        return List.of();
    }

    @Override
    @Transactional
    public void processDueDateTransitions(LocalDate asOf) {
        log.info("Processing due date transitions as of: {}", asOf);
    }
}
