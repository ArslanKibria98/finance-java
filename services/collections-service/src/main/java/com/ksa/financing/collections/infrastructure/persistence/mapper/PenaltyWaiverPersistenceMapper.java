package com.ksa.financing.collections.infrastructure.persistence.mapper;

import com.ksa.financing.collections.domain.model.PenaltyWaiver;
import com.ksa.financing.collections.domain.model.PenaltyWaiverId;
import com.ksa.financing.collections.domain.model.WaiverType;
import com.ksa.financing.collections.infrastructure.persistence.entity.PenaltyWaiverJpaEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PenaltyWaiverPersistenceMapper {

    public PenaltyWaiverJpaEntity toJpa(PenaltyWaiver waiver) {
        var entity = new PenaltyWaiverJpaEntity();
        entity.setId(waiver.getId().getValue());
        entity.setTenantId(waiver.getTenantId());
        entity.setLoanId(waiver.getLoanId());
        entity.setInstallmentId(waiver.getInstallmentId());
        entity.setOriginalPenalty(waiver.getOriginalPenalty());
        entity.setWaivedAmount(waiver.getWaivedAmount());
        entity.setRemainingPenalty(waiver.getRemainingPenalty());
        entity.setWaiverType(waiver.getWaiverType().name());
        entity.setReason(waiver.getReason());
        entity.setApprovalReference(waiver.getApprovalReference());
        entity.setWaivedBy(waiver.getWaivedBy());
        entity.setWaivedAt(waiver.getWaivedAt());
        entity.setCreatedAt(waiver.getCreatedAt() != null ? waiver.getCreatedAt() : LocalDateTime.now());
        return entity;
    }

    public PenaltyWaiver toDomain(PenaltyWaiverJpaEntity entity) {
        return PenaltyWaiver.hydrate(
                PenaltyWaiverId.of(entity.getId()),
                entity.getTenantId(),
                entity.getLoanId(),
                entity.getInstallmentId(),
                entity.getOriginalPenalty(),
                entity.getWaivedAmount(),
                entity.getRemainingPenalty(),
                WaiverType.valueOf(entity.getWaiverType()),
                entity.getReason(),
                entity.getApprovalReference(),
                entity.getWaivedBy(),
                entity.getWaivedAt(),
                entity.getCreatedAt());
    }
}
