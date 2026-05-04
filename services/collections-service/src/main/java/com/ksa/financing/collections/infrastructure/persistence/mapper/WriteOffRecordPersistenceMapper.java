package com.ksa.financing.collections.infrastructure.persistence.mapper;

import com.ksa.financing.collections.domain.model.WriteOffId;
import com.ksa.financing.collections.domain.model.WriteOffRecord;
import com.ksa.financing.collections.domain.model.WriteOffStatus;
import com.ksa.financing.collections.domain.model.WriteOffTriggerType;
import com.ksa.financing.collections.infrastructure.persistence.entity.WriteOffRecordJpaEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class WriteOffRecordPersistenceMapper {

    public WriteOffRecordJpaEntity toJpa(WriteOffRecord record) {
        var entity = new WriteOffRecordJpaEntity();
        entity.setId(record.getId().getValue());
        entity.setTenantId(record.getTenantId());
        entity.setLoanId(record.getLoanId());
        entity.setScheduleId(record.getScheduleId());
        entity.setInstallmentId(record.getInstallmentId());
        entity.setDelinquencyRuleId(record.getDelinquencyRuleId());
        entity.setPrincipalAmount(record.getPrincipalAmount());
        entity.setProfitAmount(record.getProfitAmount());
        entity.setFeeAmount(record.getFeeAmount());
        entity.setPenaltyAmount(record.getPenaltyAmount());
        entity.setTotalAmount(record.getTotalAmount());
        entity.setDpdAtWriteOff(record.getDpdAtWriteOff());
        entity.setTriggerType(record.getTriggerType().name());
        entity.setReason(record.getReason());
        entity.setApprovalReference(record.getApprovalReference());
        entity.setStatus(record.getStatus().name());
        entity.setReversalReason(record.getReversalReason());
        entity.setReversedAt(record.getReversedAt());
        entity.setReversedBy(record.getReversedBy());
        entity.setWriteOffDate(record.getWriteOffDate());
        entity.setInitiatedBy(record.getInitiatedBy());
        entity.setCreatedAt(record.getCreatedAt() != null ? record.getCreatedAt() : LocalDateTime.now());
        entity.setUpdatedAt(record.getUpdatedAt() != null ? record.getUpdatedAt() : LocalDateTime.now());
        return entity;
    }

    public WriteOffRecord toDomain(WriteOffRecordJpaEntity entity) {
        return WriteOffRecord.hydrate(
                WriteOffId.of(entity.getId()),
                entity.getTenantId(),
                entity.getLoanId(),
                entity.getScheduleId(),
                entity.getInstallmentId(),
                entity.getDelinquencyRuleId(),
                entity.getPrincipalAmount(),
                entity.getProfitAmount(),
                entity.getFeeAmount(),
                entity.getPenaltyAmount(),
                entity.getDpdAtWriteOff(),
                WriteOffTriggerType.valueOf(entity.getTriggerType()),
                entity.getReason(),
                entity.getApprovalReference(),
                WriteOffStatus.valueOf(entity.getStatus()),
                entity.getReversalReason(),
                entity.getReversedAt(),
                entity.getReversedBy(),
                entity.getWriteOffDate(),
                entity.getInitiatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
