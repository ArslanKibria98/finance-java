package com.ksa.financing.ledger.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;
import com.ksa.financing.ledger.domain.port.in.PostJournalEntryUseCase;
import com.ksa.financing.ledger.domain.port.out.EventPublisher;
import com.ksa.financing.ledger.domain.port.out.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: Post a balanced journal entry.
 * Idempotent — checks idempotency_keys before creating a new entry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostJournalEntryUseCaseImpl implements PostJournalEntryUseCase {

    private final JournalEntryRepository journalEntryRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public JournalEntryAggregate post(PostJournalEntryCommand command) {
        log.info("Posting journal entry for tenant={} reference={}/{} idempotencyKey={}",
                command.tenantId(), command.referenceType(), command.referenceId(),
                command.idempotencyKey());

        // Step 1: Idempotency guard — return cached result if already posted
        var existing = journalEntryRepository.findByIdempotencyKey(
                command.tenantId(), command.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Duplicate request detected for idempotencyKey={}. Returning cached entry={}",
                    command.idempotencyKey(), existing.get().getEntryNumber());
            return existing.get();
        }

        // Step 2: Generate entry number
        String entryNumber = journalEntryRepository.generateEntryNumber(
                command.tenantId(), command.entryDate());

        // Step 3: Create aggregate — domain validates balanced entry
        JournalEntryAggregate entry = JournalEntryAggregate.create(
                command.tenantId(),
                entryNumber,
                command.referenceType(),
                command.referenceId(),
                command.transactionType(),
                command.entryDate(),
                command.description(),
                command.lines(),
                command.createdBy()
        );

        // Step 4: Mark as POSTED
        entry.markPosted();

        // Step 5: Persist
        JournalEntryAggregate saved = journalEntryRepository.save(entry);
        log.info("Journal entry posted: entryNumber={} totalDebit={} status={}",
                saved.getEntryNumber(), saved.getTotalDebit(), saved.getStatus());

        // Step 6: Publish domain events (outbox pattern)
        eventPublisher.publishAll(saved.getUncommittedEvents());
        saved.markEventsAsCommitted();

        return saved;
    }
}
