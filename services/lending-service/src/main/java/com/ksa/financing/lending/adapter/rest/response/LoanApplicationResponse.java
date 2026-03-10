package com.ksa.financing.lending.adapter.rest.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ksa.financing.lending.application.dto.LoanApplicationDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Loan application response")
public record LoanApplicationResponse(
        @Schema(description = "Application ID") String id,
        @Schema(description = "Application number") String applicationNumber,
        @Schema(description = "Customer ID") String customerId,
        @Schema(description = "Product code") String productCode,
        @Schema(description = "Sharia structure") String shariaStructure,
        @Schema(description = "Requested amount in SAR") BigDecimal requestedAmount,
        @Schema(description = "Requested tenure in months") int requestedTenureMonths,
        @Schema(description = "Approved amount in SAR") BigDecimal approvedAmount,
        @Schema(description = "Approved tenure in months") Integer approvedTenureMonths,
        @Schema(description = "Approved profit rate") BigDecimal approvedProfitRate,
        @Schema(description = "Monthly installment in SAR") BigDecimal monthlyInstallment,
        @Schema(description = "Application status") String status,
        @Schema(description = "Current workflow stage") String currentStage,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") @Schema(description = "Submission timestamp") LocalDateTime submittedAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") @Schema(description = "Creation timestamp") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") @Schema(description = "Last update timestamp") LocalDateTime updatedAt
) {
    public static LoanApplicationResponse from(LoanApplicationDto dto) {
        return new LoanApplicationResponse(
                dto.id(),
                dto.applicationNumber(),
                dto.customerId(),
                dto.productCode(),
                dto.shariaStructure(),
                dto.requestedAmount(),
                dto.requestedTenureMonths(),
                dto.approvedAmount(),
                dto.approvedTenureMonths(),
                dto.approvedProfitRate(),
                dto.monthlyInstallment(),
                dto.status(),
                dto.currentStage(),
                dto.submittedAt(),
                dto.createdAt(),
                dto.updatedAt()
        );
    }
}
