package com.ksa.financing.collections.infrastructure.persistence.mapper;

import com.ksa.financing.collections.domain.model.PenaltyWaiverRequest;
import com.ksa.financing.collections.infrastructure.persistence.entity.PenaltyWaiverRequestJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PenaltyWaiverRequestPersistenceMapper {

    public PenaltyWaiverRequestJpaEntity toEntity(PenaltyWaiverRequest domain) {
        if (domain == null) return null;
        var entity = new PenaltyWaiverRequestJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setLoanId(domain.getLoanId());
        entity.setApplicationId(domain.getApplicationId());
        entity.setInvoiceId(domain.getInvoiceId());
        entity.setInstallmentId(domain.getInstallmentId());
        entity.setRequestedAmount(domain.getRequestedAmount());
        entity.setReason(domain.getReason());
        entity.setStatus(domain.getStatus().name());
        entity.setRequestedBy(domain.getRequestedBy());
        entity.setRequestedAt(domain.getRequestedAt());
        entity.setProcessedBy(domain.getProcessedBy());
        entity.setProcessedAt(domain.getProcessedAt());
        entity.setRejectionReason(domain.getRejectionReason());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public PenaltyWaiverRequest toDomain(PenaltyWaiverRequestJpaEntity entity) {
        if (entity == null) return null;
        return PenaltyWaiverRequest.reconstitute(
                entity.getId(),
                entity.getTenantId(),
                entity.getLoanId(),
                entity.getApplicationId(),
                entity.getInvoiceId(),
                entity.getInstallmentId(),
                entity.getRequestedAmount(),
                entity.getReason(),
                PenaltyWaiverRequest.WaiverRequestStatus.valueOf(entity.getStatus()),
                entity.getRequestedBy(),
                entity.getRequestedAt(),
                entity.getProcessedBy(),
                entity.getProcessedAt(),
                entity.getRejectionReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
