package com.ksa.financing.lending.application.mapper;

import com.ksa.financing.lending.application.dto.LoanDto;
import com.ksa.financing.lending.domain.model.LoanAggregate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps between LoanAggregate domain model and LoanDto.
 */
@Component
public class LoanMapper {

    public LoanDto toDto(LoanAggregate agg) {
        return new LoanDto(
                agg.getId().getValue().toString(),
                agg.getTenantId().toString(),
                agg.getLoanNumber(),
                agg.getApplicationId().getValue().toString(),
                agg.getCustomerId().toString(),
                agg.getProductId().toString(),
                agg.getProductCode(),
                agg.getShariaStructure().name(),
                agg.getCommodityTransactionId() != null ? agg.getCommodityTransactionId().toString() : null,
                agg.getPrincipalAmount(),
                agg.getProfitAmount(),
                agg.getTotalAmount(),
                agg.getProfitRate(),
                agg.getTenureMonths(),
                agg.getInstallmentAmount(),
                agg.getOutstandingPrincipal(),
                agg.getOutstandingProfit(),
                agg.getOutstandingFees(),
                agg.getTotalOutstanding(),
                agg.getStatus().name(),
                agg.getBookingDate(),
                agg.getDisbursementDate(),
                agg.getFirstDueDate(),
                agg.getMaturityDate(),
                agg.getSettlementDate(),
                agg.getCurrentDpd(),
                agg.getMaxDpd(),
                agg.getIfrs9Stage(),
                agg.getFineractLoanId(),
                agg.getCreatedAt(),
                agg.getUpdatedAt(),
                agg.getVersion()
        );
    }

    public List<LoanDto> toDtos(List<LoanAggregate> aggregates) {
        return aggregates.stream().map(this::toDto).toList();
    }
}
