package com.ksa.financing.product.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.product.domain.model.TemplateType;
import com.ksa.financing.product.domain.port.in.ManageTemplateTypeUseCase;
import com.ksa.financing.product.domain.port.out.TemplateTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageTemplateTypeUseCaseImpl implements ManageTemplateTypeUseCase {

    private final TemplateTypeRepository templateTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TemplateType> listAll(UUID tenantId) {
        log.debug("Listing all template types for tenant={}", tenantId);
        return templateTypeRepository.findAllByTenant(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateType> listByCategory(UUID tenantId, String category) {
        log.debug("Listing template types for tenant={} category={}", tenantId, category);
        return templateTypeRepository.findByCategory(tenantId, category);
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateType getById(UUID tenantId, UUID id) {
        log.debug("Getting template type id={} for tenant={}", id, tenantId);
        return templateTypeRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("TemplateType", id.toString()));
    }

    @Override
    @Transactional
    public TemplateType create(UUID tenantId, TemplateType type) {
        log.info("Creating template type nameEn={} category={} for tenant={}", type.getNameEn(), type.getCategory(), tenantId);

        templateTypeRepository.findByNameAndCategory(tenantId, type.getNameEn(), type.getCategory()).ifPresent(existing -> {
            throw new BusinessException(ErrorCodes.Product.DUPLICATE_CODE,
                    "Template type with this name already exists in category: " + type.getCategory(), type.getNameEn());
        });

        type.setId(UUID.randomUUID());
        type.setTenantId(tenantId);
        type.setActive(true);
        type.setCreatedAt(Instant.now());
        type.setUpdatedAt(Instant.now());

        return templateTypeRepository.save(type);
    }

    @Override
    @Transactional
    public TemplateType update(UUID tenantId, UUID id, TemplateType updates) {
        log.info("Updating template type id={} for tenant={}", id, tenantId);

        var existing = templateTypeRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("TemplateType", id.toString()));

        existing.setNameEn(updates.getNameEn());
        existing.setNameAr(updates.getNameAr());
        existing.setCategory(updates.getCategory());
        if (updates.isActive() != existing.isActive()) {
            existing.setActive(updates.isActive());
        }
        existing.setUpdatedAt(Instant.now());

        return templateTypeRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID id) {
        log.info("Deleting template type id={} for tenant={}", id, tenantId);
        templateTypeRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("TemplateType", id.toString()));
        templateTypeRepository.delete(id);
    }
}
