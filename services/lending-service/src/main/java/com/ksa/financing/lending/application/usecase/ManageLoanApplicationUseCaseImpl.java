package com.ksa.financing.lending.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;
import com.ksa.financing.lending.domain.model.LoanApplicationId;
import com.ksa.financing.lending.domain.model.ShariaStructure;
import com.ksa.financing.lending.domain.port.in.ManageLoanApplicationUseCase;
import com.ksa.financing.lending.domain.port.out.EventPublisher;
import com.ksa.financing.lending.domain.port.out.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ManageLoanApplicationUseCaseImpl implements ManageLoanApplicationUseCase {

    private final LoanApplicationRepository applicationRepository;
    private final EventPublisher eventPublisher;

    @Override
    public LoanApplicationAggregate createApplication(CreateApplicationCommand command) {
        log.info("Creating loan application for tenant: {}, customer: {}",
                command.tenantId(), command.customerId());

        // Idempotency check: return existing application if same key was already used
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            var existing = applicationRepository.findByIdempotencyKey(
                    command.tenantId(), command.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Returning existing loan application for idempotencyKey={}",
                        command.idempotencyKey());
                return existing.get();
            }
        }

        var applicationNumber = applicationRepository.generateApplicationNumber(command.tenantId());

        var aggregate = LoanApplicationAggregate.create(
                command.tenantId(),
                applicationNumber,
                command.customerId(),
                command.productId(),
                command.productCode(),
                command.shariaStructure(),
                command.requestedAmount(),
                command.requestedTenureMonths(),
                command.createdBy()
        );

        if (command.partnerId() != null) {
            aggregate.setPartner(command.partnerId(), command.leadId());
        }

        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            aggregate.setIdempotencyKey(command.idempotencyKey());
        }

        aggregate = applicationRepository.save(aggregate);

        eventPublisher.publishAll(aggregate.getUncommittedEvents());
        aggregate.markEventsAsCommitted();

        log.info("Created loan application: {}", aggregate.getApplicationNumber());
        return aggregate;
    }

    @Override
    public LoanApplicationAggregate submitApplication(SubmitApplicationCommand command) {
        log.info("Submitting loan application: {}", command.applicationId());

        var aggregate = findApplication(command.tenantId(), command.applicationId());

        aggregate.submit(command.submittedBy());

        aggregate = applicationRepository.save(aggregate);

        eventPublisher.publishAll(aggregate.getUncommittedEvents());
        aggregate.markEventsAsCommitted();

        log.info("Submitted loan application: {}", aggregate.getApplicationNumber());
        return aggregate;
    }

    @Override
    @Transactional(readOnly = true)
    public LoanApplicationAggregate getApplication(UUID tenantId, UUID applicationId) {
        log.debug("Getting loan application: {}", applicationId);
        return findApplication(tenantId, applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanApplicationAggregate> listApplications(UUID tenantId) {
        log.debug("Listing loan applications for tenant: {}", tenantId);
        return applicationRepository.findAllByTenant(tenantId);
    }

    private LoanApplicationAggregate findApplication(UUID tenantId, UUID applicationId) {
        return applicationRepository.findById(tenantId, LoanApplicationId.of(applicationId))
                .orElseThrow(() -> NotFoundException.forEntity("LoanApplication", applicationId.toString()));
    }
}
