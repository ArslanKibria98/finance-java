package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.domain.model.SourceOfIncomeOption;
import com.ksa.financing.customer.domain.port.in.ManageSourceOfIncomeUseCase;
import com.ksa.financing.customer.domain.port.out.SourceOfIncomeRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ManageSourceOfIncomeService implements ManageSourceOfIncomeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageSourceOfIncomeService.class);
    private final SourceOfIncomeRepository repository;

    public ManageSourceOfIncomeService(SourceOfIncomeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SourceOfIncomeOption create(UUID tenantId, CreateSourceOfIncomeCommand command) {
        if (repository.existsByCode(tenantId, command.code())) {
            throw new BusinessException(
                    ErrorCodes.Customer.DUPLICATE_CODE,
                    "Source of income option with code already exists: " + command.code(),
                    command.code());
        }

        log.info("Creating source of income option: {} for tenant: {}", command.code(), tenantId);

        SourceOfIncomeOption option = new SourceOfIncomeOption();
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
    public SourceOfIncomeOption update(UUID tenantId, UUID id, UpdateSourceOfIncomeCommand command) {
        SourceOfIncomeOption option = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Source of income option", id.toString()));

        log.info("Updating source of income option: {} for tenant: {}", id, tenantId);

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
    public SourceOfIncomeOption getById(UUID tenantId, UUID id) {
        return repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Source of income option", id.toString()));
    }

    @Override
    public List<SourceOfIncomeOption> getAll(UUID tenantId) {
        return repository.findAllByTenantId(tenantId);
    }

    @Override
    public List<SourceOfIncomeOption> getActive(UUID tenantId) {
        return repository.findActiveByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deactivate(UUID tenantId, UUID id) {
        SourceOfIncomeOption option = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Source of income option", id.toString()));

        log.info("Deactivating source of income option: {} for tenant: {}", id, tenantId);
        option.setActive(false);
        option.setUpdatedAt(Instant.now());
        repository.save(option);
    }

    @Override
    @Transactional
    public void activate(UUID tenantId, UUID id) {
        SourceOfIncomeOption option = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("option", id.toString()));
        option.setActive(true);
        option.setUpdatedAt(java.time.Instant.now());
        repository.save(option);
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID id) {
        repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("option", id.toString()));
        repository.softDelete(tenantId, id);
    }
}
