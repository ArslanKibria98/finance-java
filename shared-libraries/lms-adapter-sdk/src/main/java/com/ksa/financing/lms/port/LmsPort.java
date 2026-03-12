package com.ksa.financing.lms.port;

import com.ksa.financing.lms.intent.*;
import com.ksa.financing.lms.dto.*;

import java.util.List;

/**
 * LMS Port - High-level abstraction for Loan Management System operations.
 * This interface provides intent-based methods that express WHAT needs to be done,
 * allowing adapters to determine HOW to implement them using specific CBS APIs.
 *
 * Implementation can be Apache Fineract, custom CBS, or any future LMS system.
 * The port ensures NO CBS-specific details leak to the domain layer.
 */
public interface LmsPort {

    /**
     * Creates a loan product definition in the CBS.
     * The adapter translates the domain-focused intent into CBS-specific API calls.
     *
     * @param intent The loan product creation intent
     * @return The created loan product result with CBS product ID
     */
    LoanProductResult createLoanProduct(LoanProductIntent intent);

    /**
     * Creates a loan account in the CBS based on the loan intent.
     * The adapter translates the domain-focused intent into CBS-specific API calls.
     *
     * @param intent The loan creation intent containing domain objects
     * @return The created loan account ID
     */
    LoanAccountId createLoanAccount(LoanIntent intent);

    /**
     * Approves a loan application based on approval decision.
     *
     * @param intent The approval intent with approver details
     * @return The approval result with updated status
     */
    ApprovalResult approveLoan(ApprovalIntent intent);

    /**
     * Disburses the loan amount to the customer.
     * Implements idempotency to prevent double disbursements.
     *
     * @param intent The disbursement details
     * @return The disbursement result with transaction details
     */
    DisbursementResult disburseLoan(DisbursementIntent intent);

    /**
     * Records a loan repayment from the customer.
     *
     * @param intent The repayment details
     * @return The repayment result with updated balance
     */
    RepaymentResult recordRepayment(RepaymentIntent intent);

    /**
     * Retrieves detailed information about a loan account.
     *
     * @param loanAccountId The loan account identifier
     * @return The complete loan details
     */
    LoanDetails getLoanDetails(LoanAccountId loanAccountId);

    /**
     * Gets the repayment schedule for a loan.
     *
     * @param loanAccountId The loan account identifier
     * @return List of scheduled installments
     */
    List<Installment> getRepaymentSchedule(LoanAccountId loanAccountId);

    /**
     * Processes early settlement with Ibra (profit waiver) calculation.
     * The domain calculates Ibra amount, CBS just processes the settlement.
     *
     * @param intent The early settlement intent with Ibra details
     * @return The settlement result
     */
    SettlementResult processEarlySettlement(SettlementIntent intent);

    /**
     * Updates the loan status (e.g., from APPROVED to ACTIVE).
     *
     * @param loanAccountId The loan account identifier
     * @param status The new loan status
     */
    void updateLoanStatus(LoanAccountId loanAccountId, LoanStatus status);

    /**
     * Reverses a transaction (for compensation in saga pattern).
     *
     * @param transactionId The transaction to reverse
     */
    void reverseTransaction(String transactionId);

    /**
     * Checks if the LMS is available and operational.
     *
     * @return true if the LMS is healthy
     */
    boolean isHealthy();
}