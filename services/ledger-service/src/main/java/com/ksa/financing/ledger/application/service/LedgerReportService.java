package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.LedgerReportResponse;
import com.ksa.financing.ledger.application.dto.LedgerReportResponse.AccountLedger;
import com.ksa.financing.ledger.application.dto.LedgerReportResponse.LedgerMovement;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.ledger.infrastructure.persistence.entity.AccountJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalLineJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaAccountRepository;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaJournalLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generates the Ledger (General Ledger) Report — per-account T-account view with
 * opening balance, chronological debits/credits, running balance, and closing balance.
 *
 * If accountCode / accountId is provided, returns a single-account ledger.
 * Otherwise returns the ledger for every account with activity in the date range.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LedgerReportService {

    private static final String LEDGER_CURRENCY = "SAR";

    private final JpaAccountRepository accountRepository;
    private final JpaJournalLineRepository journalLineRepository;

    public LedgerReportResponse generate(UUID tenantId,
                                         LocalDate fromDate,
                                         LocalDate toDate,
                                         String accountCode,
                                         UUID accountId) {
        log.info("Generating ledger report: tenant={}, from={}, to={}, accountCode={}, accountId={}",
                tenantId, fromDate, toDate, accountCode, accountId);

        List<AccountJpaEntity> accounts = resolveAccounts(tenantId, accountCode, accountId);
        List<AccountLedger> ledgers = new ArrayList<>();

        for (AccountJpaEntity account : accounts) {
            AccountLedger ledger = buildAccountLedger(tenantId, account, fromDate, toDate);
            if (!ledger.movements().isEmpty() || accountCode != null || accountId != null) {
                ledgers.add(ledger);
            }
        }

        return LedgerReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalAccounts(ledgers.size())
                .accounts(ledgers)
                .build();
    }

    private List<AccountJpaEntity> resolveAccounts(UUID tenantId, String accountCode, UUID accountId) {
        if (accountId != null) {
            AccountJpaEntity account = accountRepository.findByTenantIdAndId(tenantId, accountId)
                    .orElseThrow(() -> NotFoundException.forEntity("Account", accountId.toString()));
            return List.of(account);
        }
        if (accountCode != null && !accountCode.isBlank()) {
            AccountJpaEntity account = accountRepository.findByTenantIdAndAccountCode(tenantId, accountCode)
                    .orElseThrow(() -> NotFoundException.forEntity("Account", accountCode));
            return List.of(account);
        }
        return accountRepository.findAllByTenantId(tenantId);
    }

    private AccountLedger buildAccountLedger(UUID tenantId,
                                             AccountJpaEntity account,
                                             LocalDate fromDate,
                                             LocalDate toDate) {
        BigDecimal openingBalance = computeOpeningBalance(tenantId, account, fromDate);
        List<JournalLineJpaEntity> lines = journalLineRepository
                .findByAccountInDateRange(tenantId, account.getId(), fromDate, toDate);

        boolean debitNormal = isDebitNormal(account);
        List<LedgerMovement> movements = new ArrayList<>(lines.size());
        BigDecimal running = openingBalance;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (JournalLineJpaEntity line : lines) {
            var entry = line.getJournalEntry();
            BigDecimal debit = nullSafe(line.getDebitAmount());
            BigDecimal credit = nullSafe(line.getCreditAmount());
            totalDebits = totalDebits.add(debit);
            totalCredits = totalCredits.add(credit);

            running = debitNormal
                    ? running.add(debit).subtract(credit)
                    : running.add(credit).subtract(debit);

            movements.add(LedgerMovement.builder()
                    .entryDate(entry.getEntryDate())
                    .entryId(entry.getId())
                    .voucherNumber(entry.getEntryNumber())
                    .referenceType(entry.getReferenceType())
                    .referenceId(entry.getReferenceId())
                    .transactionType(entry.getTransactionType())
                    .description(entry.getDescription())
                    .debitAmount(debit)
                    .creditAmount(credit)
                    .runningBalance(running)
                    .status(entry.getStatus())
                    .build());
        }

        return AccountLedger.builder()
                .accountId(account.getId())
                .accountCode(account.getAccountCode())
                .accountName(account.getAccountName())
                .accountType(account.getAccountType() != null ? account.getAccountType().name() : null)
                .currency(LEDGER_CURRENCY)
                .openingBalance(openingBalance)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .closingBalance(running)
                .movements(movements)
                .build();
    }

    private BigDecimal computeOpeningBalance(UUID tenantId, AccountJpaEntity account, LocalDate fromDate) {
        Object[] result = journalLineRepository.sumDebitsCreditsBefore(tenantId, account.getId(), fromDate);
        if (result == null || result.length < 2) {
            return BigDecimal.ZERO;
        }

        // Hibernate may return Object[] or Object[][] — defensive handling:
        Object[] row = result;
        if (result.length == 1 && result[0] instanceof Object[] inner) {
            row = inner;
        }

        BigDecimal debits = toBigDecimal(row[0]);
        BigDecimal credits = toBigDecimal(row[1]);

        return isDebitNormal(account)
                ? debits.subtract(credits)
                : credits.subtract(debits);
    }

    private boolean isDebitNormal(AccountJpaEntity account) {
        if (account.getAccountType() == null) {
            return true;
        }
        return switch (account.getAccountType()) {
            case ASSET, EXPENSE -> true;
            case LIABILITY, EQUITY, INCOME, OFF_BALANCE -> false;
        };
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
