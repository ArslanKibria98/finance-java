package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;

/**
 * Temporal activity interface for loan application processing.
 * Used by the LoanApplicationWorkflow to orchestrate the loan origination SAGA.
 */
@ActivityInterface
public interface LoanApplicationActivity {

    @ActivityMethod
    CreateApplicationResult createLoanApplication(CreateApplicationInput input);

    @ActivityMethod
    void updateApplicationStatus(UpdateStatusInput input);

    @ActivityMethod
    CreditCheckResult performCreditCheck(CreditCheckInput input);

    @ActivityMethod
    ShariaValidationResult validateShariaCompliance(ShariaValidationInput input);

    @ActivityMethod
    ProfitCalculationResult calculateProfit(ProfitCalculationInput input);

    @ActivityMethod
    ApprovalResult processApproval(ApprovalInput input);

    @ActivityMethod
    LoanCreationResult createLoan(LoanCreationInput input);

    @ActivityMethod
    void cancelApplication(CancelApplicationInput input);

    // ==================== DTOs ====================

    record CreateApplicationInput(
            String tenantId,
            String customerId,
            String productId,
            String productCode,
            String shariaStructure,
            BigDecimal requestedAmount,
            int requestedTenureMonths,
            String partnerId,
            String leadId,
            String createdBy
    ) {}

    record CreateApplicationResult(
            String applicationId,
            String applicationNumber,
            boolean created
    ) {}

    record UpdateStatusInput(
            String tenantId,
            String applicationId,
            String targetStatus,
            String updatedBy
    ) {}

    record CreditCheckInput(
            String tenantId,
            String customerId,
            String nationalId,
            BigDecimal requestedAmount,
            int requestedTenureMonths
    ) {}

    record CreditCheckResult(
            boolean passed,
            BigDecimal dbrBefore,
            BigDecimal dbrAfter,
            int creditScore,
            String reason
    ) {}

    record ShariaValidationInput(
            String tenantId,
            String productId,
            String shariaStructure,
            BigDecimal amount,
            int tenureMonths
    ) {}

    record ShariaValidationResult(
            boolean compliant,
            String certificationId,
            String reason
    ) {}

    record ProfitCalculationInput(
            String shariaStructure,
            BigDecimal principalAmount,
            BigDecimal profitRate,
            int tenureMonths
    ) {}

    record ProfitCalculationResult(
            BigDecimal totalProfit,
            BigDecimal totalRepayment,
            BigDecimal monthlyInstallment,
            BigDecimal sellingPrice
    ) {}

    record ApprovalInput(
            String tenantId,
            String applicationId,
            BigDecimal approvedAmount,
            int approvedTenureMonths,
            BigDecimal approvedProfitRate,
            BigDecimal totalProfit,
            BigDecimal totalRepayment,
            BigDecimal monthlyInstallment,
            String approvedBy
    ) {}

    record ApprovalResult(
            boolean approved,
            String reason
    ) {}

    record LoanCreationInput(
            String tenantId,
            String applicationId,
            String customerId,
            String productId,
            String productCode,
            String shariaStructure,
            BigDecimal principalAmount,
            BigDecimal profitAmount,
            BigDecimal profitRate,
            int tenureMonths,
            BigDecimal installmentAmount
    ) {}

    record LoanCreationResult(
            String loanId,
            String loanNumber,
            boolean created
    ) {}

    record CancelApplicationInput(
            String tenantId,
            String applicationId,
            String reason,
            String cancelledBy
    ) {}
}
