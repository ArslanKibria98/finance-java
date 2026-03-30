package com.ksa.financing.lending.adapter.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BRD UC#04 — Loan contract summary response.
 * Returns contract details from loan + application data.
 */
@Schema(description = "Loan contract summary")
public record LoanContractResponse(

        @Schema(description = "Loan ID")
        String loanId,

        @Schema(description = "Loan number")
        String loanNumber,

        @Schema(description = "Application ID")
        String applicationId,

        @Schema(description = "Customer ID")
        String customerId,

        @Schema(description = "Product code")
        String productCode,

        @Schema(description = "Sharia structure (MURABAHA, TAWARRUQ, IJARA, MUSHARAKAH)")
        String shariaStructure,

        @Schema(description = "Principal amount in SAR")
        BigDecimal principalAmount,

        @Schema(description = "Total profit amount in SAR")
        BigDecimal profitAmount,

        @Schema(description = "Total payable amount in SAR")
        BigDecimal totalAmount,

        @Schema(description = "Annual profit rate")
        BigDecimal profitRate,

        @Schema(description = "Tenure in months")
        int tenureMonths,

        @Schema(description = "Monthly installment in SAR")
        BigDecimal installmentAmount,

        @Schema(description = "Loan status")
        String status,

        @Schema(description = "Disbursement date")
        LocalDate disbursementDate,

        @Schema(description = "First installment due date")
        LocalDate firstDueDate,

        @Schema(description = "Maturity date")
        LocalDate maturityDate,

        @Schema(description = "Outstanding principal")
        BigDecimal outstandingPrincipal,

        @Schema(description = "Outstanding profit")
        BigDecimal outstandingProfit,

        @Schema(description = "Total outstanding balance")
        BigDecimal totalOutstanding,

        @Schema(description = "Contract PDF available for download")
        boolean pdfAvailable,

        @Schema(description = "Message if PDF not available")
        String pdfMessage
) {}
