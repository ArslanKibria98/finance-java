package com.ksa.financing.domain.port.in;

import com.ksa.financing.domain.enums.LoanStatus;
import com.ksa.financing.domain.enums.ShariaStructure;
import com.ksa.financing.domain.valueobject.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Input port (query) for retrieving loan details.
 * <p>
 * This query retrieves comprehensive information about a loan including
 * its current status, outstanding balances, and all key attributes.
 * This follows the CQRS pattern for read operations.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public interface GetLoanDetailsQuery {

    /**
     * Execute the get loan details query.
     *
     * @param query the loan details query
     * @return the result containing loan details or failure information
     */
    Result execute(Query query);

    /**
     * Query for retrieving loan details.
     *
     * @param loanId the ID of the loan to retrieve
     */
    record Query(
            @NotNull LoanId loanId
    ) {
        /**
         * Canonical constructor with validation.
         */
        public Query {
            Objects.requireNonNull(loanId, "LoanId cannot be null");
        }
    }

    /**
     * Result of the get loan details query.
     *
     * @param success true if loan details were retrieved successfully
     * @param loanDetails the loan details DTO (present if successful)
     * @param errorMessage the error message (present if failed)
     */
    record Result(
            boolean success,
            LoanDetailsDTO loanDetails,
            String errorMessage
    ) {
        /**
         * Create a successful result.
         *
         * @param loanDetails the loan details
         * @return the success result
         */
        public static Result success(LoanDetailsDTO loanDetails) {
            Objects.requireNonNull(loanDetails, "LoanDetails cannot be null");
            return new Result(true, loanDetails, null);
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

    /**
     * Data Transfer Object for loan details.
     *
     * @param loanId the loan identifier
     * @param contractId the contract identifier
     * @param customerId the customer identifier
     * @param productId the product identifier
     * @param status the current loan status
     * @param shariaStructure the Sharia structure
     * @param principal the original principal amount
     * @param profitAmount the total profit amount
     * @param totalAmount the total amount (principal + profit)
     * @param outstandingPrincipal the remaining principal balance
     * @param outstandingProfit the remaining profit balance
     * @param profitRate the profit rate
     * @param tenure the loan tenure
     * @param bookingDate the booking date
     * @param disbursementDate the disbursement date
     * @param maturityDate the maturity date
     * @param currentDPD the current days past due
     * @param maxDPD the maximum days past due ever recorded
     */
    record LoanDetailsDTO(
            @NotNull LoanId loanId,
            @NotNull ContractId contractId,
            @NotNull CustomerId customerId,
            @NotNull ProductId productId,
            @NotNull LoanStatus status,
            @NotNull ShariaStructure shariaStructure,
            @NotNull SarMoney principal,
            @NotNull SarMoney profitAmount,
            @NotNull SarMoney totalAmount,
            @NotNull SarMoney outstandingPrincipal,
            @NotNull SarMoney outstandingProfit,
            @NotNull ProfitRate profitRate,
            @NotNull Tenure tenure,
            @NotNull LocalDate bookingDate,
            LocalDate disbursementDate,
            LocalDate maturityDate,
            int currentDPD,
            int maxDPD
    ) {
        /**
         * Canonical constructor with validation.
         */
        public LoanDetailsDTO {
            Objects.requireNonNull(loanId, "LoanId cannot be null");
            Objects.requireNonNull(contractId, "ContractId cannot be null");
            Objects.requireNonNull(customerId, "CustomerId cannot be null");
            Objects.requireNonNull(productId, "ProductId cannot be null");
            Objects.requireNonNull(status, "Status cannot be null");
            Objects.requireNonNull(shariaStructure, "ShariaStructure cannot be null");
            Objects.requireNonNull(principal, "Principal cannot be null");
            Objects.requireNonNull(profitAmount, "ProfitAmount cannot be null");
            Objects.requireNonNull(totalAmount, "TotalAmount cannot be null");
            Objects.requireNonNull(outstandingPrincipal, "OutstandingPrincipal cannot be null");
            Objects.requireNonNull(outstandingProfit, "OutstandingProfit cannot be null");
            Objects.requireNonNull(profitRate, "ProfitRate cannot be null");
            Objects.requireNonNull(tenure, "Tenure cannot be null");
            Objects.requireNonNull(bookingDate, "BookingDate cannot be null");
        }
    }
}
