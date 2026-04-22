package com.ksa.financing.collections.infrastructure.persistence.mapper;

import com.ksa.financing.collections.domain.model.SettlementAggregate;
import com.ksa.financing.collections.domain.model.SettlementId;
import com.ksa.financing.collections.infrastructure.persistence.entity.SettlementJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SettlementPersistenceMapper {

    public SettlementJpaEntity toEntity(SettlementAggregate agg) {
        SettlementJpaEntity entity = new SettlementJpaEntity();
        entity.setId(agg.getId().value());
        entity.setTenantId(agg.getTenantId());
        entity.setSettlementNumber(agg.getSettlementNumber());
        entity.setLoanId(agg.getLoanId());
        entity.setCustomerId(agg.getCustomerId());
        entity.setSettlementType(agg.getSettlementType());
        entity.setSettlementAmount(agg.getSettlementAmount());
        entity.setIbraAmount(agg.getIbraAmount());
        entity.setDiscountAmount(agg.getDiscountAmount());
        entity.setSettlementDate(agg.getSettlementDate());
        entity.setIdempotencyKey(agg.getIdempotencyKey());
        entity.setStatus(agg.getStatus());
        entity.setPaymentId(agg.getPaymentId());
        entity.setCreatedAt(agg.getCreatedAt());
        entity.setUpdatedAt(agg.getUpdatedAt());
        entity.setVersion(agg.getVersion());
        return entity;
    }

    public SettlementAggregate toDomain(SettlementJpaEntity entity) {
        return SettlementAggregate.reconstitute(
                SettlementId.of(entity.getId()),
                entity.getTenantId(),
                entity.getSettlementNumber(),
                entity.getLoanId(),
                entity.getCustomerId(),
                entity.getSettlementType(),
                entity.getSettlementAmount(),
                entity.getIbraAmount(),
                entity.getDiscountAmount(),
                entity.getSettlementDate(),
                entity.getIdempotencyKey(),
                entity.getStatus(),
                entity.getPaymentId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
