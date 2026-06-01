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
        var responseStatus = resolveResponseStatus(agg);
        return new LoanApplicationDto(
                agg.getId().getValue().toString(),
                agg.getTenantId().toString(),
                agg.getApplicationNumber(),
                agg.getCustomerId().toString(),
                agg.getNationalId(),
                responseStatus,
                agg.getStatus().getStepperIndex(),
                agg.getStatus().getStepperLabel(),
                agg.getWorkflowId(),

                // Pre-qualification
                agg.getMonthlyIncome(),
                agg.getTotalExpenses(),
                agg.getExistingLiabilities(),
                agg.getAdultDependents(),
                agg.getChildDependents(),

                // Individual expense categories
                agg.getFoodGroceries(),
                agg.getUtilities(),
                agg.getHealthcare(),
                agg.getCommunication(),
                agg.getHousingRent(),
                agg.getClothingEssentials(),
                agg.getEducation(),
                agg.getTransportation(),

                // Step 1: Basic Info
                agg.getProductId() != null ? agg.getProductId().toString() : null,
                agg.getProductCode(),
                agg.getProductName(),
                agg.getShariaStructure() != null ? agg.getShariaStructure().name() : null,
                agg.getRequestedAmount(),
                agg.getRequestedTenureMonths(),
                agg.getPurposeOfFinance(),
                agg.getPurposeOfFinanceOther(),
                agg.getProfitRate(),
                agg.getApr(),

                // SafeWatch AML
                agg.getSafeWatchSessionId(),
                agg.getSafeWatchStatus(),

                // Masdar Employment
                agg.getEmployerName(),
                agg.getEmploymentSector(),
                agg.getEmploymentStatus(),
                agg.getBasicSalary(),
                agg.getTotalSalary(),
                agg.getEmploymentStartDate(),

                // AML Declaration
                agg.isAmlDeclarationCompleted(),
                agg.getAmlDeclarationAt(),

                // Step 2: Bank Account
                agg.getDisbursementBankCode(),
                agg.getDisbursementBankName(),
                agg.getDisbursementIban(),
                agg.getDisbursementAccountHolder(),
                agg.isIbanVerified(),

                // Step 3: Eligibility
                agg.isSimahConsent(),
                agg.getSimahConsentAt(),
                agg.getCreditScore(),
                agg.getSimahReferenceId(),
                agg.getVerifiedSalary(),
                agg.getMaxEligibleAmount(),

                // Step 3.5: Credit Decision Engine
                agg.getCreditDecision(),
                agg.getCreditDecisionReason(),
                agg.getScoringTotalScore(),
                agg.getScoringMaxScore(),
                agg.getScoringPercentage(),
                agg.getScoringGreenThreshold(),
                agg.getScoringAmberThreshold(),
                agg.getScoringSummary(),
                agg.getScoringDetailsJson(),
                agg.getScoringEvaluatedAt(),

                // Step 4: Offer
                agg.getOfferedAmount(),
                agg.getOfferedMonthlyInstallment(),
                agg.getOfferedTotalProfit(),
                agg.getOfferedTotalPayable(),
                agg.getProcessingFee(),
                agg.getAdminFee(),
                agg.getAcceptedAmount(),

                // Step 5: Contract
                agg.getContractExpiresAt(),
                agg.isOtpVerified(),
                agg.getOtpAttempts(),
                agg.isIvrVerified(),
                agg.getIvrAttempts(),

                // NABA + PaymentGuard
                agg.isNabaNotificationSent(),
                agg.getPaymentGuardSessionId(),
                agg.getPaymentGuardStatus(),

                // Audit
                agg.getCreatedAt(),
                agg.getUpdatedAt(),
                agg.getVersion(),

                // Loan / Disbursement (populated separately via withLoanData)
                null, null, null, null, null, null, null, null, null, null, null, null
        );
    }

    private String resolveResponseStatus(LoanApplicationAggregate agg) {
        var stage = agg.getCurrentStage();
        if (stage != null && "MANUAL_REVIEW".equals(stage.trim().toUpperCase())) {
            return "MANUAL_REVIEW";
        }
        return agg.getStatus().name();
    }

    public LoanApplicationDto withLoanData(LoanApplicationDto dto,
                                            String loanId, String loanNumber, String loanStatus,
                                            java.math.BigDecimal principalAmount, java.math.BigDecimal totalAmount,
                                            java.math.BigDecimal installmentAmount, String fineractLoanId,
                                            java.time.LocalDate disbursementDate,
                                            Integer currentTenureMonths,
                                            java.time.LocalDate currentMaturityDate,
                                            java.math.BigDecimal currentProfitRate,
                                            Boolean earlySettlementEligible) {
        return new LoanApplicationDto(
                dto.id(), dto.tenantId(), dto.applicationNumber(), dto.customerId(), dto.nationalId(),
                dto.status(), dto.stepperIndex(), dto.stepperLabel(), dto.workflowId(),
                dto.monthlyIncome(), dto.totalExpenses(), dto.existingLiabilities(),
                dto.adultDependents(), dto.childDependents(),
                dto.foodGroceries(), dto.utilities(), dto.healthcare(), dto.communication(),
                dto.housingRent(), dto.clothingEssentials(), dto.education(), dto.transportation(),
                dto.productId(), dto.productCode(), dto.productName(), dto.shariaStructure(),
                dto.requestedAmount(), dto.requestedTenureMonths(), dto.purposeOfFinance(),
                dto.purposeOfFinanceOther(), dto.profitRate(), dto.apr(),
                dto.safeWatchSessionId(), dto.safeWatchStatus(),
                dto.employerName(), dto.employmentSector(), dto.employmentStatus(),
                dto.basicSalary(), dto.totalSalary(), dto.employmentStartDate(),
                dto.amlDeclarationCompleted(), dto.amlDeclarationAt(),
                dto.disbursementBankCode(), dto.disbursementBankName(), dto.disbursementIban(),
                dto.disbursementAccountHolder(), dto.ibanVerified(),
                dto.simahConsent(), dto.simahConsentAt(), dto.creditScore(), dto.simahReferenceId(),
                dto.verifiedSalary(), dto.maxEligibleAmount(),
                dto.creditDecision(), dto.creditDecisionReason(),
                dto.scoringTotalScore(), dto.scoringMaxScore(), dto.scoringPercentage(),
                dto.scoringGreenThreshold(), dto.scoringAmberThreshold(),
                dto.scoringSummary(), dto.scoringDetailsJson(), dto.scoringEvaluatedAt(),
                dto.offeredAmount(), dto.offeredMonthlyInstallment(), dto.offeredTotalProfit(),
                dto.offeredTotalPayable(), dto.processingFee(), dto.adminFee(), dto.acceptedAmount(),
                dto.contractExpiresAt(), dto.otpVerified(), dto.otpAttempts(),
                dto.ivrVerified(), dto.ivrAttempts(),
                dto.nabaNotificationSent(), dto.paymentGuardSessionId(), dto.paymentGuardStatus(),
                dto.createdAt(), dto.updatedAt(), dto.version(),
                loanId, loanNumber, loanStatus, principalAmount, totalAmount,
                installmentAmount, fineractLoanId, disbursementDate,
                currentTenureMonths, currentMaturityDate, currentProfitRate, earlySettlementEligible
        );
    }

    public List<LoanApplicationDto> toDtos(List<LoanApplicationAggregate> aggregates) {
        return aggregates.stream().map(this::toDto).toList();
    }
}
