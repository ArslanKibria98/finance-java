package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.domain.model.PurposeOfFinanceOption;
import com.ksa.financing.customer.domain.port.in.ManagePurposeOfFinanceUseCase;
import com.ksa.financing.customer.domain.port.out.PurposeOfFinanceRepository;
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
public class ManagePurposeOfFinanceService implements ManagePurposeOfFinanceUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManagePurposeOfFinanceService.class);
    private final PurposeOfFinanceRepository repository;

    public ManagePurposeOfFinanceService(PurposeOfFinanceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PurposeOfFinanceOption create(UUID tenantId, CreatePurposeOfFinanceCommand command) {
        if (repository.existsByCode(tenantId, command.code())) {
            throw new BusinessException(
                    ErrorCodes.Customer.DUPLICATE_CODE,
                    "Purpose of finance option with code already exists: " + command.code(),
                    command.code());
        }

        log.info("Creating purpose of finance option: {} for tenant: {}", command.code(), tenantId);

        PurposeOfFinanceOption option = new PurposeOfFinanceOption();
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
    public PurposeOfFinanceOption update(UUID tenantId, UUID id, UpdatePurposeOfFinanceCommand command) {
        PurposeOfFinanceOption option = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Purpose of finance option", id.toString()));

        log.info("Updating purpose of finance option: {} for tenant: {}", id, tenantId);

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
    public PurposeOfFinanceOption getById(UUID tenantId, UUID id) {
        return repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Purpose of finance option", id.toString()));
    }

    @Override
    public List<PurposeOfFinanceOption> getAll(UUID tenantId) {
        return repository.findAllByTenantId(tenantId);
    }

    @Override
    public List<PurposeOfFinanceOption> getActive(UUID tenantId) {
        return repository.findActiveByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deactivate(UUID tenantId, UUID id) {
        PurposeOfFinanceOption option = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Purpose of finance option", id.toString()));

        log.info("Deactivating purpose of finance option: {} for tenant: {}", id, tenantId);
        option.setActive(false);
        option.setUpdatedAt(Instant.now());
        repository.save(option);
    }
}
