package com.ksa.financing.lms.adapter.fineract;

import com.ksa.financing.lms.adapter.fineract.dto.*;
import com.ksa.financing.lms.dto.*;
import com.ksa.financing.lms.intent.LoanIntent;
import com.ksa.financing.lms.intent.LoanProductIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mapper to convert between domain objects and Fineract DTOs.
 */
@Component
@RequiredArgsConstructor
public class FineractMapper {

    /**
     * Maps LoanProductIntent to FineractLoanProductRequest.
     */
    public FineractLoanProductRequest toFineractLoanProductRequest(LoanProductIntent intent) {
        BigDecimal defaultPrincipal = intent.getDefaultPrincipal() != null
                ? intent.getDefaultPrincipal()
                : intent.getMinPrincipal().add(intent.getMaxPrincipal())
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        int defaultTenure = intent.getDefaultTenureMonths() > 0
                ? intent.getDefaultTenureMonths()
                : (intent.getMinTenureMonths() + intent.getMaxTenureMonths()) / 2;

        String shortName = intent.getShortName() != null
                ? intent.getShortName()
                : intent.getProductCode().length() > 4
                    ? intent.getProductCode().substring(0, 4).toUpperCase()
                    : intent.getProductCode().toUpperCase();

        int interestType = "FLAT".equalsIgnoreCase(intent.getRateType()) ? 1 : 0;

        return FineractLoanProductRequest.builder()
                .name(intent.getName())
                .shortName(shortName)
                .description(intent.getDescription() != null ? intent.getDescription() : intent.getName())
                .currencyCode(intent.getCurrency() != null ? intent.getCurrency() : "SAR")
                .principal(defaultPrincipal)
                .minPrincipal(intent.getMinPrincipal())
                .maxPrincipal(intent.getMaxPrincipal())
                .numberOfRepayments(defaultTenure)
                .minNumberOfRepayments(intent.getMinTenureMonths())
                .maxNumberOfRepayments(intent.getMaxTenureMonths())
                .interestRatePerPeriod(intent.getAnnualProfitRate())
                .interestType(interestType)
                .graceOnPrincipalPayment(intent.getGracePeriodDays() > 0 ? 1 : null)
                .externalId(intent.getExternalId())
                .build();
    }

    /**
     * Maps LoanIntent to FineractLoanRequest.
     */
    public FineractLoanRequest toFineractLoanRequest(LoanIntent intent) {
        return FineractLoanRequest.builder()
            .clientId(Long.parseLong(intent.getCustomerId()))
            .productId(mapProductCodeToId(intent.getProductCode()))
            .principal(intent.getPrincipalAmount())
            .loanTermFrequency(intent.getTenureInMonths())
            .loanTermFrequencyType(2) // Months
            .numberOfRepayments(intent.getTenureInMonths())
            .repaymentEvery(1) // Monthly
            .repaymentFrequencyType(2) // Months
            .interestRatePerPeriod(calculateMonthlyProfitRate(intent))
            .amortizationType(1) // Equal installments
            .interestType(0) // Declining balance
            .interestCalculationPeriodType(1) // Same as repayment
            .expectedDisbursementDate(intent.getExpectedDisbursementDate())
            .submittedOnDate(LocalDate.now())
            .loanOfficerId(Long.parseLong(intent.getOfficerId()))
            .externalId(intent.getRequestId())
            .transactionProcessingStrategyId(1L) // Default strategy
            .dateFormat("dd MMMM yyyy")
            .locale("en")
            .build();
    }

    /**
     * Maps Fineract loan details to domain LoanDetails.
     */
    public LoanDetails toLoanDetails(FineractLoanDetails fineractDetails, LoanAccountId loanAccountId) {
        return LoanDetails.builder()
            .loanAccountId(loanAccountId)
            .accountNumber(fineractDetails.getAccountNo())
            .customerId(String.valueOf(fineractDetails.getClientId()))
            .customerName(fineractDetails.getClientName())
            .productCode(String.valueOf(fineractDetails.getLoanProductId()))
            .productName(fineractDetails.getLoanProductName())
            .principalAmount(fineractDetails.getPrincipal())
            .profitAmount(calculateTotalProfit(fineractDetails))
            .totalAmount(fineractDetails.getPrincipal().add(calculateTotalProfit(fineractDetails)))
            .tenureInMonths(fineractDetails.getNumberOfRepayments())
            .repaymentFrequency(mapFrequencyType(fineractDetails.getRepaymentFrequencyType()))
            .status(mapFineractStatus(fineractDetails.getStatus()))
            .approvalDate(fineractDetails.getApprovedOnDate())
            .disbursementDate(fineractDetails.getActualDisbursementDate())
            .maturityDate(fineractDetails.getMaturityDate())
            .disbursedAmount(fineractDetails.getDisbursedAmount())
            .outstandingPrincipal(fineractDetails.getPrincipalOutstanding())
            .outstandingProfit(fineractDetails.getInterestOutstanding())
            .totalOutstanding(fineractDetails.getTotalOutstanding())
            .installmentAmount(fineractDetails.getInstallmentAmount())
            .lastPaymentDate(fineractDetails.getLastPaymentDate())
            .lastPaymentAmount(fineractDetails.getLastPaymentAmount())
            .nextDueDate(fineractDetails.getNextPaymentDueDate())
            .installmentsPaid(fineractDetails.getNumberOfRepaymentsPaid())
            .installmentsRemaining(fineractDetails.getNumberOfRepaymentsRemaining())
            .daysInArrears(fineractDetails.getDaysInArrears() != null ? fineractDetails.getDaysInArrears() : 0)
            .arrearsAmount(fineractDetails.getTotalOverdue())
            .penaltyAmount(fineractDetails.getPenaltyChargesOutstanding())
            .charityAmount(fineractDetails.getPenaltyChargesOutstanding()) // Penalties go to charity
            .branchCode(String.valueOf(fineractDetails.getOfficeId()))
            .officerId(String.valueOf(fineractDetails.getLoanOfficerId()))
            .lastModifiedDate(fineractDetails.getLastModifiedOn())
            .build();
    }

