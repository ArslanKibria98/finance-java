package com.ksa.financing.lending.adapter.rest.response;

import com.ksa.financing.lending.domain.port.in.CheckEligibilityUseCase.EligibilityCheckResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "BRD Steps 4-10: Pre-qualification / Eligibility check result")
public record CheckEligibilityResponse(

        @Schema(description = "Whether user is eligible for financing")
        boolean eligible,

        @Schema(description = "Monthly installment in SAR")
        BigDecimal monthlyInstallment,

        @Schema(description = "Finance tenure in months")
        int tenure,

        @Schema(description = "Number of installments")
        int numInstallments,

        @Schema(description = "Total payable amount in SAR")
        BigDecimal totalPayable,

        @Schema(description = "Cost of term (profit) in SAR")
        BigDecimal costOfTerm,

        @Schema(description = "DBR before new finance (%)")
        BigDecimal dbrBefore,

        @Schema(description = "DBR after new finance (%)")
        BigDecimal dbrAfter,

        @Schema(description = "Disposable income after all obligations in SAR")
        BigDecimal disposableIncome,

        @Schema(description = "Maximum eligible amount if requested exceeds DBR cap")
        BigDecimal maxEligibleAmount,

        @Schema(description = "First installment due date (30 days from today)")
        LocalDate firstInstallmentDueDate,

        @Schema(description = "Reason for rejection (null if eligible)")
        String reason
) {
    public static CheckEligibilityResponse from(EligibilityCheckResult result) {
        return new CheckEligibilityResponse(
                result.eligible(),
                result.monthlyInstallment(),
                result.tenure(),
                result.numInstallments(),
                result.totalPayable(),
                result.costOfTerm(),
                result.dbrBefore(),
                result.dbrAfter(),
                result.disposableIncome(),
                result.maxEligibleAmount(),
                result.firstInstallmentDueDate(),
                result.reason()
        );
    }
}
