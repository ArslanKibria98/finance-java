package com.ksa.financing.ledger.application.service;

import com.ksa.financing.ledger.application.dto.JournalVoucherReportResponse;
import com.ksa.financing.ledger.application.dto.JournalVoucherReportResponse.VoucherItem;
import com.ksa.financing.ledger.application.dto.JournalVoucherReportResponse.VoucherLine;
import com.ksa.financing.ledger.infrastructure.persistence.entity.AccountJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalEntryJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalLineJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaAccountRepository;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaJournalEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates the Journal Vouchers Report — lists all journal entries (vouchers)
 * posted within a date range, with optional filters by reference type and status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class JournalVoucherReportService {

    private final JpaJournalEntryRepository journalEntryRepository;
    private final JpaAccountRepository accountRepository;

    public JournalVoucherReportResponse generate(UUID tenantId,
                                                 LocalDate fromDate,
                                                 LocalDate toDate,
                                                 String referenceType,
                                                 String status) {
        log.info("Generating journal vouchers report: tenant={}, from={}, to={}, type={}, status={}",
                tenantId, fromDate, toDate, referenceType, status);

        List<JournalEntryJpaEntity> entries = journalEntryRepository.findForVouchersReport(
                tenantId, fromDate, toDate, referenceType, status);

        Map<UUID, AccountJpaEntity> accountCache = new HashMap<>();
        List<VoucherItem> vouchers = new ArrayList<>(entries.size());
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (JournalEntryJpaEntity entry : entries) {
            List<VoucherLine> lines = new ArrayList<>(entry.getLines().size());
            for (JournalLineJpaEntity line : entry.getLines()) {
                AccountJpaEntity account = accountCache.computeIfAbsent(
                        line.getAccountId(),
                        id -> accountRepository.findByTenantIdAndId(tenantId, id).orElse(null));

                lines.add(VoucherLine.builder()
                        .lineNumber(line.getLineNumber())
                        .accountCode(account != null ? account.getAccountCode() : null)
                        .accountName(account != null ? account.getAccountName() : null)
                        .description(line.getDescription())
                        .debitAmount(nullSafe(line.getDebitAmount()))
                        .creditAmount(nullSafe(line.getCreditAmount()))
                        .build());
            }

            BigDecimal entryDebit = nullSafe(entry.getTotalDebit());
            BigDecimal entryCredit = nullSafe(entry.getTotalCredit());
            totalDebits = totalDebits.add(entryDebit);
            totalCredits = totalCredits.add(entryCredit);

            vouchers.add(VoucherItem.builder()
                    .entryId(entry.getId())
                    .voucherNumber(entry.getEntryNumber())
                    .entryDate(entry.getEntryDate())
                    .valueDate(entry.getValueDate())
                    .referenceType(entry.getReferenceType())
                    .referenceId(entry.getReferenceId())
                    .transactionType(entry.getTransactionType())
                    .description(entry.getDescription())
                    .status(entry.getStatus())
                    .currency(entry.getCurrency())
                    .totalDebit(entryDebit)
                    .totalCredit(entryCredit)
                    .isReversal(entry.isReversal())
                    .fineractSynced(entry.isFineractSynced())
                    .lines(lines)
                    .build());
        }

        return JournalVoucherReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalVouchers(vouchers.size())
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .vouchers(vouchers)
                .build();
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
