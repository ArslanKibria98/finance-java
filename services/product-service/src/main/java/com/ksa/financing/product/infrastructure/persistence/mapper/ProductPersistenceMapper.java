package com.ksa.financing.product.infrastructure.persistence.mapper;

import com.ksa.financing.product.domain.model.Product;
import com.ksa.financing.product.domain.model.ProductStatus;
import com.ksa.financing.product.domain.model.ProductType;
import com.ksa.financing.product.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Maps between the {@link Product} domain model and {@link ProductJpaEntity} JPA entity.
 * Handles enum conversion (ProductStatus) and Instant / OffsetDateTime conversion.
 */
@Component
public class ProductPersistenceMapper {

    /**
     * Converts a domain Product to a JPA entity for persistence.
     */
    public static ProductJpaEntity toEntity(Product domain) {
        if (domain == null) {
            return null;
        }

        var entity = new ProductJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());

        // Identification
        entity.setProductCode(domain.getProductCode());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setDescriptionEn(domain.getDescriptionEn());
        entity.setDescriptionAr(domain.getDescriptionAr());
        entity.setShortDescriptionEn(domain.getShortDescriptionEn());
        entity.setShortDescriptionAr(domain.getShortDescriptionAr());
        entity.setLogoUrl(domain.getLogoUrl());

        // Classification
        entity.setProductType(domain.getProductType() != null ? domain.getProductType().name() : null);
        entity.setTargetSegment(domain.getTargetSegment());
        entity.setMasterCategoryId(domain.getMasterCategoryId());
        entity.setSubCategoryId(domain.getSubCategoryId());
        entity.setTemplateId(domain.getTemplateId());

        // UI fields
        entity.setNotificationEmail(domain.getNotificationEmail());
        entity.setCustomerTypes(domain.getCustomerTypes() != null
                ? domain.getCustomerTypes().toArray(new String[0]) : null);
        entity.setInvolvesCommodity(domain.isInvolvesCommodity());
        entity.setSetupMethod(domain.getSetupMethod());

        // Wizard progress
        entity.setWizardStep(domain.getWizardStep());
        entity.setWizardCompleted(domain.isWizardCompleted());

        // Sharia configuration
        entity.setShariaStructure(domain.getShariaStructure());
        entity.setCommodityRequired(domain.isCommodityRequired());

        // Availability
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        entity.setStartDate(domain.getStartDate());
        entity.setEndDate(domain.getEndDate());
        entity.setVisibleToCustomers(domain.isVisibleToCustomers());
        entity.setVisibleToPartners(domain.isVisibleToPartners());

        // Financial terms
        entity.setMinAmount(domain.getMinAmount());
        entity.setMaxAmount(domain.getMaxAmount());
        entity.setMinTenureMonths(domain.getMinTenureMonths());
        entity.setMaxTenureMonths(domain.getMaxTenureMonths());
        entity.setAllowedTenures(domain.getAllowedTenures() != null
                ? domain.getAllowedTenures().toArray(new Integer[0]) : null);

        // Profit rate
        entity.setBaseProfitRate(domain.getBaseProfitRate());
        entity.setRateType(domain.getRateType());

        // Repayment
        entity.setRepaymentFrequency(domain.getRepaymentFrequency());
        entity.setGracePeriodDays(domain.getGracePeriodDays());

        // Early settlement
        entity.setEarlySettlementAllowed(domain.isEarlySettlementAllowed());
        entity.setWaiveUnearnedProfit(domain.isWaiveUnearnedProfit());
        entity.setMinTenureBeforeSettlement(domain.getMinTenureBeforeSettlement());

        // Core banking
        entity.setFineractProductId(domain.getFineractProductId());

        // Regional
        entity.setCurrency(domain.getCurrency());
        entity.setCountryId(domain.getCountryId());

