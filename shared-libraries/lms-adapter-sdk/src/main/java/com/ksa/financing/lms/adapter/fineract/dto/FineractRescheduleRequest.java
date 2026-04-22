package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Fineract Loan Reschedule Request DTO.
 * Maps to POST /rescheduleloans Fineract API.
 *
 * Fineract reschedule supports:
 *   - graceOnPrincipalPayment: skip principal installments
 *   - graceOnInterestPayment:  skip profit installments
 *   - extraTerms:              extend tenure
 *   - adjustedDueDate:        change next due date
 *   - newInterestRate:        change profit rate (RESTRUCTURING)
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FineractRescheduleRequest {

    private Long loanId;
    private String rescheduleFromDate;      // "dd MMMM yyyy" format

    // Grace / Skip
    private Integer graceOnPrincipalPayment;
    private Integer graceOnInterestPayment;

    // Tenure extension
    private Integer extraTerms;             // Additional installments to add

    // New installment amount (for tenure changes)
    private BigDecimal adjustedInstallmentAmount;

    // Reschedule reason (Fineract requires this)
    private Long rescheduleReasonId;        // Configured reason codes in Fineract
    private String rescheduleReasonComment;

    // Dates
    private String submittedOnDate;

    // Format
    private String dateFormat;
    private String locale;
}
