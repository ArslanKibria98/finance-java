package com.ksa.financing.lending.adapter.temporal.activity;

import com.ksa.financing.lending.domain.model.LoanApplicationId;
import com.ksa.financing.lending.domain.model.ShariaStructure;
import com.ksa.financing.lending.domain.port.in.ManageLoanApplicationUseCase;
import com.ksa.financing.lending.domain.port.in.ManageLoanUseCase;
import com.ksa.financing.lending.domain.port.out.LoanApplicationRepository;
import com.ksa.islamic.orchestration.activity.lending.LoanApplicationActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanApplicationActivityImpl implements LoanApplicationActivity {

    private final ManageLoanApplicationUseCase applicationUseCase;
    private final ManageLoanUseCase loanUseCase;
    private final LoanApplicationRepository applicationRepository;

    @Override
    public CreateApplicationResult createLoanApplication(CreateApplicationInput input) {
        log.info("Activity: Creating loan application for customer: {}", input.customerId());

        var command = new ManageLoanApplicationUseCase.CreateApplicationCommand(
                UUID.fromString(input.tenantId()),
                UUID.fromString(input.customerId()),
                UUID.fromString(input.productId()),
                input.productCode(),
                ShariaStructure.valueOf(input.shariaStructure()),
                input.requestedAmount(),
                input.requestedTenureMonths(),
                input.partnerId() != null ? UUID.fromString(input.partnerId()) : null,
                input.leadId() != null ? UUID.fromString(input.leadId()) : null,
                UUID.fromString(input.createdBy()),
                null  // idempotencyKey — Temporal provides its own idempotency via workflow ID
        );

        var aggregate = applicationUseCase.createApplication(command);

        return new CreateApplicationResult(
                aggregate.getId().getValue().toString(),
                aggregate.getApplicationNumber(),
                true
        );
    }

    @Override
    public void updateApplicationStatus(UpdateStatusInput input) {
        log.info("Activity: Updating application {} status to {}", input.applicationId(), input.targetStatus());

        var tenantId = UUID.fromString(input.tenantId());
        var appId = LoanApplicationId.of(UUID.fromString(input.applicationId()));
        var updatedBy = UUID.fromString(input.updatedBy());

        var aggregate = applicationRepository.findById(tenantId, appId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + input.applicationId()));

        switch (input.targetStatus()) {
            case "SUBMITTED" -> aggregate.submit(updatedBy);
            case "DOCUMENTS_PENDING" -> aggregate.moveToDocumentsPending(updatedBy);
            case "UNDER_REVIEW" -> aggregate.moveToUnderReview(updatedBy);
            case "CREDIT_CHECK" -> aggregate.moveToCreditCheck(updatedBy);
            case "SHARIA_VALIDATION" -> aggregate.moveToShariaValidation(updatedBy);
            case "PENDING_APPROVAL" -> aggregate.moveToPendingApproval(updatedBy);
            case "REJECTED" -> aggregate.reject("Workflow rejection", updatedBy);
            case "CANCELLED" -> aggregate.cancel(updatedBy);
            default -> throw new IllegalArgumentException("Unknown target status: " + input.targetStatus());
        }

        applicationRepository.save(aggregate);
    }

    @Override
    public CreditCheckResult performCreditCheck(CreditCheckInput input) {
        log.info("Activity: Performing credit check for customer: {}", input.customerId());
        // TODO: Call credit-service via REST or Temporal activity
        // For now, return a passing result as placeholder
        return new CreditCheckResult(
                true,
                new BigDecimal("0.35"),
                new BigDecimal("0.55"),
                720,
                null
        );
    }

    @Override
    public ShariaValidationResult validateShariaCompliance(ShariaValidationInput input) {
        log.info("Activity: Validating sharia compliance for product: {}", input.productId());
        // TODO: Call sharia-service via REST or Temporal activity
        return new ShariaValidationResult(
                true,
                UUID.randomUUID().toString(),
                null
        );
    }

    @Override
    public ProfitCalculationResult calculateProfit(ProfitCalculationInput input) {
        log.info("Activity: Calculating profit for {} structure", input.shariaStructure());

        // Use domain-core-sdk MurabahaCalculator in production
        // Simplified calculation for now
        var principal = input.principalAmount();
        var rate = input.profitRate();
        var months = input.tenureMonths();

        var totalProfit = principal.multiply(rate)
                .multiply(BigDecimal.valueOf(months))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        var totalRepayment = principal.add(totalProfit);
        var monthlyInstallment = totalRepayment.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        return new ProfitCalculationResult(
                totalProfit,
                totalRepayment,
                monthlyInstallment,
                totalRepayment
        );
    }

    @Override
    public ApprovalResult processApproval(ApprovalInput input) {
        log.info("Activity: Processing approval for application: {}", input.applicationId());

        var tenantId = UUID.fromString(input.tenantId());
        var appId = LoanApplicationId.of(UUID.fromString(input.applicationId()));
        var approvedBy = UUID.fromString(input.approvedBy());

        var aggregate = applicationRepository.findById(tenantId, appId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + input.applicationId()));

        aggregate.approve(
                input.approvedAmount(),
                input.approvedTenureMonths(),
                input.approvedProfitRate(),
                input.totalProfit(),
                input.totalRepayment(),
                input.monthlyInstallment(),
                approvedBy
        );

        applicationRepository.save(aggregate);

        return new ApprovalResult(true, null);
    }

    @Override
    public LoanCreationResult createLoan(LoanCreationInput input) {
        log.info("Activity: Creating loan from application: {}", input.applicationId());

        var command = new ManageLoanUseCase.CreateLoanCommand(
                UUID.fromString(input.tenantId()),
                LoanApplicationId.of(UUID.fromString(input.applicationId())),
                UUID.fromString(input.customerId()),
                UUID.fromString(input.productId()),
                input.productCode(),
                ShariaStructure.valueOf(input.shariaStructure()),
                input.principalAmount(),
                input.profitAmount(),
                input.profitRate(),
                input.tenureMonths(),
                input.installmentAmount()
        );

        var loan = loanUseCase.createLoanFromApplication(command);

        return new LoanCreationResult(
                loan.getId().getValue().toString(),
                loan.getLoanNumber(),
                true
        );
    }

    @Override
    public void cancelApplication(CancelApplicationInput input) {
        log.info("Activity: Cancelling application: {} — reason: {}", input.applicationId(), input.reason());

        var tenantId = UUID.fromString(input.tenantId());
        var appId = LoanApplicationId.of(UUID.fromString(input.applicationId()));
        var cancelledBy = UUID.fromString(input.cancelledBy());

        applicationRepository.findById(tenantId, appId).ifPresent(aggregate -> {
            try {
                aggregate.cancel(cancelledBy);
                applicationRepository.save(aggregate);
            } catch (Exception e) {
                log.warn("Could not cancel application {} (may already be terminal): {}",
                        input.applicationId(), e.getMessage());
            }
        });
    }
}
