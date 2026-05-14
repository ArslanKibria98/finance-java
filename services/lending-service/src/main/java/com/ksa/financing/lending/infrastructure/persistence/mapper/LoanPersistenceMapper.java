package com.ksa.financing.lending.infrastructure.persistence.mapper;

import com.ksa.financing.lending.domain.model.LoanAggregate;
import com.ksa.financing.lending.domain.model.LoanApplicationId;
import com.ksa.financing.lending.domain.model.LoanId;
import com.ksa.financing.lending.domain.model.LoanStatus;
import com.ksa.financing.lending.domain.model.ShariaStructure;
import com.ksa.financing.lending.infrastructure.persistence.entity.LoanJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Maps between LoanAggregate domain model and JPA entity.
 */
@Component
public class LoanPersistenceMapper {

    public LoanJpaEntity toEntity(LoanAggregate agg) {
        var entity = new LoanJpaEntity();
        entity.setId(agg.getId().getValue());
        entity.setTenantId(agg.getTenantId());
        entity.setLoanNumber(agg.getLoanNumber());
        entity.setApplicationId(agg.getApplicationId().getValue());
        entity.setCustomerId(agg.getCustomerId());
        entity.setProductId(agg.getProductId());
        entity.setProductCode(agg.getProductCode());
        entity.setShariaStructure(agg.getShariaStructure().name());
        entity.setCommodityTransactionId(agg.getCommodityTransactionId());
        entity.setPrincipalAmount(agg.getPrincipalAmount());
        entity.setProfitAmount(agg.getProfitAmount());
        entity.setFeeAmount(agg.getFeeAmount());
        entity.setTotalAmount(agg.getTotalAmount());
        entity.setProfitRate(agg.getProfitRate());
        entity.setTenureMonths(agg.getTenureMonths());
        entity.setInstallmentAmount(agg.getInstallmentAmount());
        entity.setOutstandingPrincipal(agg.getOutstandingPrincipal());
        entity.setOutstandingProfit(agg.getOutstandingProfit());
        entity.setOutstandingFees(agg.getOutstandingFees());
        entity.setTotalOutstanding(agg.getTotalOutstanding());
        entity.setStatus(agg.getStatus().name());
        entity.setBookingDate(agg.getBookingDate());
        entity.setDisbursementDate(agg.getDisbursementDate());
        entity.setFirstDueDate(agg.getFirstDueDate());
        entity.setMaturityDate(agg.getMaturityDate());
        entity.setSettlementDate(agg.getSettlementDate());
        entity.setCurrentDpd(agg.getCurrentDpd());
        entity.setMaxDpd(agg.getMaxDpd());
        entity.setIfrs9Stage(agg.getIfrs9Stage());
        entity.setFineractLoanId(agg.getFineractLoanId());
        entity.setCreatedAt(agg.getCreatedAt());
        entity.setUpdatedAt(agg.getUpdatedAt());
        entity.setVersion(agg.getVersion());
        return entity;
    }

    public LoanAggregate toDomain(LoanJpaEntity entity) {
        return LoanAggregate.reconstitute(
                LoanId.of(entity.getId()),
                entity.getTenantId(),
                entity.getLoanNumber(),
                LoanApplicationId.of(entity.getApplicationId()),
                entity.getCustomerId(),
                entity.getProductId(),
                entity.getProductCode(),
                ShariaStructure.valueOf(entity.getShariaStructure()),
                entity.getCommodityTransactionId(),
                entity.getPrincipalAmount(),
                entity.getProfitAmount(),
                entity.getFeeAmount(),
                entity.getTotalAmount(),
                entity.getProfitRate(),
                entity.getTenureMonths(),
                entity.getInstallmentAmount(),
                entity.getOutstandingPrincipal(),
                entity.getOutstandingProfit(),
                entity.getOutstandingFees(),
                entity.getTotalOutstanding(),
                LoanStatus.valueOf(entity.getStatus()),
                entity.getBookingDate(),
                entity.getDisbursementDate(),
                entity.getFirstDueDate(),
                entity.getMaturityDate(),
                entity.getSettlementDate(),
                entity.getCurrentDpd(),
                entity.getMaxDpd(),
                entity.getIfrs9Stage(),
                entity.getFineractLoanId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
