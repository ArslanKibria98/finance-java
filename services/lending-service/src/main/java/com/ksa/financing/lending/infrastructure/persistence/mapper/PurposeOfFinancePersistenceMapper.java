package com.ksa.financing.lending.infrastructure.persistence.mapper;

import com.ksa.financing.lending.domain.model.PurposeOfFinanceEntry;
import com.ksa.financing.lending.infrastructure.persistence.entity.PurposeOfFinanceJpaEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PurposeOfFinancePersistenceMapper {

    public PurposeOfFinanceEntry toDomain(PurposeOfFinanceJpaEntity entity) {
        return PurposeOfFinanceEntry.reconstitute(
                entity.getId(),
                entity.getTenantId(),
                entity.getCode(),
                entity.getNameEn(),
                entity.getNameAr(),
                entity.getDescriptionEn(),
                entity.getDescriptionAr(),
                entity.isActive(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getVersion()
        );
    }

    public PurposeOfFinanceJpaEntity toEntity(PurposeOfFinanceEntry entry) {
        var entity = new PurposeOfFinanceJpaEntity();
        entity.setId(entry.getId());
        entity.setTenantId(entry.getTenantId());
        entity.setCode(entry.getCode());
        entity.setNameEn(entry.getNameEn());
        entity.setNameAr(entry.getNameAr());
        entity.setDescriptionEn(entry.getDescriptionEn());
        entity.setDescriptionAr(entry.getDescriptionAr());
        entity.setActive(entry.isActive());
        entity.setSortOrder(entry.getSortOrder());
        entity.setCreatedAt(entry.getCreatedAt() != null ? entry.getCreatedAt() : LocalDateTime.now());
        entity.setUpdatedAt(entry.getUpdatedAt() != null ? entry.getUpdatedAt() : LocalDateTime.now());
        entity.setCreatedBy(entry.getCreatedBy());
        entity.setVersion(entry.getVersion());
        return entity;
    }
}
