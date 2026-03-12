package com.ksa.financing.lending.domain.port.in;

import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;

import java.util.List;
import java.util.UUID;

/**
 * Read-only use case for loan applications.
 * Write operations are handled by Temporal activities (LoanApplicationActivityImpl)
 * which directly access the repository and aggregate methods.
 */
public interface ManageLoanApplicationUseCase {

    LoanApplicationAggregate getApplication(UUID tenantId, UUID applicationId);

    List<LoanApplicationAggregate> listApplications(UUID tenantId);

    List<LoanApplicationAggregate> listApplicationsByCustomer(UUID tenantId, UUID customerId);
}
