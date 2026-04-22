package com.ksa.financing.ledger.unit.application;

import com.ksa.financing.ledger.application.usecase.PostJournalEntryUseCaseImpl;
import com.ksa.financing.ledger.domain.model.AccountId;
import com.ksa.financing.ledger.domain.model.EntryStatus;
import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;
import com.ksa.financing.ledger.domain.model.JournalEntryId;
import com.ksa.financing.ledger.domain.model.JournalLine;
import com.ksa.financing.ledger.domain.port.in.PostJournalEntryUseCase;
import com.ksa.financing.ledger.domain.port.out.EventPublisher;
import com.ksa.financing.ledger.domain.port.out.JournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Use case unit tests — all ports mocked, no Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostJournalEntryUseCase Tests")
class PostJournalEntryUseCaseTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PostJournalEntryUseCaseImpl useCase;

    private UUID tenantId;
    private UUID referenceId;
    private UUID userId;
    private List<JournalLine> balancedLines;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        referenceId = UUID.randomUUID();
        userId = UUID.randomUUID();

        AccountId accountA = AccountId.generate();
        AccountId accountB = AccountId.generate();

        balancedLines = List.of(
                JournalLine.debit(accountA, new BigDecimal("1000.000000"), "Murabaha debit", 1),
                JournalLine.credit(accountB, new BigDecimal("1000.000000"), "Murabaha credit", 2)
        );
    }

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should post a new journal entry successfully")
    void shouldPostNewJournalEntry() {
        // Given
        String idempotencyKey = "LOAN-" + UUID.randomUUID();
        PostJournalEntryUseCase.PostJournalEntryCommand command =
                new PostJournalEntryUseCase.PostJournalEntryCommand(
                        tenantId, LocalDate.now(), "LOAN", referenceId,
                        "DISBURSEMENT", "Murabaha disbursement", balancedLines,
                        idempotencyKey, userId
                );

        // No existing entry (not a duplicate)
        when(journalEntryRepository.findByIdempotencyKey(tenantId, idempotencyKey))
                .thenReturn(Optional.empty());

        when(journalEntryRepository.generateEntryNumber(eq(tenantId), any(LocalDate.class)))
                .thenReturn("JE-202601-000001");

        // Stub save to return the domain entry
        when(journalEntryRepository.save(any(JournalEntryAggregate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        JournalEntryAggregate result = useCase.post(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEntryNumber()).isEqualTo("JE-202601-000001");
        assertThat(result.getStatus()).isEqualTo(EntryStatus.POSTED);
        assertThat(result.getTotalDebit()).isEqualByComparingTo(new BigDecimal("1000.000000"));

        verify(journalEntryRepository).findByIdempotencyKey(tenantId, idempotencyKey);
        verify(journalEntryRepository).generateEntryNumber(eq(tenantId), any(LocalDate.class));
        verify(journalEntryRepository).save(any(JournalEntryAggregate.class));
        verify(eventPublisher).publishAll(any());
    }

    // -----------------------------------------------------------------------
    // Idempotency guard
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should return cached entry for duplicate idempotency key")
    void shouldReturnCachedEntryForDuplicate() {
        // Given
        String idempotencyKey = "LOAN-DUPLICATE-123";
        PostJournalEntryUseCase.PostJournalEntryCommand command =
                new PostJournalEntryUseCase.PostJournalEntryCommand(
                        tenantId, LocalDate.now(), "LOAN", referenceId,
                        "DISBURSEMENT", "Duplicate request", balancedLines,
                        idempotencyKey, userId
                );

        JournalEntryAggregate existing = buildExistingEntry();
        when(journalEntryRepository.findByIdempotencyKey(tenantId, idempotencyKey))
                .thenReturn(Optional.of(existing));

        // When
        JournalEntryAggregate result = useCase.post(command);

        // Then — returns cached, does NOT save again
        assertThat(result).isEqualTo(existing);
        verify(journalEntryRepository, never()).save(any());
        verify(journalEntryRepository, never()).generateEntryNumber(any(), any());
        verify(eventPublisher, never()).publishAll(any());
    }

    // -----------------------------------------------------------------------
    // Domain validation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should throw IllegalArgumentException for null tenantId in command")
    void shouldRejectNullTenantId() {
        assertThatThrownBy(() -> new PostJournalEntryUseCase.PostJournalEntryCommand(
                null, LocalDate.now(), "LOAN", referenceId,
                "TEST", "Desc", balancedLines, "KEY-001", userId
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TenantId");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for blank idempotency key")
    void shouldRejectBlankIdempotencyKey() {
        assertThatThrownBy(() -> new PostJournalEntryUseCase.PostJournalEntryCommand(
                tenantId, LocalDate.now(), "LOAN", referenceId,
                "TEST", "Desc", balancedLines, "", userId
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IdempotencyKey");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for empty lines list")
    void shouldRejectEmptyLines() {
        assertThatThrownBy(() -> new PostJournalEntryUseCase.PostJournalEntryCommand(
                tenantId, LocalDate.now(), "LOAN", referenceId,
                "TEST", "Desc", List.of(), "KEY-002", userId
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 journal lines");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private JournalEntryAggregate buildExistingEntry() {
        return JournalEntryAggregate.reconstitute(
                JournalEntryAggregate.builder()
                        .id(JournalEntryId.generate())
                        .tenantId(tenantId)
                        .entryNumber("JE-202601-000001")
                        .referenceType("LOAN")
                        .referenceId(referenceId)
                        .transactionType("DISBURSEMENT")
                        .entryDate(LocalDate.now())
                        .valueDate(LocalDate.now())
                        .currency("SAR")
                        .description("Cached entry")
                        .lines(new ArrayList<>(balancedLines))
                        .totalDebit(new BigDecimal("1000.000000"))
                        .totalCredit(new BigDecimal("1000.000000"))
                        .status(EntryStatus.POSTED)
                        .fineractSynced(false)
                        .createdBy(userId)
                        .createdAt(LocalDateTime.now())
        );
    }
}
