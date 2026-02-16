package com.ksa.financing.domain.port.in;

import com.ksa.financing.domain.enums.PaymentMethod;
import com.ksa.financing.domain.valueobject.LoanId;
import com.ksa.financing.domain.valueobject.SarMoney;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Input port (use case) for recording a payment against a loan.
 * <p>
 * This use case handles the recording and application of customer payments
 * to reduce outstanding loan balances. Payments are allocated between
 * principal and profit according to the payment allocation strategy.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public interface RecordPaymentUseCase {

    /**
     * Execute the record payment use case.
     *
     * @param command the record payment command
     * @return the result indicating success or failure
     */
    Result execute(RecordPaymentCommand command);

    /**
     * Command for recording a payment.
     *
     * @param loanId the ID of the loan to apply payment to
     * @param paymentAmount the total payment amount
     * @param paymentDate the date the payment was received
     * @param paymentMethod the method of payment
     * @param paymentReference the unique payment reference
     */
    record RecordPaymentCommand(
            @NotNull LoanId loanId,
            @NotNull SarMoney paymentAmount,
            @NotNull LocalDate paymentDate,
            @NotNull PaymentMethod paymentMethod,
            @NotNull String paymentReference
    ) {
        /**
         * Canonical constructor with validation.
         */
        public RecordPaymentCommand {
            Objects.requireNonNull(loanId, "LoanId cannot be null");
            Objects.requireNonNull(paymentAmount, "PaymentAmount cannot be null");
            Objects.requireNonNull(paymentDate, "PaymentDate cannot be null");
            Objects.requireNonNull(paymentMethod, "PaymentMethod cannot be null");
            Objects.requireNonNull(paymentReference, "PaymentReference cannot be null");

            if (!paymentAmount.isPositive()) {
                throw new IllegalArgumentException("PaymentAmount must be positive");
            }

            if (paymentReference.trim().isEmpty()) {
                throw new IllegalArgumentException("PaymentReference cannot be empty");
            }
        }
    }

    /**
     * Result of the record payment use case.
     *
     * @param success true if payment was recorded successfully
     * @param loanId the loan ID
     * @param principalPaid the amount allocated to principal
     * @param profitPaid the amount allocated to profit
     * @param remainingPrincipal the remaining principal balance
     * @param remainingProfit the remaining profit balance
     * @param errorMessage the error message (present if failed)
     */
    record Result(
            boolean success,
            LoanId loanId,
            SarMoney principalPaid,
            SarMoney profitPaid,
            SarMoney remainingPrincipal,
            SarMoney remainingProfit,
            String errorMessage
    ) {
        /**
         * Create a successful result.
         *
         * @param loanId the loan ID
         * @param principalPaid the principal paid
         * @param profitPaid the profit paid
         * @param remainingPrincipal the remaining principal
         * @param remainingProfit the remaining profit
         * @return the success result
         */
        public static Result success(LoanId loanId, SarMoney principalPaid, SarMoney profitPaid,
                                      SarMoney remainingPrincipal, SarMoney remainingProfit) {
            Objects.requireNonNull(loanId, "LoanId cannot be null");
            Objects.requireNonNull(principalPaid, "PrincipalPaid cannot be null");
            Objects.requireNonNull(profitPaid, "ProfitPaid cannot be null");
            Objects.requireNonNull(remainingPrincipal, "RemainingPrincipal cannot be null");
            Objects.requireNonNull(remainingProfit, "RemainingProfit cannot be null");
            return new Result(true, loanId, principalPaid, profitPaid,
                    remainingPrincipal, remainingProfit, null);
        }

        /**
         * Create a failure result.
         *
         * @param errorMessage the error message
         * @return the failure result
         */
        public static Result failure(String errorMessage) {
            Objects.requireNonNull(errorMessage, "ErrorMessage cannot be null");
            return new Result(false, null, null, null, null, null, errorMessage);
        }
    }
}
