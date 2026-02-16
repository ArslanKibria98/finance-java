package com.ksa.financing.domain.port.in;

import com.ksa.financing.domain.enums.ShariaStructure;
import com.ksa.financing.domain.valueobject.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Input port (use case) for creating a new loan application.
 * <p>
 * This use case handles the creation of a new loan application with all
 * necessary details including customer information, product selection,
 * and requested financing terms.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public interface CreateLoanUseCase {

    /**
     * Execute the create loan use case.
     *
     * @param command the create loan command
     * @return the result containing the created loan ID or failure information
     */
    Result execute(CreateLoanCommand command);

    /**
     * Command for creating a new loan application.
     *
     * @param customerId the customer applying for the loan
     * @param productId the product being applied for
     * @param requestedAmount the amount requested by the customer
     * @param tenure the requested loan tenure
     * @param profitRate the applicable profit rate
     * @param shariaStructure the Sharia-compliant structure
     * @param bookingDate the date the application is booked
     * @param purpose the purpose of the loan
     */
    record CreateLoanCommand(
            @NotNull CustomerId customerId,
            @NotNull ProductId productId,
            @NotNull SarMoney requestedAmount,
            @NotNull Tenure tenure,
            @NotNull ProfitRate profitRate,
            @NotNull ShariaStructure shariaStructure,
            @NotNull LocalDate bookingDate,
            String purpose
    ) {
        /**
         * Canonical constructor with validation.
         */
        public CreateLoanCommand {
            Objects.requireNonNull(customerId, "CustomerId cannot be null");
            Objects.requireNonNull(productId, "ProductId cannot be null");
            Objects.requireNonNull(requestedAmount, "RequestedAmount cannot be null");
            Objects.requireNonNull(tenure, "Tenure cannot be null");
            Objects.requireNonNull(profitRate, "ProfitRate cannot be null");
            Objects.requireNonNull(shariaStructure, "ShariaStructure cannot be null");
            Objects.requireNonNull(bookingDate, "BookingDate cannot be null");

            if (!requestedAmount.isPositive()) {
                throw new IllegalArgumentException("RequestedAmount must be positive");
            }
        }
    }

    /**
     * Result of the create loan use case.
     *
     * @param success true if loan was created successfully
     * @param loanId the created loan ID (present if successful)
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
         * @param loanId the created loan ID
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
