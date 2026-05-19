package com.ksa.financing.customer.infrastructure.persistence.mapper;

import com.ksa.financing.customer.domain.model.NetWorthRangeOption;
import com.ksa.financing.customer.domain.model.OccupationOption;
import com.ksa.financing.customer.domain.model.PurposeOfFinanceOption;
import com.ksa.financing.customer.domain.model.SourceOfFundsOption;
import com.ksa.financing.customer.domain.model.SourceOfIncomeOption;
import com.ksa.financing.customer.domain.model.SourceOfWealthOption;
import com.ksa.financing.customer.infrastructure.persistence.entity.NetWorthRangeOptionJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.entity.OccupationOptionJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.entity.PurposeOfFinanceOptionJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.entity.SourceOfFundsOptionJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.entity.SourceOfIncomeOptionJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.entity.SourceOfWealthOptionJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class ReferenceDataPersistenceMapper {

    // ---- SourceOfWealth mapping ----

    public static SourceOfWealthOptionJpaEntity toSowEntity(SourceOfWealthOption domain) {
        if (domain == null) return null;

        SourceOfWealthOptionJpaEntity entity = new SourceOfWealthOptionJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCode(domain.getCode());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setDescriptionEn(domain.getDescriptionEn());
        entity.setDescriptionAr(domain.getDescriptionAr());
        entity.setActive(domain.isActive());
        entity.setDeleted(domain.isDeleted());
        entity.setDisplayOrder(domain.getDisplayOrder());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public static SourceOfWealthOption toDomain(SourceOfWealthOptionJpaEntity entity) {
        if (entity == null) return null;

        SourceOfWealthOption domain = new SourceOfWealthOption();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setCode(entity.getCode());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setDescriptionEn(entity.getDescriptionEn());
        domain.setDescriptionAr(entity.getDescriptionAr());
        domain.setActive(entity.isActive());
        domain.setDeleted(entity.isDeleted());
        domain.setDisplayOrder(entity.getDisplayOrder());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // ---- SourceOfFunds mapping ----

    public static SourceOfFundsOptionJpaEntity toSofEntity(SourceOfFundsOption domain) {
        if (domain == null) return null;

        SourceOfFundsOptionJpaEntity entity = new SourceOfFundsOptionJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCode(domain.getCode());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setDescriptionEn(domain.getDescriptionEn());
        entity.setDescriptionAr(domain.getDescriptionAr());
        entity.setActive(domain.isActive());
        entity.setDeleted(domain.isDeleted());
        entity.setDisplayOrder(domain.getDisplayOrder());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public static SourceOfFundsOption toDomain(SourceOfFundsOptionJpaEntity entity) {
        if (entity == null) return null;

        SourceOfFundsOption domain = new SourceOfFundsOption();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setCode(entity.getCode());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setDescriptionEn(entity.getDescriptionEn());
        domain.setDescriptionAr(entity.getDescriptionAr());
        domain.setActive(entity.isActive());
        domain.setDeleted(entity.isDeleted());
        domain.setDisplayOrder(entity.getDisplayOrder());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // ---- NetWorthRange mapping ----

    public static NetWorthRangeOptionJpaEntity toNwrEntity(NetWorthRangeOption domain) {
        if (domain == null) return null;

        NetWorthRangeOptionJpaEntity entity = new NetWorthRangeOptionJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCode(domain.getCode());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setDescriptionEn(domain.getDescriptionEn());
        entity.setDescriptionAr(domain.getDescriptionAr());
        entity.setMinValue(domain.getMinValue());
        entity.setMaxValue(domain.getMaxValue());
        entity.setActive(domain.isActive());
        entity.setDeleted(domain.isDeleted());
        entity.setDisplayOrder(domain.getDisplayOrder());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public static NetWorthRangeOption toDomain(NetWorthRangeOptionJpaEntity entity) {
        if (entity == null) return null;

        NetWorthRangeOption domain = new NetWorthRangeOption();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setCode(entity.getCode());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setDescriptionEn(entity.getDescriptionEn());
        domain.setDescriptionAr(entity.getDescriptionAr());
        domain.setMinValue(entity.getMinValue());
        domain.setMaxValue(entity.getMaxValue());
        domain.setActive(entity.isActive());
        domain.setDeleted(entity.isDeleted());
        domain.setDisplayOrder(entity.getDisplayOrder());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // ---- SourceOfIncome mapping ----

    public static SourceOfIncomeOptionJpaEntity toSoiEntity(SourceOfIncomeOption domain) {
        if (domain == null) return null;

        SourceOfIncomeOptionJpaEntity entity = new SourceOfIncomeOptionJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCode(domain.getCode());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setDescriptionEn(domain.getDescriptionEn());
        entity.setDescriptionAr(domain.getDescriptionAr());
        entity.setActive(domain.isActive());
        entity.setDeleted(domain.isDeleted());
        entity.setDisplayOrder(domain.getDisplayOrder());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public static SourceOfIncomeOption toDomain(SourceOfIncomeOptionJpaEntity entity) {
        if (entity == null) return null;

        SourceOfIncomeOption domain = new SourceOfIncomeOption();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setCode(entity.getCode());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setDescriptionEn(entity.getDescriptionEn());
        domain.setDescriptionAr(entity.getDescriptionAr());
        domain.setActive(entity.isActive());
        domain.setDeleted(entity.isDeleted());
        domain.setDisplayOrder(entity.getDisplayOrder());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // ---- Occupation mapping ----

    public static OccupationOptionJpaEntity toOccupationEntity(OccupationOption domain) {
        if (domain == null) return null;

        OccupationOptionJpaEntity entity = new OccupationOptionJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCode(domain.getCode());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setDescriptionEn(domain.getDescriptionEn());
        entity.setDescriptionAr(domain.getDescriptionAr());
        entity.setActive(domain.isActive());
        entity.setDeleted(domain.isDeleted());
        entity.setDisplayOrder(domain.getDisplayOrder());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public static OccupationOption toDomain(OccupationOptionJpaEntity entity) {
        if (entity == null) return null;

        OccupationOption domain = new OccupationOption();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setCode(entity.getCode());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setDescriptionEn(entity.getDescriptionEn());
        domain.setDescriptionAr(entity.getDescriptionAr());
        domain.setActive(entity.isActive());
        domain.setDeleted(entity.isDeleted());
        domain.setDisplayOrder(entity.getDisplayOrder());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // ---- PurposeOfFinance mapping ----

    public static PurposeOfFinanceOptionJpaEntity toPofEntity(PurposeOfFinanceOption domain) {
        if (domain == null) return null;

        PurposeOfFinanceOptionJpaEntity entity = new PurposeOfFinanceOptionJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCode(domain.getCode());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setDescriptionEn(domain.getDescriptionEn());
        entity.setDescriptionAr(domain.getDescriptionAr());
        entity.setActive(domain.isActive());
        entity.setDeleted(domain.isDeleted());
        entity.setDisplayOrder(domain.getDisplayOrder());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public static PurposeOfFinanceOption toDomain(PurposeOfFinanceOptionJpaEntity entity) {
        if (entity == null) return null;

        PurposeOfFinanceOption domain = new PurposeOfFinanceOption();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setCode(entity.getCode());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setDescriptionEn(entity.getDescriptionEn());
        domain.setDescriptionAr(entity.getDescriptionAr());
        domain.setActive(entity.isActive());
        domain.setDeleted(entity.isDeleted());
        domain.setDisplayOrder(entity.getDisplayOrder());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // ---- Utility methods ----

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

    private static Instant toInstant(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toInstant() : null;
    }
}
