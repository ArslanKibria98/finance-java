package com.ksa.financing.ledger.application.mapper;

import com.ksa.financing.ledger.application.dto.AccountResponse;
import com.ksa.financing.ledger.application.dto.JournalEntryResponse;
import com.ksa.financing.ledger.application.dto.ReconciliationReportResponse;
import com.ksa.financing.ledger.domain.model.AccountAggregate;
import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;
import com.ksa.financing.ledger.domain.model.JournalLine;
import com.ksa.financing.ledger.domain.port.in.RunReconciliationUseCase;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Application-layer mapper: domain models → response DTOs.
 * Manual mapping (no MapStruct) to avoid circular dependencies.
 */
@Component
public class JournalEntryMapper {

    public JournalEntryResponse toResponse(JournalEntryAggregate entry) {
        List<JournalEntryResponse.JournalLineResponse> lineResponses = entry.getLines().stream()
                .map(this::toLineResponse)
                .toList();

        return new JournalEntryResponse(
                entry.getId().value(),
                entry.getTenantId(),
                entry.getEntryNumber(),
                entry.getReferenceType(),
                entry.getReferenceId(),
                entry.getTransactionType(),
                entry.getEntryDate(),
                entry.getValueDate(),
                entry.getCurrency(),
                entry.getDescription(),
                entry.getTotalDebit(),
                entry.getTotalCredit(),
                entry.getStatus().name(),
                entry.isReversal(),
                entry.getOriginalEntryId() != null ? entry.getOriginalEntryId().value() : null,
                entry.isFineractSynced(),
                entry.getFineractTransactionId(),
                lineResponses,
                entry.getCreatedBy(),
                entry.getCreatedAt()
        );
    }

    private JournalEntryResponse.JournalLineResponse toLineResponse(JournalLine line) {
        return new JournalEntryResponse.JournalLineResponse(
                line.accountId().value().toString(),
                line.debitAmount(),
                line.creditAmount(),
                line.description(),
                line.lineNumber()
        );
    }

    public AccountResponse toAccountResponse(AccountAggregate account) {
        return new AccountResponse(
                account.getId().value(),
                account.getTenantId(),
                account.getAccountCode(),
                account.getAccountName(),
                account.getAccountNameAr(),
                account.getAccountType(),
                account.getParentAccountId() != null ? account.getParentAccountId().value() : null,
                account.getHierarchyLevel(),
                account.isHeader(),
                account.isManualEntriesAllowed(),
                account.getStatus().name(),
                account.getIban(),
                account.getFineractMappingId(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    public ReconciliationReportResponse toReconResponse(RunReconciliationUseCase.ReconciliationSummary summary) {
        List<ReconciliationReportResponse.AccountDiscrepancyDto> discrepancies = summary.discrepancies().stream()
                .map(d -> new ReconciliationReportResponse.AccountDiscrepancyDto(
                        d.accountId(),
                        d.accountCode(),
                        d.accountName(),
                        d.internalBalance(),
                        d.fineractBalance(),
                        d.variance()
                ))
                .toList();

        return new ReconciliationReportResponse(
                summary.tenantId(),
                summary.reconciliationDate(),
                summary.totalAccountsChecked(),
                summary.matchedAccounts(),
                summary.discrepancyAccounts(),
                discrepancies
        );
    }
}
