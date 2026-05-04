package com.ksa.financing.ledger.infrastructure.messaging;

import com.ksa.financing.ledger.domain.model.JournalLine;
import com.ksa.financing.ledger.domain.port.in.PostJournalEntryUseCase;
import com.ksa.financing.ledger.domain.port.in.PostJournalEntryUseCase.PostJournalEntryCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Helper that builds and posts journal entries from loan lifecycle events.
 * Uses standard GL account codes (see {@link LedgerAccountCodes}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlPostingService {

    private final PostJournalEntryUseCase postJournalEntry;
    private final LedgerAccountResolver accountResolver;

    /**
     * Post disbursement entry: Dr. Loans Receivable, Cr. Bank Account.
     */
    public void postDisbursement(UUID tenantId, UUID loanId, UUID customerId,
                                  BigDecimal amount, LocalDate entryDate,
                                  String idempotencyKey) {
        var loansReceivable = accountResolver.resolve(tenantId, LedgerAccountCodes.LOANS_RECEIVABLE);
        var bank = accountResolver.resolve(tenantId, LedgerAccountCodes.BANK_ACCOUNT);

        var lines = List.of(
                JournalLine.debit(loansReceivable, amount, "Loan disbursement — loanId=" + loanId, 1),
                JournalLine.credit(bank, amount, "Cash disbursed to customer=" + customerId, 2)
        );

        post(tenantId, entryDate, LedgerAccountCodes.REF_TYPE_LOAN, loanId,
                LedgerAccountCodes.TXN_TYPE_DISBURSEMENT,
                "Loan disbursed: " + amount + " to loanId=" + loanId,
                lines, idempotencyKey, customerId);
    }

    /**
     * Post repayment entry: Dr. Bank Account, Cr. Loans Receivable.
     */
    public void postRepayment(UUID tenantId, UUID loanId, UUID customerId, UUID paymentId,
                               BigDecimal amount, LocalDate entryDate,
                               String idempotencyKey) {
        var bank = accountResolver.resolve(tenantId, LedgerAccountCodes.BANK_ACCOUNT);
        var loansReceivable = accountResolver.resolve(tenantId, LedgerAccountCodes.LOANS_RECEIVABLE);

        var lines = List.of(
                JournalLine.debit(bank, amount, "Payment received — paymentId=" + paymentId, 1),
                JournalLine.credit(loansReceivable, amount, "Loan balance reduced — loanId=" + loanId, 2)
        );

        post(tenantId, entryDate, LedgerAccountCodes.REF_TYPE_PAYMENT, paymentId,
                LedgerAccountCodes.TXN_TYPE_REPAYMENT,
                "Loan repayment: " + amount + " on loanId=" + loanId,
                lines, idempotencyKey, customerId);
    }

    /**
     * Post early-settlement entry: Dr. Bank (amount), Dr. Profit Income reversal (ibra),
     * Cr. Loans Receivable (amount + ibra).
     * Ibra represents forgiven profit; reduces profit income.
     */
    public void postSettlement(UUID tenantId, UUID loanId, UUID customerId,
                                UUID settlementId, BigDecimal amountPaid, BigDecimal ibraAmount,
                                LocalDate entryDate, String idempotencyKey) {
        var bank = accountResolver.resolve(tenantId, LedgerAccountCodes.BANK_ACCOUNT);
        var loansReceivable = accountResolver.resolve(tenantId, LedgerAccountCodes.LOANS_RECEIVABLE);
        var profitIncome = accountResolver.resolve(tenantId, LedgerAccountCodes.PROFIT_INCOME);

        var ibra = ibraAmount == null ? BigDecimal.ZERO : ibraAmount;
        var totalCleared = amountPaid.add(ibra);

        var linesBuilder = new java.util.ArrayList<JournalLine>();
        linesBuilder.add(JournalLine.debit(bank, amountPaid,
                "Settlement cash received — settlementId=" + settlementId, 1));
        if (ibra.compareTo(BigDecimal.ZERO) > 0) {
            linesBuilder.add(JournalLine.debit(profitIncome, ibra,
                    "Ibra (forgiven profit) on early settlement", 2));
        }
        linesBuilder.add(JournalLine.credit(loansReceivable, totalCleared,
                "Loan fully settled — loanId=" + loanId, linesBuilder.size() + 1));

        post(tenantId, entryDate, LedgerAccountCodes.REF_TYPE_SETTLEMENT, settlementId,
                LedgerAccountCodes.TXN_TYPE_SETTLEMENT,
                "Early settlement: paid=" + amountPaid + " ibra=" + ibra + " loanId=" + loanId,
                linesBuilder, idempotencyKey, customerId);
    }

    /**
     * Post provision entry on overdue: Dr. Provision Expense, Cr. Loans Receivable (contra).
     * Conservative treatment; can be reversed once overdue is cleared.
     */
    public void postOverdueProvision(UUID tenantId, UUID loanId, UUID customerId,
                                      BigDecimal provisionAmount, LocalDate entryDate,
                                      String idempotencyKey) {
        var provision = accountResolver.resolve(tenantId, LedgerAccountCodes.PROVISION_FOR_BAD_DEBTS);
        var loansReceivable = accountResolver.resolve(tenantId, LedgerAccountCodes.LOANS_RECEIVABLE);

        var lines = List.of(
                JournalLine.debit(provision, provisionAmount,
                        "Provision for overdue loanId=" + loanId, 1),
                JournalLine.credit(loansReceivable, provisionAmount,
                        "Allowance against loanId=" + loanId, 2)
        );

        post(tenantId, entryDate, LedgerAccountCodes.REF_TYPE_OVERDUE, loanId,
                LedgerAccountCodes.TXN_TYPE_PROVISION,
                "Overdue provision: " + provisionAmount + " loanId=" + loanId,
                lines, idempotencyKey, customerId);
    }

    private void post(UUID tenantId, LocalDate entryDate, String referenceType, UUID referenceId,
                      String transactionType, String description, List<JournalLine> lines,
                      String idempotencyKey, UUID createdBy) {
        var command = new PostJournalEntryCommand(
                tenantId, entryDate, referenceType, referenceId, transactionType,
                description, lines, idempotencyKey, createdBy
        );
        try {
            var entry = postJournalEntry.post(command);
            log.info("Auto-posted journal entry: {} ref={}/{} amount={}",
                    entry.getEntryNumber(), referenceType, referenceId, entry.getTotalDebit());
        } catch (Exception e) {
            log.error("Failed to post auto journal entry for {}/{} key={}: {}",
                    referenceType, referenceId, idempotencyKey, e.getMessage(), e);
            throw e;
        }
    }
}
