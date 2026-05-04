package com.ksa.financing.lending.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;
import com.ksa.financing.lending.domain.model.LoanApplicationId;
import com.ksa.financing.lending.domain.port.in.ManageLoanApplicationUseCase;
import com.ksa.financing.lending.domain.port.out.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public PageResponse<LoanApplicationAggregate> listApplications(UUID tenantId, PageQuery query) {
        log.debug("Listing loan applications for tenant: {}", tenantId);
        return applicationRepository.findAllByTenant(tenantId, query);
    }

    @Override
    public PageResponse<LoanApplicationAggregate> listApplicationsByCustomer(UUID tenantId, UUID customerId, PageQuery query) {
        log.debug("Listing loan applications for customer: {}", customerId);
        return applicationRepository.findByCustomer(tenantId, customerId, query);
    }

    @Override
    @Transactional
    public void cancelApplication(UUID tenantId, UUID applicationId, UUID cancelledBy) {
        log.info("Cancelling loan application: {} by {}", applicationId, cancelledBy);
        var aggregate = applicationRepository.findById(tenantId, LoanApplicationId.of(applicationId))
                .orElseThrow(() -> NotFoundException.forEntity("LoanApplication", applicationId.toString()));
        
        if (!aggregate.getStatus().isTerminal()) {
            aggregate.cancel(cancelledBy);
            applicationRepository.save(aggregate);
            log.info("Application {} status updated to CANCELLED", applicationId);
        } else {
            log.warn("Application {} is already in terminal state: {}", applicationId, aggregate.getStatus());
        }
    }
}