        // Audit
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setUpdatedBy(domain.getUpdatedBy());
        entity.setVersionNumber(domain.getVersionNumber());
        entity.setVersion(domain.getVersion());
        entity.setDeletedAt(toOffsetDateTime(domain.getDeletedAt()));

        return entity;
    }

    /**
     * Converts a JPA entity to a domain Product model.
     */
    public static Product toDomain(ProductJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        var domain = new Product();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());

        // Identification
        domain.setProductCode(entity.getProductCode());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setDescriptionEn(entity.getDescriptionEn());
        domain.setDescriptionAr(entity.getDescriptionAr());
        domain.setShortDescriptionEn(entity.getShortDescriptionEn());
        domain.setShortDescriptionAr(entity.getShortDescriptionAr());
        domain.setLogoUrl(entity.getLogoUrl());

        // Classification
        domain.setProductType(entity.getProductType() != null
                ? ProductType.valueOf(entity.getProductType())
                : null);
        domain.setTargetSegment(entity.getTargetSegment());
        domain.setMasterCategoryId(entity.getMasterCategoryId());
        domain.setSubCategoryId(entity.getSubCategoryId());
        domain.setTemplateId(entity.getTemplateId());

        // UI fields
        domain.setNotificationEmail(entity.getNotificationEmail());
        domain.setCustomerTypes(entity.getCustomerTypes() != null
                ? Arrays.asList(entity.getCustomerTypes()) : Collections.emptyList());
        domain.setInvolvesCommodity(entity.isInvolvesCommodity());
        domain.setSetupMethod(entity.getSetupMethod());

        // Wizard progress
        domain.setWizardStep(entity.getWizardStep());
        domain.setWizardCompleted(entity.isWizardCompleted());

        // Sharia configuration
        domain.setShariaStructure(entity.getShariaStructure());
        domain.setCommodityRequired(entity.isCommodityRequired());

        // Availability — convert string to ProductStatus enum
        domain.setStatus(entity.getStatus() != null
                ? ProductStatus.valueOf(entity.getStatus())
                : null);
        domain.setStartDate(entity.getStartDate());
        domain.setEndDate(entity.getEndDate());
        domain.setVisibleToCustomers(entity.isVisibleToCustomers());
        domain.setVisibleToPartners(entity.isVisibleToPartners());

        // Financial terms
        domain.setMinAmount(entity.getMinAmount());
        domain.setMaxAmount(entity.getMaxAmount());
        domain.setMinTenureMonths(entity.getMinTenureMonths());
        domain.setMaxTenureMonths(entity.getMaxTenureMonths());
        domain.setAllowedTenures(entity.getAllowedTenures() != null
                ? Arrays.asList(entity.getAllowedTenures()) : Collections.emptyList());

        // Profit rate
        domain.setBaseProfitRate(entity.getBaseProfitRate());
        domain.setRateType(entity.getRateType());

        // Repayment
        domain.setRepaymentFrequency(entity.getRepaymentFrequency());
        domain.setGracePeriodDays(entity.getGracePeriodDays());

        // Early settlement
        domain.setEarlySettlementAllowed(entity.isEarlySettlementAllowed());
        domain.setWaiveUnearnedProfit(entity.isWaiveUnearnedProfit());
        domain.setMinTenureBeforeSettlement(entity.getMinTenureBeforeSettlement());

        // Core banking
        domain.setFineractProductId(entity.getFineractProductId());

        // Regional
        domain.setCurrency(entity.getCurrency());
        domain.setCountryId(entity.getCountryId());

        // Audit
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setCreatedBy(entity.getCreatedBy());
        domain.setUpdatedBy(entity.getUpdatedBy());
        domain.setVersionNumber(entity.getVersionNumber());
        domain.setVersion(entity.getVersion());
        domain.setDeletedAt(toInstant(entity.getDeletedAt()));

        return domain;
    }

    // --- Timestamp conversion helpers ---

    static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

    static Instant toInstant(OffsetDateTime odt) {
        return odt != null ? odt.toInstant() : null;
    }

}
