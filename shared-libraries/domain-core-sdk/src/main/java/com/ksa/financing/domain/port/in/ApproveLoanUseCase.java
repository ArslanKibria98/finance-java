package com.ksa.financing.domain.port.in;

import com.ksa.financing.domain.valueobject.LoanId;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * Input port (use case) for approving a loan application.
 * <p>
 * This use case handles the approval of a pending loan application after
 * all underwriting checks have passed. The loan becomes ready for disbursement.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public interface ApproveLoanUseCase {

    /**
     * Execute the approve loan use case.
     *
     * @param command the approve loan command
     * @return the result indicating success or failure
     */
    Result execute(ApproveLoanCommand command);

    /**
     * Command for approving a loan application.
     *
     * @param loanId the ID of the loan to approve
     * @param approvedBy the user/system approving the loan
     * @param approvalNotes optional notes about the approval
     */
    record ApproveLoanCommand(
            @NotNull LoanId loanId,
            @NotNull String approvedBy,
            String approvalNotes
    ) {
        /**
         * Canonical constructor with validation.
         */
        public ApproveLoanCommand {
            Objects.requireNonNull(loanId, "LoanId cannot be null");
            Objects.requireNonNull(approvedBy, "ApprovedBy cannot be null");

            if (approvedBy.trim().isEmpty()) {
                throw new IllegalArgumentException("ApprovedBy cannot be empty");
            }
        }
    }

    /**
     * Result of the approve loan use case.
     *
     * @param success true if loan was approved successfully
     * @param loanId the approved loan ID
     * @param errorMessage the error message (present if failed)
     */
    record Result(
            boolean success,
            LoanId loanId,
            String errorMessage
    ) {
        /**
         * Create a successful result.
         *
         * @param loanId the approved loan ID
         * @return the success result
         */
        public static Result success(LoanId loanId) {
            Objects.requireNonNull(loanId, "LoanId cannot be null");
            return new Result(true, loanId, null);
        }

        /**
         * Create a failure result.
         *
         * @param errorMessage the error message
         * @return the failure result
         */
        public static Result failure(String errorMessage) {
            Objects.requireNonNull(errorMessage, "ErrorMessage cannot be null");
            return new Result(false, null, errorMessage);
        }
    }
}
