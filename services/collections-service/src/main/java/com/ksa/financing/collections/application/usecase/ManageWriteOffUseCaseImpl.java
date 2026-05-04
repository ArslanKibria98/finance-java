package com.ksa.financing.collections.application.usecase;

import com.ksa.financing.collections.application.service.WriteOffEligibilityService;
import com.ksa.financing.collections.domain.model.DelinquencyRule;
import com.ksa.financing.collections.domain.model.DelinquencyType;
import com.ksa.financing.collections.domain.model.Installment;
import com.ksa.financing.collections.domain.model.InstallmentStatus;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.model.WriteOffId;
import com.ksa.financing.collections.domain.model.WriteOffRecord;
import com.ksa.financing.collections.domain.model.WriteOffStatus;
import com.ksa.financing.collections.domain.model.WriteOffTriggerType;
import com.ksa.financing.collections.domain.port.in.ManageWriteOffUseCase;
import com.ksa.financing.collections.domain.port.out.EventPublisher;
import com.ksa.financing.collections.domain.port.out.RepaymentScheduleRepository;
import com.ksa.financing.collections.domain.port.out.WriteOffRepository;
import com.ksa.financing.collections.application.service.DelinquencyRulesResolver;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageWriteOffUseCaseImpl implements ManageWriteOffUseCase {

    private final RepaymentScheduleRepository scheduleRepository;
    private final WriteOffRepository writeOffRepository;
    private final WriteOffEligibilityService eligibilityService;
    private final DelinquencyRulesResolver rulesResolver;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public int evaluateEligibility(UUID tenantId, UUID loanId, LocalDate asOf) {
        var schedule = loadSchedule(tenantId, loanId);
        int eligible = eligibilityService.evaluate(tenantId, schedule, asOf != null ? asOf : LocalDate.now());
        scheduleRepository.save(schedule);
        return eligible;
    }

    @Override
    @Transactional
    public List<WriteOffRecord> executeWriteOff(ExecuteWriteOffCommand command) {
        if (command.actorId() == null) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "actorId is required for write-off");
        }
        if (command.reason() == null || command.reason().isBlank()) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Write-off reason is required");
        }

        var schedule = loadSchedule(command.tenantId(), command.loanId());
        LocalDate asOf = command.asOf() != null ? command.asOf() : LocalDate.now();

        // Re-evaluate eligibility unless override is requested
        if (!command.override()) {
            eligibilityService.evaluate(command.tenantId(), schedule, asOf);
        }

        UUID ruleId = resolveWriteOffRuleId(command.tenantId(), schedule.getProductId());
        WriteOffTriggerType trigger = command.triggerType() != null
                ? command.triggerType()
                : (command.override() ? WriteOffTriggerType.MANUAL : WriteOffTriggerType.AUTO_RULE);

        List<Installment> writtenInstallments;
        if (command.installmentId() != null) {
            writtenInstallments = List.of(schedule.writeOffInstallment(
                    command.installmentId(), command.reason(), command.actorId(), asOf, command.override()));
        } else if (command.invoiceId() != null && !command.invoiceId().isBlank()) {
            String[] parts = command.invoiceId().split("-");
            int installmentNumber;
            try {
                installmentNumber = Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Invalid invoiceId format: " + command.invoiceId());
            }
            Installment targetInst = schedule.getInstallments().stream()
                    .filter(i -> i.getInstallmentNumber() == installmentNumber)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCodes.VALIDATION_FAILED, "Installment not found for invoice " + command.invoiceId()));
            writtenInstallments = List.of(schedule.writeOffInstallment(
                    targetInst.getId(), command.reason(), command.actorId(), asOf, command.override()));
        } else {
            writtenInstallments = schedule.writeOffEligibleInstallments(
                    command.reason(), command.actorId(), asOf, command.override());
            if (writtenInstallments.isEmpty()) {
                throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                        "No installments eligible for write-off on loan " + command.loanId());
            }
        }

        List<WriteOffRecord> records = new ArrayList<>(writtenInstallments.size());
        for (Installment inst : writtenInstallments) {
            WriteOffRecord record = WriteOffRecord.create(
                    command.tenantId(),
                    command.loanId(),
                    schedule.getId().getValue(),
                    inst.getId(),
                    ruleId,
                    inst.getWrittenOffPrincipal(),
                    inst.getWrittenOffProfit(),
                    inst.getWrittenOffFee(),
                    inst.getWrittenOffPenalty(),
                    inst.getDpd(),
                    trigger,
                    command.reason(),
                    command.approvalReference(),
                    asOf,
                    command.actorId());
            records.add(record);
        }
        records = writeOffRepository.saveAll(records);

        var savedSchedule = scheduleRepository.save(schedule);
        publishAndClear(savedSchedule);

        log.info("Write-off executed: tenant={} loan={} installments={} totalAmount={} actor={}",
                command.tenantId(), command.loanId(), writtenInstallments.size(),
                records.stream().map(WriteOffRecord::getTotalAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                command.actorId());

        return records;
    }

    @Override
    @Transactional
    public WriteOffRecord reverseWriteOff(ReverseWriteOffCommand command) {
        if (command.actorId() == null) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "actorId is required");
        }
        var record = writeOffRepository.findById(command.tenantId(), WriteOffId.of(command.writeOffId()))
                .orElseThrow(() -> NotFoundException.forEntity("WriteOffRecord", command.writeOffId().toString()));

        if (record.getStatus() == WriteOffStatus.REVERSED) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Write-off already reversed");
        }

        record.reverse(command.reason(), command.actorId());
        writeOffRepository.save(record);

        // Reverse on the installment if one is linked
        if (record.getInstallmentId() != null) {
            var schedule = scheduleRepository.findActiveByLoanId(command.tenantId(), record.getLoanId())
                    .orElseThrow(() -> NotFoundException.forEntity("RepaymentSchedule", record.getLoanId().toString()));
            schedule.getInstallments().stream()
                    .filter(i -> i.getId().equals(record.getInstallmentId()))
                    .findFirst()
                    .ifPresent(inst -> {
                        if (inst.getStatus() == InstallmentStatus.WRITTEN_OFF) {
                            int dpd = (int) (LocalDate.now().toEpochDay() - inst.getDueDate().toEpochDay());
                            inst.reverseWriteOff(Math.max(0, dpd));
                        }
                    });
            scheduleRepository.save(schedule);
        }

        log.info("Write-off reversed: id={} actor={} reason={}",
                command.writeOffId(), command.actorId(), command.reason());
        return record;
    }

    @Override
    @Transactional(readOnly = true)
    public WriteOffRecord getWriteOff(UUID tenantId, UUID writeOffId) {
        return writeOffRepository.findById(tenantId, WriteOffId.of(writeOffId))
                .orElseThrow(() -> NotFoundException.forEntity("WriteOffRecord", writeOffId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WriteOffRecord> listByTenant(UUID tenantId, PageQuery query) {
        return writeOffRepository.findAllByTenant(tenantId, query);
    }

    // ─────────────────────────────────────────────

    private RepaymentScheduleAggregate loadSchedule(UUID tenantId, UUID loanId) {
        return scheduleRepository.findActiveByLoanId(tenantId, loanId)
                .orElseThrow(() -> NotFoundException.forEntity("RepaymentSchedule", loanId.toString()));
    }

    private UUID resolveWriteOffRuleId(UUID tenantId, UUID productId) {
        if (productId == null) return null;
        return rulesResolver.rule(tenantId, productId, DelinquencyType.WRITE_OFFS)
                .map(DelinquencyRule::getId)
                .map(id -> id.getValue())
                .orElse(null);
    }

    private void publishAndClear(RepaymentScheduleAggregate schedule) {
        var events = schedule.getUncommittedEvents();
        if (events.isEmpty()) return;
        try {
            eventPublisher.publishAll(new java.util.ArrayList<>(events));
        } catch (Exception ex) {
            log.warn("Failed to publish {} write-off event(s): {}", events.size(), ex.getMessage());
        }
        schedule.markEventsAsCommitted();
    }
}
