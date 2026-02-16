package com.ksa.islamic.reporting.event;

import com.ksa.islamic.reporting.projector.BaseProjectableEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Loan domain events for projection into read models.
 */
public class LoanEvents {

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    public static class LoanApproved extends BaseProjectableEvent {
        private String loanAccountNumber;
        private String customerId;
        private String customerName;
        private String productType;
        private BigDecimal principalAmount;
        private BigDecimal interestRate;
        private Integer termMonths;
        private LocalDate approvalDate;
        private String approverUserId;
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    public static class LoanDisbursed extends BaseProjectableEvent {
        private String loanAccountNumber;
        private String customerId;
        private BigDecimal disbursementAmount;
        private LocalDate disbursementDate;
        private String disbursementChannel;
        private String bankAccountNumber;
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    public static class PaymentReceived extends BaseProjectableEvent {
        private String loanAccountNumber;
        private String customerId;
        private String paymentId;
        private BigDecimal paymentAmount;
        private BigDecimal principalPortion;
        private BigDecimal interestPortion;
        private BigDecimal lateFee;
        private LocalDate paymentDate;
        private String paymentChannel;
        private BigDecimal outstandingAfter;
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    public static class LoanOverdue extends BaseProjectableEvent {
        private String loanAccountNumber;
        private String customerId;
        private Integer daysOverdue;
        private BigDecimal overdueAmount;
        private Integer overdueInstallments;
        private LocalDate lastPaymentDate;
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    public static class LoanClosed extends BaseProjectableEvent {
        private String loanAccountNumber;
        private String customerId;
        private LocalDate closureDate;
        private String closureReason; // FULLY_PAID, PREPAID, WRITTEN_OFF
        private BigDecimal finalPaymentAmount;
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    public static class LoanRestructured extends BaseProjectableEvent {
        private String loanAccountNumber;
        private String customerId;
        private BigDecimal newPrincipalAmount;
        private BigDecimal newInterestRate;
        private Integer newTermMonths;
        private LocalDate restructureDate;
        private String restructureReason;
    }
}