package com.ksa.financing.product.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.product.domain.model.Product;
import com.ksa.financing.product.domain.model.ProductStatus;
import com.ksa.financing.product.domain.port.in.ManageProductUseCase;
import com.ksa.financing.product.domain.port.out.EventPublisherPort;
import com.ksa.financing.product.domain.port.out.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageProductUseCaseImpl implements ManageProductUseCase {

    private final ProductRepository productRepository;
    private final EventPublisherPort eventPublisher;

    @Override
    @Transactional
    public Product create(CreateProductCommand command) {
        log.info("Creating product with code: {} for tenant: {}", command.productCode(), command.tenantId());

        if (command.productCode() != null && !command.productCode().isBlank()
                && productRepository.existsByProductCode(command.tenantId(), command.productCode())) {
            throw new BusinessException(
                ErrorCodes.Product.DUPLICATE_CODE,
                "Product with code already exists: " + command.productCode(),
                command.productCode());
        }

        var product = Product.create(
            command.tenantId(),
            command.productCode(),
            command.nameEn(),
            command.productType(),
            command.shariaStructure(),
            command.minAmount(),
            command.maxAmount(),
            command.minTenureMonths(),
            command.maxTenureMonths(),
            command.baseProfitRate()
        );

        // Set additional fields from command
        product.setNameAr(command.nameAr());
        product.setDescriptionEn(command.descriptionEn());
        product.setDescriptionAr(command.descriptionAr());
        product.setShortDescriptionEn(command.shortDescriptionEn());
        product.setShortDescriptionAr(command.shortDescriptionAr());
        product.setTargetSegment(command.targetSegment() != null ? command.targetSegment() : "INDIVIDUAL");
        product.setMasterCategoryId(command.masterCategoryId());
        product.setSubCategoryId(command.subCategoryId());
        product.setTemplateId(command.templateId());
        product.setNotificationEmail(command.notificationEmail());
        product.setCustomerTypes(command.customerTypes());
        product.setInvolvesCommodity(command.involvesCommodity());
        product.setSetupMethod(command.setupMethod());
        product.setAllowedTenures(command.allowedTenures());
        product.setRateType(command.rateType() != null ? command.rateType() : "REDUCING_BALANCE");
        product.setRepaymentFrequency(command.repaymentFrequency() != null ? command.repaymentFrequency() : "MONTHLY");
        product.setGracePeriodDays(command.gracePeriodDays() > 0 ? command.gracePeriodDays() : 3);
        product.setEarlySettlementAllowed(command.earlySettlementAllowed());
        product.setVisibleToPartners(true);
        product.setCreatedBy(command.createdBy());

        Product saved = productRepository.save(product);
        log.info("Product created with id: {}", saved.getId());

        eventPublisher.publishProductCreated(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Product getById(UUID tenantId, UUID productId) {
        return productRepository.findById(tenantId, productId)
            .orElseThrow(() -> NotFoundException.forEntity("Product", productId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> listByTenant(UUID tenantId) {
        return productRepository.findAllByTenant(tenantId);
    }

    @Override
    @Transactional
    public Product updateBasicInfo(UUID tenantId, UUID productId, UpdateBasicInfoCommand command) {
        log.info("Updating basic info for product: {}", productId);

        var product = productRepository.findById(tenantId, productId)
            .orElseThrow(() -> NotFoundException.forEntity("Product", productId.toString()));

        if (command.nameEn() != null) product.setNameEn(command.nameEn());
        if (command.nameAr() != null) product.setNameAr(command.nameAr());
        if (command.descriptionEn() != null) product.setDescriptionEn(command.descriptionEn());
        if (command.descriptionAr() != null) product.setDescriptionAr(command.descriptionAr());
        if (command.shortDescriptionEn() != null) product.setShortDescriptionEn(command.shortDescriptionEn());
        if (command.shortDescriptionAr() != null) product.setShortDescriptionAr(command.shortDescriptionAr());
        if (command.notificationEmail() != null) product.setNotificationEmail(command.notificationEmail());
        if (command.customerTypes() != null) product.setCustomerTypes(command.customerTypes());
        product.setInvolvesCommodity(command.involvesCommodity());
        if (command.logoUrl() != null) product.setLogoUrl(command.logoUrl());
        product.setUpdatedBy(command.updatedBy());

        Product saved = productRepository.save(product);
        eventPublisher.publishProductUpdated(saved);
        return saved;
    }

    @Override
    @Transactional
    public void activate(UUID tenantId, UUID productId) {
        log.info("Activating product: {}", productId);

        var product = productRepository.findById(tenantId, productId)
            .orElseThrow(() -> NotFoundException.forEntity("Product", productId.toString()));

        try {
            product.activate();
        } catch (IllegalStateException e) {
            throw new BusinessException(
                ErrorCodes.Product.INVALID_TRANSITION,
                e.getMessage(),
                product.getStatus().name(), ProductStatus.ACTIVE.name());
        }

        productRepository.save(product);
        eventPublisher.publishProductActivated(product);
    }

    @Override
    @Transactional
    public void deactivate(UUID tenantId, UUID productId) {
        log.info("Deactivating product: {}", productId);

        var product = productRepository.findById(tenantId, productId)
            .orElseThrow(() -> NotFoundException.forEntity("Product", productId.toString()));

        try {
            product.deactivate();
        } catch (IllegalStateException e) {
            throw new BusinessException(
                ErrorCodes.Product.INVALID_TRANSITION,
                e.getMessage(),
                product.getStatus().name(), ProductStatus.INACTIVE.name());
        }

        productRepository.save(product);
    }

    @Override
    @Transactional
    public void softDelete(UUID tenantId, UUID productId) {
        log.info("Soft-deleting product: {}", productId);

        var product = productRepository.findById(tenantId, productId)
            .orElseThrow(() -> NotFoundException.forEntity("Product", productId.toString()));

        product.setDeletedAt(java.time.Instant.now());
        productRepository.save(product);
    }
}
