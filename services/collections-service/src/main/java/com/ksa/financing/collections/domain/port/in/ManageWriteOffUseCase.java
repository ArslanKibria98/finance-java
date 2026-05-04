package com.ksa.financing.collections.domain.port.in;

import com.ksa.financing.collections.domain.model.WriteOffRecord;
import com.ksa.financing.collections.domain.model.WriteOffTriggerType;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ManageWriteOffUseCase {

    /**
     * Re-evaluates the WRITE_OFFS DelinquencyRule (type=4) against all unpaid
     * installments on the loan and flips their {@code isEligibleForWriteOff} flag.
     * Returns the count of installments that became eligible.
     */
    int evaluateEligibility(UUID tenantId, UUID loanId, LocalDate asOf);

    /**
     * Executes write-off on all eligible installments of a loan (or a specific
     * installment when {@code command.installmentId} is set). Creates
     * {@link WriteOffRecord}s for audit and emits domain events.
     */
    List<WriteOffRecord> executeWriteOff(ExecuteWriteOffCommand command);

    /** Reverses a prior write-off — typically triggered by a late recovery. */
    WriteOffRecord reverseWriteOff(ReverseWriteOffCommand command);

    WriteOffRecord getWriteOff(UUID tenantId, UUID writeOffId);

    PageResponse<WriteOffRecord> listByTenant(UUID tenantId, PageQuery query);

    record ExecuteWriteOffCommand(
            UUID tenantId,
            UUID loanId,
            UUID installmentId,        // null = all eligible installments on the loan (unless invoiceId is provided)
            String invoiceId,          // optional: alternative to installmentId
            String reason,
            String approvalReference,
            WriteOffTriggerType triggerType,
            boolean override,          // bypass eligibility flag — admin override only
            LocalDate asOf,
            UUID actorId
    ) {}

    record ReverseWriteOffCommand(
            UUID tenantId,
            UUID writeOffId,
            String reason,
            UUID actorId
    ) {}
}
