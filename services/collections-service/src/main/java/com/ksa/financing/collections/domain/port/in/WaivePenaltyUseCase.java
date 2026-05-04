package com.ksa.financing.collections.domain.port.in;

import com.ksa.financing.collections.domain.model.PenaltyWaiver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WaivePenaltyUseCase {

    /**
     * Waives penalty on a single installment. {@code amount=null} means full waiver.
     */
    PenaltyWaiver waivePenalty(WaivePenaltyCommand command);

    PenaltyWaiver getWaiver(UUID tenantId, UUID waiverId);

    List<PenaltyWaiver> listByLoan(UUID tenantId, UUID loanId);

    List<PenaltyWaiver> listByInstallment(UUID tenantId, UUID installmentId);

    List<PenaltyWaiver> listByDateRange(UUID tenantId, LocalDate fromDate, LocalDate toDate);

    record WaivePenaltyCommand(
            UUID tenantId,
            UUID loanId,
            UUID installmentId,
            BigDecimal amount,         // null = full
            String reason,
            String approvalReference,
            UUID actorId
    ) {}
}
