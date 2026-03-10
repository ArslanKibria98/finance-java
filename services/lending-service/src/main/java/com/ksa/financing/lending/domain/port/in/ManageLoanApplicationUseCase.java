package com.ksa.financing.lending.domain.port.in;

import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;
import com.ksa.financing.lending.domain.model.ShariaStructure;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageLoanApplicationUseCase {

    LoanApplicationAggregate createApplication(CreateApplicationCommand command);

    LoanApplicationAggregate submitApplication(SubmitApplicationCommand command);

    LoanApplicationAggregate getApplication(UUID tenantId, UUID applicationId);

    List<LoanApplicationAggregate> listApplications(UUID tenantId);

    // ==================== COMMANDS ====================

    record CreateApplicationCommand(
            UUID tenantId,
            UUID customerId,
            UUID productId,
            String productCode,
            ShariaStructure shariaStructure,
            BigDecimal requestedAmount,
            int requestedTenureMonths,
            UUID partnerId,
            UUID leadId,
            UUID createdBy,
            String idempotencyKey
    ) {}

    record SubmitApplicationCommand(
            UUID tenantId,
            UUID applicationId,
            UUID submittedBy
    ) {}
}
