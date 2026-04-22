package com.ksa.financing.collections.domain.port.in;

import com.ksa.financing.collections.domain.model.Installment;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ManageRepaymentScheduleUseCase {

    record CreateScheduleCommand(
            UUID tenantId,
            UUID loanId,
            UUID productId,
            String scheduleNumber,
            BigDecimal totalPrincipal,
            BigDecimal totalProfit,
            LocalDate firstDueDate,
            LocalDate lastDueDate,
            List<InstallmentEntry> installments,
            UUID createdBy
    ) {
        public record InstallmentEntry(
                int installmentNumber,
                LocalDate dueDate,
                BigDecimal principalAmount,
                BigDecimal profitAmount,
                BigDecimal feeAmount
        ) {}
    }

    RepaymentScheduleAggregate createSchedule(CreateScheduleCommand command);

    RepaymentScheduleAggregate getSchedule(UUID tenantId, UUID scheduleId);

    RepaymentScheduleAggregate getScheduleByLoan(UUID tenantId, UUID loanId);

    List<Installment> getInstallments(UUID tenantId, UUID scheduleId);

    List<Installment> getOverdueInstallments(UUID tenantId);

    void processDueDateTransitions(LocalDate asOf);
}
