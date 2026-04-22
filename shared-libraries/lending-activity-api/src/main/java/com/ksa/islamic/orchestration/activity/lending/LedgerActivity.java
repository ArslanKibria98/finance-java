package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;

/**
 * Temporal activity contract for GL journal entries via ledger-service.
 *
 * Ledger-service is the ONLY bridge to Fineract GL.
 * All financial transactions (disbursement, repayment, settlement) must call
 * this activity — zero direct Fineract GL calls from other services.
 *
 * Implemented in lending-service/adapter/temporal/activity/LedgerActivityImpl.java.
 * The implementation calls ledger-service REST API (POST /api/v1/journal-entries).
 * Ledger-service then handles Fineract GL sync asynchronously.
 */
@ActivityInterface
public interface LedgerActivity {

    /**
     * Post GL journal entry for a loan disbursement.
     *
     * Entries:
     *   Dr. Loans Receivable   (amount)
     *   Cr. Bank / Cash        (amount)
     *
     * Idempotent: same idempotencyKey always returns the same result.
     */
    @ActivityMethod
    GlEntryResult postDisbursementGlEntry(DisbursementGlInput input);

    /**
     * Post GL journal entry for a loan repayment.
     *
     * Entries:
     *   Dr. Bank / Cash        (amount)
     *   Cr. Loans Receivable   (amount)
     *
     * Idempotent: same idempotencyKey always returns the same result.
     */
    @ActivityMethod
    GlEntryResult postRepaymentGlEntry(RepaymentGlInput input);

    /**
     * Post GL write-off and profit-waiver entries for loan restructuring.
     *
     * Entries (write-off):
     *   Dr. Provision for Bad Debts   (writeOffAmount)
     *   Cr. Loans Receivable          (writeOffAmount)
     *
     * Entries (profit waiver):
     *   Dr. Unearned/Deferred Income  (profitWaiverAmount)
     *   Cr. Loans Receivable Profit   (profitWaiverAmount)
     *
     * Per Blueprint 17 § 4.4 (restructuring_with_write_off).
     * Idempotent: same idempotencyKey always returns the same result.
     */
    @ActivityMethod
    GlEntryResult postRestructuringWriteOffEntry(RestructuringGlInput input);

    /**
     * Post GL journal entry for a loan settlement (early or final).
     *
     * Entries:
     *   Dr. Bank / Cash                    (settlementAmount)
     *   Dr. Unearned/Deferred Income (Ibra) (ibraAmount)
     *   Cr. Loans Receivable               (settlementAmount + ibraAmount)
     *
     * Per Blueprint 17 § 3.1 (early_settlement_with_ibra).
     * Idempotent: same idempotencyKey always returns the same result.
     */
    @ActivityMethod
    GlEntryResult postSettlementGlEntry(SettlementGlInput input);

    // ══════════ DTOs ══════════

    record SettlementGlInput(
            String tenantId,
            String loanId,
            String loanNumber,
            BigDecimal settlementAmount,
            BigDecimal ibraAmount,
            String idempotencyKey,
            String createdBy
    ) {}

    record DisbursementGlInput(
            String tenantId,
            String loanId,
            String loanNumber,
            BigDecimal amount,
            String idempotencyKey,
            String createdBy
    ) {}

    record RepaymentGlInput(
            String tenantId,
            String loanId,
            String loanNumber,
            BigDecimal amount,
            String idempotencyKey,
            String createdBy
    ) {}

    record RestructuringGlInput(
            String tenantId,
            String loanId,
            String loanNumber,
            BigDecimal writeOffAmount,
            BigDecimal profitWaiverAmount,
            String idempotencyKey,
            String createdBy
    ) {}

    record GlEntryResult(
            String entryNumber,
            String status,
            boolean posted
    ) {}
}
