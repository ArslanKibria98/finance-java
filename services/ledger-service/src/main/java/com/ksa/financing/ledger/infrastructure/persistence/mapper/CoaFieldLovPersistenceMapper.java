package com.ksa.financing.ledger.infrastructure.persistence.mapper;

import com.ksa.financing.ledger.domain.model.CoaFieldLov;
import com.ksa.financing.ledger.domain.model.CoaFieldStatus;
import com.ksa.financing.ledger.infrastructure.persistence.entity.CoaFieldLovJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CoaFieldLovPersistenceMapper {

    public CoaFieldLovJpaEntity toJpaEntity(CoaFieldLov domain) {
        return CoaFieldLovJpaEntity.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId())
                .fieldKey(domain.getFieldKey())
                .fieldLabelEn(domain.getFieldLabelEn())
                .fieldLabelAr(domain.getFieldLabelAr())
                .category(domain.getCategory())
                .mandatoryDefault(domain.isMandatoryDefault())
                .displayOrder(domain.getDisplayOrder())
                .status(domain.getStatus().name())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .build();
    }

    public CoaFieldLov toDomain(CoaFieldLovJpaEntity entity) {
        return CoaFieldLov.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .fieldKey(entity.getFieldKey())
                .fieldLabelEn(entity.getFieldLabelEn())
                .fieldLabelAr(entity.getFieldLabelAr())
                .category(entity.getCategory())
                .mandatoryDefault(entity.isMandatoryDefault())
                .displayOrder(entity.getDisplayOrder())
                .status(CoaFieldStatus.valueOf(entity.getStatus()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }
}
