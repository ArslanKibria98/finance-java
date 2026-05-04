package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.domain.model.NetWorthRangeOption;
import com.ksa.financing.customer.domain.port.in.ManageNetWorthRangeUseCase;
import com.ksa.financing.customer.domain.port.out.NetWorthRangeRepository;
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
public class ManageNetWorthRangeService implements ManageNetWorthRangeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageNetWorthRangeService.class);
    private final NetWorthRangeRepository repository;

    public ManageNetWorthRangeService(NetWorthRangeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public NetWorthRangeOption create(UUID tenantId, CreateNetWorthRangeCommand command) {
        if (repository.existsByCode(tenantId, command.code())) {
            throw new BusinessException(
                    ErrorCodes.Customer.DUPLICATE_CODE,
                    "Net worth range option with code already exists: " + command.code(),
                    command.code());
        }

        log.info("Creating net worth range option: {} for tenant: {}", command.code(), tenantId);

        NetWorthRangeOption option = new NetWorthRangeOption();
        option.setTenantId(tenantId);
        option.setCode(command.code());
        option.setNameEn(command.nameEn());
        option.setNameAr(command.nameAr());
        option.setDescriptionEn(command.descriptionEn());
        option.setDescriptionAr(command.descriptionAr());
        option.setMinValue(command.minValue());
        option.setMaxValue(command.maxValue());
        option.setActive(true);
        option.setDisplayOrder(command.displayOrder());
        Instant now = Instant.now();
        option.setCreatedAt(now);
        option.setUpdatedAt(now);

        return repository.save(option);
    }

    @Override
    @Transactional
    public NetWorthRangeOption update(UUID tenantId, UUID id, UpdateNetWorthRangeCommand command) {
        NetWorthRangeOption option = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Net worth range option", id.toString()));

        log.info("Updating net worth range option: {} for tenant: {}", id, tenantId);

        if (command.nameEn() != null) option.setNameEn(command.nameEn());
        if (command.nameAr() != null) option.setNameAr(command.nameAr());
        if (command.descriptionEn() != null) option.setDescriptionEn(command.descriptionEn());
        if (command.descriptionAr() != null) option.setDescriptionAr(command.descriptionAr());
        if (command.minValue() != null) option.setMinValue(command.minValue());
        if (command.maxValue() != null) option.setMaxValue(command.maxValue());
        if (command.isActive() != null) option.setActive(command.isActive());
        if (command.displayOrder() != null) option.setDisplayOrder(command.displayOrder());
        option.setUpdatedAt(Instant.now());

        return repository.save(option);
    }

    @Override
    public NetWorthRangeOption getById(UUID tenantId, UUID id) {
        return repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Net worth range option", id.toString()));
    }

    @Override
    public PageResponse<NetWorthRangeOption> getAll(UUID tenantId, PageQuery pageQuery) {
        return repository.findAllByTenantId(tenantId, pageQuery);
    }

    @Override
    public PageResponse<NetWorthRangeOption> getActive(UUID tenantId, PageQuery pageQuery) {
        return repository.findActiveByTenantId(tenantId, pageQuery);
    }

    @Override
    @Transactional
    public void deactivate(UUID tenantId, UUID id) {
        NetWorthRangeOption option = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Net worth range option", id.toString()));

        log.info("Deactivating net worth range option: {} for tenant: {}", id, tenantId);
        option.setActive(false);
        option.setUpdatedAt(Instant.now());
        repository.save(option);
    }

    @Override
    @Transactional
    public void activate(UUID tenantId, UUID id) {
        NetWorthRangeOption option = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Net worth range option", id.toString()));
        option.setActive(true);
        option.setUpdatedAt(Instant.now());
        repository.save(option);
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID id) {
        repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Net worth range option", id.toString()));
        repository.softDelete(tenantId, id);
    }
}
