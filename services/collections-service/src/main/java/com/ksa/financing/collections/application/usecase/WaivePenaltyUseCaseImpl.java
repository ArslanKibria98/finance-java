package com.ksa.financing.collections.application.usecase;

import com.ksa.financing.collections.domain.model.PenaltyWaiver;
import com.ksa.financing.collections.domain.model.PenaltyWaiverId;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.port.in.WaivePenaltyUseCase;
import com.ksa.financing.collections.domain.port.out.EventPublisher;
import com.ksa.financing.collections.domain.port.out.PenaltyWaiverRepository;
import com.ksa.financing.collections.domain.port.out.RepaymentScheduleRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaivePenaltyUseCaseImpl implements WaivePenaltyUseCase {

    private final RepaymentScheduleRepository scheduleRepository;
    private final PenaltyWaiverRepository waiverRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public PenaltyWaiver waivePenalty(WaivePenaltyCommand command) {
        if (command.actorId() == null) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "actorId is required");
        }
        if (command.reason() == null || command.reason().isBlank()) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Waiver reason is required");
        }
        if (command.installmentId() == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "installmentId is required");
        }

        var schedule = loadSchedule(command.tenantId(), command.loanId());

        BigDecimal amountToWaive = resolveAmount(command, schedule);
        if (amountToWaive.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "No penalty remaining to waive on installment");
        }

        PenaltyWaiver waiver = schedule.waivePenaltyOnInstallment(
                command.installmentId(), amountToWaive,
                command.reason(), command.approvalReference(), command.actorId());

        PenaltyWaiver saved = waiverRepository.save(waiver);
        var savedSchedule = scheduleRepository.save(schedule);
        publishAndClear(savedSchedule);

        log.info("Penalty waived: tenant={} loan={} installment={} amount={} type={} actor={}",
                command.tenantId(), command.loanId(), command.installmentId(),
                amountToWaive, waiver.getWaiverType(), command.actorId());

        return saved;
    }

    @Override
    public PenaltyWaiver getWaiver(UUID tenantId, UUID waiverId) {
        return waiverRepository.findById(tenantId, PenaltyWaiverId.of(waiverId))
                .orElseThrow(() -> NotFoundException.forEntity("PenaltyWaiver", waiverId.toString()));
    }

    @Override
    public List<PenaltyWaiver> listByLoan(UUID tenantId, UUID loanId) {
        return waiverRepository.findByLoanId(tenantId, loanId);
    }

    @Override
    public List<PenaltyWaiver> listByInstallment(UUID tenantId, UUID installmentId) {
        return waiverRepository.findByInstallmentId(tenantId, installmentId);
    }

    @Override
    public List<PenaltyWaiver> listByDateRange(UUID tenantId, LocalDate fromDate, LocalDate toDate) {
        return waiverRepository.findByDateRange(tenantId, fromDate, toDate);
    }

    // ─────────────────────────────────────────────

    private BigDecimal resolveAmount(WaivePenaltyCommand command, RepaymentScheduleAggregate schedule) {
        var installment = schedule.getInstallments().stream()
                .filter(i -> i.getId().equals(command.installmentId()))
                .findFirst()
                .orElseThrow(() -> NotFoundException.forEntity("Installment", command.installmentId().toString()));

        BigDecimal remaining = installment.getRemainingPenalty();
        if (command.amount() == null) {
            return remaining;                     // full waiver
        }
        if (command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Waiver amount must be positive");
        }
        return command.amount().min(remaining);   // never exceed remaining penalty
    }

    private RepaymentScheduleAggregate loadSchedule(UUID tenantId, UUID loanId) {
        return scheduleRepository.findActiveByLoanId(tenantId, loanId)
                .orElseThrow(() -> NotFoundException.forEntity("RepaymentSchedule", loanId.toString()));
    }

    private void publishAndClear(RepaymentScheduleAggregate schedule) {
        var events = schedule.getUncommittedEvents();
        if (events.isEmpty()) return;
        try {
            eventPublisher.publishAll(new java.util.ArrayList<>(events));
        } catch (Exception ex) {
            log.warn("Failed to publish {} waiver event(s): {}", events.size(), ex.getMessage());
        }
        schedule.markEventsAsCommitted();
    }
}
