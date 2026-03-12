package com.ksa.financing.lending.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;
import com.ksa.financing.lending.domain.model.LoanApplicationId;
import com.ksa.financing.lending.domain.port.in.ManageLoanApplicationUseCase;
import com.ksa.financing.lending.domain.port.out.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Read-only use case implementation for loan applications.
 * Write operations are handled by Temporal activities (LoanApplicationActivityImpl)
 * which directly access the repository and aggregate methods.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManageLoanApplicationUseCaseImpl implements ManageLoanApplicationUseCase {

    private final LoanApplicationRepository applicationRepository;

    @Override
    public LoanApplicationAggregate getApplication(UUID tenantId, UUID applicationId) {
        log.debug("Getting loan application: {}", applicationId);
        return applicationRepository.findById(tenantId, LoanApplicationId.of(applicationId))
                .orElseThrow(() -> NotFoundException.forEntity("LoanApplication", applicationId.toString()));
    }

    @Override
    public List<LoanApplicationAggregate> listApplications(UUID tenantId) {
        log.debug("Listing loan applications for tenant: {}", tenantId);
        return applicationRepository.findAllByTenant(tenantId);
    }

    @Override
    public List<LoanApplicationAggregate> listApplicationsByCustomer(UUID tenantId, UUID customerId) {
        log.debug("Listing loan applications for customer: {}", customerId);
        return applicationRepository.findByCustomer(tenantId, customerId);
    }
}
