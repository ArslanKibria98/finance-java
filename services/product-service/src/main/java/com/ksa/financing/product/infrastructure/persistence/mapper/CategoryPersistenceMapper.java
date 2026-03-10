package com.ksa.financing.product.infrastructure.persistence.mapper;

import com.ksa.financing.product.domain.model.MasterCategory;
import com.ksa.financing.product.domain.model.SubCategory;
import com.ksa.financing.product.infrastructure.persistence.entity.MasterCategoryJpaEntity;
import com.ksa.financing.product.infrastructure.persistence.entity.SubCategoryJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Maps between domain MasterCategory / SubCategory models and their JPA entities.
 * Handles Instant / OffsetDateTime conversion.
 */
@Component
public class CategoryPersistenceMapper {

    // =====================================================================
    // MasterCategory mapping
    // =====================================================================

    /**
     * Converts a JPA entity to a domain MasterCategory.
     */
    public static MasterCategory toDomain(MasterCategoryJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        var domain = new MasterCategory();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setCode(entity.getCode());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setDescriptionEn(entity.getDescriptionEn());
        domain.setDescriptionAr(entity.getDescriptionAr());
        domain.setIconUrl(entity.getIconUrl());
        domain.setSortOrder(entity.getSortOrder());
        domain.setActive(entity.isActive());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));

        return domain;
    }

    /**
     * Converts a domain MasterCategory to a JPA entity.
     */
    public static MasterCategoryJpaEntity toEntity(MasterCategory domain) {
        if (domain == null) {
            return null;
        }

        var entity = new MasterCategoryJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCode(domain.getCode());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setDescriptionEn(domain.getDescriptionEn());
        entity.setDescriptionAr(domain.getDescriptionAr());
        entity.setIconUrl(domain.getIconUrl());
        entity.setSortOrder(domain.getSortOrder());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));

        return entity;
    }

    // =====================================================================
    // SubCategory mapping
    // =====================================================================

    /**
     * Converts a JPA entity to a domain SubCategory.
     */
    public static SubCategory toDomain(SubCategoryJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        var domain = new SubCategory();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setMasterCategoryId(entity.getMasterCategoryId());
        domain.setCode(entity.getCode());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setSortOrder(entity.getSortOrder());
        domain.setActive(entity.isActive());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));

        return domain;
    }

    /**
     * Converts a domain SubCategory to a JPA entity.
     */
    public static SubCategoryJpaEntity toEntity(SubCategory domain) {
        if (domain == null) {
            return null;
        }

        var entity = new SubCategoryJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setMasterCategoryId(domain.getMasterCategoryId());
        entity.setCode(domain.getCode());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setSortOrder(domain.getSortOrder());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));

        return entity;
    }

    // =====================================================================
    // Timestamp conversion helpers
    // =====================================================================

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

    private static Instant toInstant(OffsetDateTime odt) {
        return odt != null ? odt.toInstant() : null;
    }
}
