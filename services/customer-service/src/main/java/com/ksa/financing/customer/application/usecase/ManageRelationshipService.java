package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.domain.model.RelationshipOption;
import com.ksa.financing.customer.domain.port.in.ManageRelationshipUseCase;
import com.ksa.financing.customer.domain.port.out.RelationshipRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ManageRelationshipService implements ManageRelationshipUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageRelationshipService.class);
    private final RelationshipRepository repository;

    public ManageRelationshipService(RelationshipRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public RelationshipOption create(UUID tenantId, CreateRelationshipCommand command) {
        if (repository.existsByCode(tenantId, command.code())) {
            throw new BusinessException(
                    ErrorCodes.Customer.DUPLICATE_CODE,
                    "Relationship option with code already exists: " + command.code(),
                    command.code());
        }

        log.info("Creating relationship option: {} for tenant: {}", command.code(), tenantId);

        RelationshipOption option = new RelationshipOption();
        option.setTenantId(tenantId);
        option.setCode(command.code());
        option.setNameEn(command.nameEn());
        option.setNameAr(command.nameAr());
        option.setDescriptionEn(command.descriptionEn());
        option.setDescriptionAr(command.descriptionAr());
        option.setActive(true);
        option.setDisplayOrder(command.displayOrder());
        Instant now = Instant.now();
        option.setCreatedAt(now);
        option.setUpdatedAt(now);

        return repository.save(option);
    }

    @Override
    @Transactional
    public RelationshipOption update(UUID tenantId, UUID id, UpdateRelationshipCommand command) {
        RelationshipOption option = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Relationship option", id.toString()));

        log.info("Updating relationship option: {} for tenant: {}", id, tenantId);

        if (command.nameEn() != null) option.setNameEn(command.nameEn());
        if (command.nameAr() != null) option.setNameAr(command.nameAr());
        if (command.descriptionEn() != null) option.setDescriptionEn(command.descriptionEn());
        if (command.descriptionAr() != null) option.setDescriptionAr(command.descriptionAr());
        if (command.isActive() != null) option.setActive(command.isActive());
        if (command.displayOrder() != null) option.setDisplayOrder(command.displayOrder());
        option.setUpdatedAt(Instant.now());

        return repository.save(option);
    }

    @Override
    public RelationshipOption getById(UUID tenantId, UUID id) {
        return repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Relationship option", id.toString()));
    }

    @Override
    public PageResponse<RelationshipOption> getAll(UUID tenantId, PageQuery pageQuery) {
        return repository.findAllByTenantId(tenantId, pageQuery);
    }

    @Override
    public PageResponse<RelationshipOption> getActive(UUID tenantId, PageQuery pageQuery) {
        return repository.findActiveByTenantId(tenantId, pageQuery);
    }

    @Override
    @Transactional
    public void deactivate(UUID tenantId, UUID id) {
        RelationshipOption option = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Relationship option", id.toString()));

        log.info("Deactivating relationship option: {} for tenant: {}", id, tenantId);
        option.setActive(false);
        option.setUpdatedAt(Instant.now());
        repository.save(option);
    }

    @Override
    @Transactional
    public void activate(UUID tenantId, UUID id) {
        RelationshipOption option = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Relationship option", id.toString()));
        option.setActive(true);
        option.setUpdatedAt(Instant.now());
        repository.save(option);
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID id) {
        repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Relationship option", id.toString()));
        repository.softDelete(tenantId, id);
    }
}
