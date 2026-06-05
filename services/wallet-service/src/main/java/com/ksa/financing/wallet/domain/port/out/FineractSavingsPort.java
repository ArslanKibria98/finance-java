package com.ksa.financing.wallet.domain.port.out;

import java.math.BigDecimal;

/**
 * Output port for Fineract core banking savings account operations.
 * Each wallet maps to a Fineract savings account.
 * <p>
 * Wallet-service uses Fineract as the SINGLE SOURCE OF TRUTH for balance.
 * Balance reads + writes go directly to Fineract.
 */
public interface FineractSavingsPort {

    Long lookupClientByExternalId(String externalId);

    Long createClient(String externalId, String displayName);

    Long createSavings(Long fineractClientId, String walletNumber);

    void approveSavings(Long savingsId);

    void activateSavings(Long savingsId);

    void deleteSavings(Long savingsId);

    /** Fetch current account info (incl. balance + clientId) from Fineract. */
    SavingsAccountInfo getAccountInfo(Long savingsId);

    /** Deposit funds into a savings account. Returns Fineract transactionId. */
    Long deposit(Long savingsId, BigDecimal amount, String externalReference);

    /** Withdraw funds from a savings account. Returns Fineract transactionId. */
    Long withdraw(Long savingsId, BigDecimal amount, String externalReference);

    /** Place an amount hold (blocks it from availableBalance). Returns the hold transaction id. */
    Long hold(Long savingsId, BigDecimal amount, String externalReference);

    /** Release a previously held amount by its hold transaction id. */
    void releaseHold(Long savingsId, Long holdTransactionId, String externalReference);

    /**
     * Transfer between two savings accounts (account-to-account).
     * Returns the Fineract transfer resource id.
     */
    Long transferBetweenSavings(
            Long fromClientId, Long fromSavingsId,
            Long toClientId,   Long toSavingsId,
            BigDecimal amount, String description);

    /** Fetch all transactions for a savings account (newest first). */
    java.util.List<SavingsTransaction> getTransactions(Long savingsId);

    record SavingsTransaction(
            Long transactionId,
            String transactionType,    // Deposit / Withdrawal / Interest Posting / Fee etc.
            BigDecimal amount,
            BigDecimal runningBalance,
            String date,                // ISO yyyy-MM-dd
            boolean reversed,
            String paymentDetails
    ) {}

    record SavingsAccountInfo(
            Long savingsId,
            Long clientId,
            String externalId,
            String status,
            BigDecimal accountBalance,
            BigDecimal availableBalance,
            String currency
    ) {}
}
