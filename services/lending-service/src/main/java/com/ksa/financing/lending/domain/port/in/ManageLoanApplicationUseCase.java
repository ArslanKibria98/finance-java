package com.ksa.financing.lending.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;

import java.util.UUID;

/**
 * Read-only use case for loan applications.
 * Write operations are handled by Temporal activities (LoanApplicationActivityImpl)
 * which directly access the repository and aggregate methods.
 */
public interface ManageLoanApplicationUseCase {

    LoanApplicationAggregate getApplication(UUID tenantId, UUID applicationId);

    PageResponse<LoanApplicationAggregate> listApplications(UUID tenantId, PageQuery query);

    PageResponse<LoanApplicationAggregate> listApplicationsByCustomer(UUID tenantId, UUID customerId, PageQuery query);

    /**
     * Cancel a non-terminal application directly in DB.
     * Used by the cancel endpoint and auto-cancel-on-re-initiate logic.
     */
    void cancelApplication(UUID tenantId, UUID applicationId, UUID cancelledBy);
}
