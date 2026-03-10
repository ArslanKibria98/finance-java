package com.ksa.financing.service.template.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ksa.financing.service.template.domain.model.ExampleAggregate;
import com.ksa.financing.service.template.domain.port.in.ManageExampleUseCase;
import com.ksa.financing.service.template.domain.port.out.ExampleRepository;
import com.ksa.financing.service.template.domain.port.out.EventPublisher;
import com.ksa.financing.service.template.domain.service.ExampleDomainService;

/**
 * Application Service: Implementation of the ManageExampleUseCase.
 * This orchestrates domain operations and coordinates with infrastructure.
 *
 * Key responsibilities:
 * - Transaction management
 * - Orchestration of domain services
 * - Coordination with infrastructure services
 * - Event publishing
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ManageExampleUseCaseImpl implements ManageExampleUseCase {

    private final ExampleRepository repository;
    private final EventPublisher eventPublisher;
    private final ExampleDomainService domainService;

    @Override
    public ExampleAggregate createExample(CreateExampleCommand command) {
        log.info("Creating example aggregate for tenant: {}", command.tenantId());

        // Create aggregate using domain factory method
        var aggregate = ExampleAggregate.create(
                command.tenantId(),
                command.name(),
                command.description(),
                command.createdBy()
        );

        // Persist
        aggregate = repository.save(aggregate);

        // Publish domain events
        eventPublisher.publishAll(aggregate.getUncommittedEvents());
        aggregate.markEventsAsCommitted();

        log.info("Created example aggregate: {}", aggregate.getId());
        return aggregate;
    }

    @Override
    public void activateExample(ActivateExampleCommand command) {
        log.info("Activating example aggregate: {}", command.aggregateId());

        // Load aggregate
        var aggregate = repository.findById(command.tenantId(), command.aggregateId())
                .orElseThrow(() -> NotFoundException.forEntity("ExampleAggregate", command.aggregateId().toString()));

        // Check business rules via domain service
        if (!domainService.canActivate(aggregate)) {
            throw new BusinessException(
                    ErrorCodes.CONFLICT,
                    "Cannot activate aggregate: " + command.aggregateId());
        }

        // Perform domain operation
        aggregate.activate(command.activatedBy());

        // Persist changes
        repository.save(aggregate);

        // Publish events
        eventPublisher.publishAll(aggregate.getUncommittedEvents());
        aggregate.markEventsAsCommitted();

        log.info("Activated example aggregate: {}", command.aggregateId());
    }

    @Override
    public void addEntity(AddEntityCommand command) {
        log.info("Adding entity to aggregate: {}", command.aggregateId());

        // Load aggregate
        var aggregate = repository.findById(command.tenantId(), command.aggregateId())
                .orElseThrow(() -> NotFoundException.forEntity("ExampleAggregate", command.aggregateId().toString()));

        // Perform domain operation
        aggregate.addEntity(
                command.entityName(),
                command.entityValue(),
                command.addedBy()
        );

        // Persist
        repository.save(aggregate);

        // Publish events
        eventPublisher.publishAll(aggregate.getUncommittedEvents());
        aggregate.markEventsAsCommitted();

        log.info("Added entity to aggregate: {}", command.aggregateId());
    }

    @Override
    public void completeExample(CompleteExampleCommand command) {
        log.info("Completing example aggregate: {}", command.aggregateId());

        // Load aggregate
        var aggregate = repository.findById(command.tenantId(), command.aggregateId())
                .orElseThrow(() -> NotFoundException.forEntity("ExampleAggregate", command.aggregateId().toString()));

        // Calculate completion score before completing
        double score = domainService.calculateCompletionScore(aggregate);
        log.debug("Completion score: {}", score);

        if (score < 70.0) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_FAILED,
                    "Aggregate not ready for completion. Score: " + score);
        }

        // Perform domain operation
        aggregate.complete(command.completedBy());

        // Persist
        repository.save(aggregate);

        // Publish events
        eventPublisher.publishAll(aggregate.getUncommittedEvents());
        aggregate.markEventsAsCommitted();

        log.info("Completed example aggregate: {}", command.aggregateId());
    }

    @Override
    @Transactional(readOnly = true)
    public ExampleAggregate getExample(GetExampleQuery query) {
        log.debug("Getting example aggregate: {}", query.aggregateId());

        return repository.findById(query.tenantId(), query.aggregateId())
                .orElseThrow(() -> NotFoundException.forEntity("ExampleAggregate", query.aggregateId().toString()));
    }

}