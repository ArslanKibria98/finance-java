package com.ksa.financing.collections.domain.port.in;

import com.ksa.financing.collections.domain.model.PenaltyWaiverRequest;
import java.math.BigDecimal;
import java.util.UUID;

public interface RequestPenaltyWaiverUseCase {
    PenaltyWaiverRequest submitRequest(SubmitWaiverRequestCommand command);

    record SubmitWaiverRequestCommand(
        UUID tenantId,
        UUID loanId,
        UUID applicationId,
        String invoiceId,
        UUID installmentId,
        BigDecimal amount,
        String reason,
        UUID customerId
    ) {}
}
