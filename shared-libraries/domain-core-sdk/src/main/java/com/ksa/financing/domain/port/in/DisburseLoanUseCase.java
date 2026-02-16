package com.ksa.financing.domain.port.in;

import com.ksa.financing.domain.valueobject.LoanId;
import com.ksa.financing.domain.valueobject.SarMoney;
import com.ksa.financing.domain.valueobject.WalletId;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Input port (use case) for disbursing an approved loan.
 * <p>
 * This use case handles the disbursement of loan funds to the customer's
 * wallet or bank account. The loan status changes from PENDING_DISBURSEMENT
 * to ACTIVE, and the maturity date is calculated based on the tenure.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public interface DisburseLoanUseCase {

    /**
     * Execute the disburse loan use case.
     *
     * @param command the disburse loan command
     * @return the result indicating success or failure
     */
    Result execute(DisburseLoanCommand command);

    /**
     * Command for disbursing a loan.
     *
     * @param loanId the ID of the loan to disburse
     * @param disbursementDate the date of disbursement
     * @param destinationWalletId the wallet to credit (optional, if using wallet)
     * @param disbursementMethod the method of disbursement (WALLET, BANK_TRANSFER, etc.)
     * @param disbursementReference the transaction reference
     */
    record DisburseLoanCommand(
            @NotNull LoanId loanId,
            @NotNull LocalDate disbursementDate,
            WalletId destinationWalletId,
            @NotNull String disbursementMethod,
            String disbursementReference
    ) {
        /**
         * Canonical constructor with validation.
         */
        public DisburseLoanCommand {
            Objects.requireNonNull(loanId, "LoanId cannot be null");
            Objects.requireNonNull(disbursementDate, "DisbursementDate cannot be null");
            Objects.requireNonNull(disbursementMethod, "DisbursementMethod cannot be null");

            if (disbursementMethod.trim().isEmpty()) {
                throw new IllegalArgumentException("DisbursementMethod cannot be empty");
            }
        }
    }

    /**
     * Result of the disburse loan use case.
     *
     * @param success true if loan was disbursed successfully
     * @param loanId the disbursed loan ID
     * @param disbursedAmount the amount disbursed
     * @param maturityDate the calculated maturity date
     * @param errorMessage the error message (present if failed)
     */
    record Result(
            boolean success,
            LoanId loanId,
            SarMoney disbursedAmount,
            LocalDate maturityDate,
            String errorMessage
    ) {
        /**
         * Create a successful result.
         *
         * @param loanId the disbursed loan ID
         * @param disbursedAmount the amount disbursed
         * @param maturityDate the maturity date
         * @return the success result
         */
        public static Result success(LoanId loanId, SarMoney disbursedAmount, LocalDate maturityDate) {
            Objects.requireNonNull(loanId, "LoanId cannot be null");
            Objects.requireNonNull(disbursedAmount, "DisbursedAmount cannot be null");
            Objects.requireNonNull(maturityDate, "MaturityDate cannot be null");
            return new Result(true, loanId, disbursedAmount, maturityDate, null);
        }

        /**
         * Create a failure result.
         *
         * @param errorMessage the error message
         * @return the failure result
         */
        public static Result failure(String errorMessage) {
            Objects.requireNonNull(errorMessage, "ErrorMessage cannot be null");
            return new Result(false, null, null, null, errorMessage);
        }
    }
}
