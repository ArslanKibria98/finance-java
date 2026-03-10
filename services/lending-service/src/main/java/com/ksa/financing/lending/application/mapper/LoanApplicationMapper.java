package com.ksa.financing.lending.application.mapper;

import com.ksa.financing.lending.application.dto.LoanApplicationDto;
import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps between LoanApplicationAggregate domain model and LoanApplicationDto.
 */
@Component
public class LoanApplicationMapper {

    public LoanApplicationDto toDto(LoanApplicationAggregate agg) {
        return new LoanApplicationDto(
                agg.getId().getValue().toString(),
                agg.getTenantId().toString(),
                agg.getApplicationNumber(),
                agg.getCustomerId().toString(),
                agg.getProductId().toString(),
                agg.getProductCode(),
                agg.getShariaStructure().name(),
                agg.getRequestedAmount(),
                agg.getRequestedTenureMonths(),
                agg.getPartnerId() != null ? agg.getPartnerId().toString() : null,
                agg.getLeadId() != null ? agg.getLeadId().toString() : null,
                agg.getApprovedAmount(),
                agg.getApprovedTenureMonths(),
                agg.getApprovedProfitRate(),
                agg.getTotalProfit(),
                agg.getTotalRepayment(),
                agg.getMonthlyInstallment(),
                agg.getDbrBefore(),
                agg.getDbrAfter(),
                agg.getStatus().name(),
                agg.getWorkflowId(),
                agg.getCurrentStage(),
                agg.getSubmittedAt(),
                agg.getExpiresAt(),
                agg.getCreatedBy() != null ? agg.getCreatedBy().toString() : null,
                agg.getCreatedAt(),
                agg.getUpdatedBy() != null ? agg.getUpdatedBy().toString() : null,
                agg.getUpdatedAt(),
                agg.getVersion()
        );
    }

    public List<LoanApplicationDto> toDtos(List<LoanApplicationAggregate> aggregates) {
        return aggregates.stream().map(this::toDto).toList();
    }
}