    /**
     * Maps Fineract repayment schedule to domain Installments.
     */
    public List<Installment> toInstallments(FineractRepaymentSchedule schedule) {
        List<Installment> installments = new ArrayList<>();

        if (schedule.getPeriods() != null) {
            int installmentNumber = 0;
            for (FineractRepaymentPeriod period : schedule.getPeriods()) {
                if (period.getPeriod() > 0) { // Skip disbursement period
                    installmentNumber++;
                    installments.add(toInstallment(period, installmentNumber));
                }
            }
        }

        return installments;
    }

    private Installment toInstallment(FineractRepaymentPeriod period, int installmentNumber) {
        return Installment.builder()
            .installmentNumber(installmentNumber)
            .dueDate(period.getDueDate())
            .totalAmount(period.getTotalDueForPeriod())
            .principalAmount(period.getPrincipalDue())
            .profitAmount(period.getInterestDue())
            .feeAmount(period.getFeeChargesDue())
            .status(mapInstallmentStatus(period))
            .paidDate(period.getObligationsMetOnDate())
            .paidAmount(period.getTotalPaidForPeriod())
            .principalBalance(period.getPrincipalLoanBalanceOutstanding())
            .profitBalance(period.getInterestOutstanding())
            .daysOverdue(calculateDaysOverdue(period))
            .penaltyAmount(period.getPenaltyChargesDue())
            .charityAmount(period.getPenaltyChargesDue()) // Islamic finance
            .paymentReference(null) // Not available in basic schedule
            .isGracePeriod(false)
            .build();
    }

    private Installment.InstallmentStatus mapInstallmentStatus(FineractRepaymentPeriod period) {
        if (period.isComplete()) {
            return Installment.InstallmentStatus.PAID;
        } else if (period.getTotalPaidForPeriod() != null &&
                   period.getTotalPaidForPeriod().compareTo(BigDecimal.ZERO) > 0) {
            return Installment.InstallmentStatus.PARTIALLY_PAID;
        } else if (period.getDueDate().isBefore(LocalDate.now())) {
            return Installment.InstallmentStatus.OVERDUE;
        } else {
            return Installment.InstallmentStatus.PENDING;
        }
    }

    private int calculateDaysOverdue(FineractRepaymentPeriod period) {
        if (!period.isComplete() && period.getDueDate().isBefore(LocalDate.now())) {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(
                period.getDueDate(), LocalDate.now());
        }
        return 0;
    }

    private BigDecimal calculateMonthlyProfitRate(LoanIntent intent) {
        // Calculate monthly profit rate from total profit
        if (intent.getProfitAmount() != null && intent.getPrincipalAmount() != null) {
            BigDecimal totalRate = intent.getProfitAmount()
                .divide(intent.getPrincipalAmount(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            return totalRate.divide(BigDecimal.valueOf(intent.getTenureInMonths()), 4, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateTotalProfit(FineractLoanDetails details) {
        // In Islamic finance, this would be the agreed profit amount
        return details.getInterestCharged() != null ? details.getInterestCharged() : BigDecimal.ZERO;
    }

    private Long mapProductCodeToId(String productCode) {
        // Map product codes to Fineract product IDs
        // This would be configured in a mapping table
        Map<String, Long> productMap = Map.of(
            "PERSONAL", 1L,
            "AUTO", 2L,
            "HOME", 3L,
            "BUSINESS", 4L
        );
        return productMap.getOrDefault(productCode, 1L);
    }

    private String mapFrequencyType(Integer frequencyType) {
        return switch (frequencyType) {
            case 0 -> "DAYS";
            case 1 -> "WEEKS";
            case 2 -> "MONTHS";
            case 3 -> "YEARS";
            default -> "MONTHS";
        };
    }

    private LoanStatus mapFineractStatus(FineractLoanStatus status) {
        if (status == null || status.getValue() == null) {
            return LoanStatus.SUBMITTED;
        }

        return switch (status.getValue().toLowerCase()) {
            case "submitted and pending approval" -> LoanStatus.SUBMITTED;
            case "approved" -> LoanStatus.APPROVED;
            case "active" -> LoanStatus.ACTIVE;
            case "rejected" -> LoanStatus.REJECTED;
            case "withdrawn by applicant" -> LoanStatus.WITHDRAWN;
            case "closed (obligations met)" -> LoanStatus.CLOSED_OBLIGATIONS_MET;
            case "closed (written off)" -> LoanStatus.CLOSED_WRITTEN_OFF;
            case "closed (reschedule outstanding amount)" -> LoanStatus.RESTRUCTURED;
            case "overpaid" -> LoanStatus.CLOSED_PREPAID;
            default -> LoanStatus.SUBMITTED;
        };
    }
}