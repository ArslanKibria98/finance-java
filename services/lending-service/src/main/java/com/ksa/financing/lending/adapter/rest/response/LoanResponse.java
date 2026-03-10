package com.ksa.financing.lending.adapter.rest.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ksa.financing.lending.application.dto.LoanDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Loan response")
public record LoanResponse(
        @Schema(description = "Loan ID") String id,
        @Schema(description = "Loan number") String loanNumber,
        @Schema(description = "Application ID") String applicationId,
        @Schema(description = "Customer ID") String customerId,
        @Schema(description = "Product code") String productCode,
        @Schema(description = "Sharia structure") String shariaStructure,
        @Schema(description = "Principal amount in SAR") BigDecimal principalAmount,
        @Schema(description = "Profit amount in SAR") BigDecimal profitAmount,
        @Schema(description = "Total amount in SAR") BigDecimal totalAmount,
        @Schema(description = "Profit rate") BigDecimal profitRate,
        @Schema(description = "Tenure in months") int tenureMonths,
        @Schema(description = "Monthly installment in SAR") BigDecimal installmentAmount,
        @Schema(description = "Outstanding principal") BigDecimal outstandingPrincipal,
        @Schema(description = "Total outstanding") BigDecimal totalOutstanding,
        @Schema(description = "Loan status") String status,
        @JsonFormat(pattern = "yyyy-MM-dd") @Schema(description = "Booking date") LocalDate bookingDate,
        @JsonFormat(pattern = "yyyy-MM-dd") @Schema(description = "Disbursement date") LocalDate disbursementDate,
        @JsonFormat(pattern = "yyyy-MM-dd") @Schema(description = "Maturity date") LocalDate maturityDate,
        @Schema(description = "Current DPD") int currentDpd,
        @Schema(description = "IFRS9 stage") int ifrs9Stage,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") @Schema(description = "Created at") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") @Schema(description = "Updated at") LocalDateTime updatedAt
) {
    public static LoanResponse from(LoanDto dto) {
        return new LoanResponse(
                dto.id(),
                dto.loanNumber(),
                dto.applicationId(),
                dto.customerId(),
                dto.productCode(),
                dto.shariaStructure(),
                dto.principalAmount(),
                dto.profitAmount(),
                dto.totalAmount(),
                dto.profitRate(),
                dto.tenureMonths(),
                dto.installmentAmount(),
                dto.outstandingPrincipal(),
                dto.totalOutstanding(),
                dto.status(),
                dto.bookingDate(),
                dto.disbursementDate(),
                dto.maturityDate(),
                dto.currentDpd(),
                dto.ifrs9Stage(),
                dto.createdAt(),
                dto.updatedAt()
        );
    }
}
