package com.ksa.financing.product.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.product.domain.model.ContractTemplate;
import com.ksa.financing.product.domain.port.in.ManageContractTemplateUseCase;
import com.ksa.financing.product.domain.port.out.ContractTemplateRepository;
import com.ksa.financing.product.domain.port.out.ProductRepository;
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
public class ManageContractTemplateUseCaseImpl implements ManageContractTemplateUseCase {

    private final ContractTemplateRepository contractTemplateRepository;
    private final ProductRepository productRepository;
    private final TemplateTypeRepository templateTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ContractTemplate> listAll(UUID tenantId) {
        log.debug("Listing all contract templates for tenant={}", tenantId);
        return contractTemplateRepository.findAllByTenant(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTemplate> listByProduct(UUID tenantId, UUID productId) {
        log.debug("Listing contract templates for tenant={} product={}", tenantId, productId);
        return contractTemplateRepository.findByProduct(tenantId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTemplate> listByType(UUID tenantId, UUID typeId) {
        log.debug("Listing contract templates for tenant={} type={}", tenantId, typeId);
        return contractTemplateRepository.findByType(tenantId, typeId);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractTemplate getById(UUID tenantId, UUID id) {
        log.debug("Getting contract template id={} for tenant={}", id, tenantId);
        return contractTemplateRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("ContractTemplate", id.toString()));
    }

    @Override
    @Transactional
    public ContractTemplate create(UUID tenantId, ContractTemplate template) {
        log.info("Creating contract template nameEn={} for tenant={}", template.getNameEn(), tenantId);

        // Validate product exists
        productRepository.findById(tenantId, template.getProductId())
                .orElseThrow(() -> NotFoundException.forEntity("Product", template.getProductId().toString()));

        // Validate type exists
        templateTypeRepository.findById(template.getTypeId())
                .orElseThrow(() -> NotFoundException.forEntity("TemplateType", template.getTypeId().toString()));

        template.setId(UUID.randomUUID());
        template.setTenantId(tenantId);
        template.setActive(true);
        template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());

        return contractTemplateRepository.save(template);
    }

    @Override
    @Transactional
    public ContractTemplate update(UUID tenantId, UUID id, ContractTemplate updates) {
        log.info("Updating contract template id={} for tenant={}", id, tenantId);

        var existing = contractTemplateRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("ContractTemplate", id.toString()));

        // Validate product if changed
        if (updates.getProductId() != null) {
            productRepository.findById(tenantId, updates.getProductId())
                    .orElseThrow(() -> NotFoundException.forEntity("Product", updates.getProductId().toString()));
            existing.setProductId(updates.getProductId());
        }

        // Validate type if changed
        if (updates.getTypeId() != null) {
            templateTypeRepository.findById(updates.getTypeId())
                    .orElseThrow(() -> NotFoundException.forEntity("TemplateType", updates.getTypeId().toString()));
            existing.setTypeId(updates.getTypeId());
        }

        existing.setNameEn(updates.getNameEn());
        existing.setNameAr(updates.getNameAr());
        existing.setLanguage(updates.getLanguage());
        existing.setMessage(updates.getMessage());
        existing.setUpdatedAt(Instant.now());

        return contractTemplateRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID id) {
        log.info("Deleting contract template id={} for tenant={}", id, tenantId);
        contractTemplateRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("ContractTemplate", id.toString()));
        contractTemplateRepository.delete(id);
    }
}
