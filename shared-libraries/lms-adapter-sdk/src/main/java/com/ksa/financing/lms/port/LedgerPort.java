package com.ksa.financing.lms.port;

import com.ksa.financing.lms.intent.JournalIntent;
import com.ksa.financing.lms.dto.AccountBalance;
import com.ksa.financing.lms.dto.GlAccount;
import com.ksa.financing.lms.dto.JournalEntry;
import java.time.LocalDate;
import java.util.List;

/**
 * Ledger Port - Abstraction for General Ledger operations.
 * Provides accounting and bookkeeping functionality independent of the CBS implementation.
 */
public interface LedgerPort {

    /**
     * Creates a journal entry in the general ledger.
     * Used for recording financial transactions with double-entry bookkeeping.
     *
     * @param intent The journal entry intent with debit/credit details
     * @return The created journal entry with transaction ID
     */
    JournalEntry createJournalEntry(JournalIntent intent);

    /**
     * Retrieves the balance of a GL account.
     *
     * @param glAccount The general ledger account
     * @return The current account balance
     */
    AccountBalance getAccountBalance(GlAccount glAccount);

    /**
     * Gets the balance of a GL account as of a specific date.
     *
     * @param glAccount The general ledger account
     * @param asOfDate The date for which to retrieve the balance
     * @return The account balance as of the specified date
     */
    AccountBalance getAccountBalance(GlAccount glAccount, LocalDate asOfDate);

    /**
     * Retrieves journal entries for an account within a date range.
     *
     * @param glAccount The general ledger account
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @return List of journal entries
     */
    List<JournalEntry> getJournalEntries(GlAccount glAccount, LocalDate startDate, LocalDate endDate);

    /**
     * Posts accrual entries for interest/profit recognition.
     * Used for Islamic finance profit accrual calculations.
     *
     * @param loanAccountId The loan account for accrual
     * @param accrualAmount The amount to accrue
     * @param accrualDate The date of accrual
     */
    void postAccrualEntry(String loanAccountId, double accrualAmount, LocalDate accrualDate);

    /**
     * Reverses a journal entry (for error correction or compensation).
     *
     * @param journalEntryId The journal entry to reverse
     * @param reason The reason for reversal
     * @return The reversal journal entry
     */
    JournalEntry reverseJournalEntry(String journalEntryId, String reason);

    /**
     * Performs trial balance check to ensure debits equal credits.
     *
     * @param asOfDate The date for trial balance
     * @return true if the trial balance is balanced
     */
    boolean isTrialBalanceBalanced(LocalDate asOfDate);
}