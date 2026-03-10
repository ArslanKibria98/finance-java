package com.ksa.financing.lending.infrastructure.persistence.mapper;

import com.ksa.financing.lending.domain.model.ApplicationStatus;
import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;
import com.ksa.financing.lending.domain.model.LoanApplicationId;
import com.ksa.financing.lending.domain.model.ShariaStructure;
import com.ksa.financing.lending.infrastructure.persistence.entity.LoanApplicationJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Maps between LoanApplicationAggregate domain model and JPA entity.
 * Keeps domain layer independent of persistence concerns.
 */
@Component
public class LoanApplicationPersistenceMapper {

    public LoanApplicationJpaEntity toEntity(LoanApplicationAggregate agg) {
        var entity = new LoanApplicationJpaEntity();
        entity.setId(agg.getId().getValue());
        entity.setTenantId(agg.getTenantId());
        entity.setApplicationNumber(agg.getApplicationNumber());
        entity.setCustomerId(agg.getCustomerId());
        entity.setProductId(agg.getProductId());
        entity.setProductCode(agg.getProductCode());
        entity.setShariaStructure(agg.getShariaStructure().name());
        entity.setRequestedAmount(agg.getRequestedAmount());
        entity.setRequestedTenureMonths(agg.getRequestedTenureMonths());
        entity.setPartnerId(agg.getPartnerId());
        entity.setLeadId(agg.getLeadId());
        entity.setApprovedAmount(agg.getApprovedAmount());
        entity.setApprovedTenureMonths(agg.getApprovedTenureMonths());
        entity.setApprovedProfitRate(agg.getApprovedProfitRate());
        entity.setTotalProfit(agg.getTotalProfit());
        entity.setTotalRepayment(agg.getTotalRepayment());
        entity.setMonthlyInstallment(agg.getMonthlyInstallment());
        entity.setDbrBefore(agg.getDbrBefore());
        entity.setDbrAfter(agg.getDbrAfter());
        entity.setStatus(agg.getStatus().name());
        entity.setWorkflowId(agg.getWorkflowId());
        entity.setCurrentStage(agg.getCurrentStage());
        entity.setSubmittedAt(agg.getSubmittedAt());
        entity.setExpiresAt(agg.getExpiresAt());
        entity.setIdempotencyKey(agg.getIdempotencyKey());
        entity.setCreatedBy(agg.getCreatedBy());
        entity.setCreatedAt(agg.getCreatedAt());
        entity.setUpdatedBy(agg.getUpdatedBy());
        entity.setUpdatedAt(agg.getUpdatedAt());
        entity.setVersion(agg.getVersion());
        return entity;
    }

    public LoanApplicationAggregate toDomain(LoanApplicationJpaEntity entity) {
        return LoanApplicationAggregate.reconstitute(
                LoanApplicationId.of(entity.getId()),
                entity.getTenantId(),
                entity.getApplicationNumber(),
                entity.getCustomerId(),
                entity.getProductId(),
                entity.getProductCode(),
                ShariaStructure.valueOf(entity.getShariaStructure()),
                entity.getRequestedAmount(),
                entity.getRequestedTenureMonths(),
                entity.getPartnerId(),
                entity.getLeadId(),
                entity.getApprovedAmount(),
                entity.getApprovedTenureMonths(),
                entity.getApprovedProfitRate(),
                entity.getTotalProfit(),
                entity.getTotalRepayment(),
                entity.getMonthlyInstallment(),
                entity.getDbrBefore(),
                entity.getDbrAfter(),
                ApplicationStatus.valueOf(entity.getStatus()),
                entity.getWorkflowId(),
                entity.getCurrentStage(),
                entity.getSubmittedAt(),
                entity.getExpiresAt(),
                entity.getIdempotencyKey(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
