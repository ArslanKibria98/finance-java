package com.ksa.financing.ledger.infrastructure.persistence.mapper;

import com.ksa.financing.ledger.domain.model.AccountId;
import com.ksa.financing.ledger.domain.model.EntryStatus;
import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;
import com.ksa.financing.ledger.domain.model.JournalEntryId;
import com.ksa.financing.ledger.domain.model.JournalLine;
import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalEntryJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalLineJpaEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Maps between JournalEntryAggregate (domain) and JournalEntryJpaEntity (persistence).
 * Keeps domain and persistence models completely decoupled.
 */
@Component
public class JournalEntryPersistenceMapper {

    public JournalEntryJpaEntity toJpaEntity(JournalEntryAggregate domain, String idempotencyKey) {
        JournalEntryJpaEntity entity = JournalEntryJpaEntity.builder()
                .id(domain.getId().value())
                .tenantId(domain.getTenantId())
                .entryNumber(domain.getEntryNumber())
                .referenceType(domain.getReferenceType())
                .referenceId(domain.getReferenceId())
                .transactionType(domain.getTransactionType())
                .entryDate(domain.getEntryDate())
                .valueDate(domain.getValueDate())
                .currency(domain.getCurrency())
                .description(domain.getDescription())
                .totalDebit(domain.getTotalDebit())
                .totalCredit(domain.getTotalCredit())
                .status(domain.getStatus().name())
                .isReversal(domain.isReversal())
                .originalEntryId(domain.getOriginalEntryId() != null ? domain.getOriginalEntryId().value() : null)
                .reversedByEntryId(domain.getReversedByEntryId() != null ? domain.getReversedByEntryId().value() : null)
                .reversalReason(domain.getReversalReason())
                .fineractSynced(domain.isFineractSynced())
                .fineractTransactionId(domain.getFineractTransactionId())
                .idempotencyKey(idempotencyKey)
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .lines(new ArrayList<>())
                .build();

        // Map lines
        int lineNum = 1;
        for (JournalLine line : domain.getLines()) {
            JournalLineJpaEntity lineEntity = JournalLineJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(domain.getTenantId())
                    .journalEntry(entity)
                    .accountId(line.accountId().value())
                    .lineNumber(lineNum++)
                    .debitAmount(line.debitAmount() != null ? line.debitAmount() : BigDecimal.ZERO)
                    .creditAmount(line.creditAmount() != null ? line.creditAmount() : BigDecimal.ZERO)
                    .description(line.description())
                    .build();
            entity.getLines().add(lineEntity);
        }

        return entity;
    }

    public JournalEntryAggregate toDomain(JournalEntryJpaEntity entity) {
        List<JournalLine> lines = entity.getLines().stream()
                .map(this::toJournalLine)
                .toList();

        return JournalEntryAggregate.builder()
                .id(JournalEntryId.of(entity.getId()))
                .tenantId(entity.getTenantId())
                .entryNumber(entity.getEntryNumber())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .transactionType(entity.getTransactionType())
                .entryDate(entity.getEntryDate())
                .valueDate(entity.getValueDate())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .lines(lines)
                .totalDebit(entity.getTotalDebit())
                .totalCredit(entity.getTotalCredit())
                .status(EntryStatus.valueOf(entity.getStatus()))
                .isReversal(entity.isReversal())
                .originalEntryId(entity.getOriginalEntryId() != null
                        ? JournalEntryId.of(entity.getOriginalEntryId()) : null)
                .reversedByEntryId(entity.getReversedByEntryId() != null
                        ? JournalEntryId.of(entity.getReversedByEntryId()) : null)
                .reversalReason(entity.getReversalReason())
                .fineractSynced(entity.isFineractSynced())
                .fineractTransactionId(entity.getFineractTransactionId())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private JournalLine toJournalLine(JournalLineJpaEntity entity) {
        return new JournalLine(
                AccountId.of(entity.getAccountId()),
                entity.getDebitAmount() != null ? entity.getDebitAmount() : BigDecimal.ZERO,
                entity.getCreditAmount() != null ? entity.getCreditAmount() : BigDecimal.ZERO,
                entity.getDescription(),
                entity.getLineNumber()
        );
    }
}
