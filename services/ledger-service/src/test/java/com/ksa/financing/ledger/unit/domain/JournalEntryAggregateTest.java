package com.ksa.financing.ledger.unit.domain;

import com.ksa.financing.ledger.domain.model.AccountId;
import com.ksa.financing.ledger.domain.model.EntryStatus;
import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;
import com.ksa.financing.ledger.domain.model.JournalEntryId;
import com.ksa.financing.ledger.domain.model.JournalLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JournalEntryAggregate — covers domain invariants at >= 90%.
 * Zero infrastructure dependencies — pure Java.
 */
@DisplayName("JournalEntryAggregate Domain Tests")
class JournalEntryAggregateTest {

    private UUID tenantId;
    private UUID referenceId;
    private UUID userId;
    private AccountId assetAccountId;
    private AccountId liabilityAccountId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        referenceId = UUID.randomUUID();
        userId = UUID.randomUUID();
        assetAccountId = AccountId.generate();
        liabilityAccountId = AccountId.generate();
    }

    // -----------------------------------------------------------------------
    // Happy path — creation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should create a balanced journal entry successfully")
    void shouldCreateBalancedJournalEntry() {
        // Given
        List<JournalLine> lines = List.of(
                JournalLine.debit(assetAccountId, new BigDecimal("1000.00"), "Debit line", 1),
                JournalLine.credit(liabilityAccountId, new BigDecimal("1000.00"), "Credit line", 2)
        );

        // When
        JournalEntryAggregate entry = JournalEntryAggregate.create(
                tenantId, "JE-202601-000001", "LOAN", referenceId,
                "DISBURSEMENT", LocalDate.now(), "Test journal entry", lines, userId
        );

        // Then
        assertThat(entry).isNotNull();
        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getTenantId()).isEqualTo(tenantId);
        assertThat(entry.getEntryNumber()).isEqualTo("JE-202601-000001");
        assertThat(entry.getStatus()).isEqualTo(EntryStatus.PENDING);
        assertThat(entry.getTotalDebit()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(entry.getTotalCredit()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(entry.getLines()).hasSize(2);
        assertThat(entry.isFineractSynced()).isFalse();
        assertThat(entry.getUncommittedEvents()).hasSize(1);
        assertThat(entry.getUncommittedEvents().get(0))
                .isInstanceOf(JournalEntryAggregate.JournalEntryPosted.class);
    }

    @Test
    @DisplayName("Should create entry with multiple debit and credit lines")
    void shouldCreateEntryWithMultipleLines() {
        // Given
        AccountId account3 = AccountId.generate();
        List<JournalLine> lines = List.of(
                JournalLine.debit(assetAccountId, new BigDecimal("600.00"), "Debit 1", 1),
                JournalLine.debit(account3, new BigDecimal("400.00"), "Debit 2", 2),
                JournalLine.credit(liabilityAccountId, new BigDecimal("1000.00"), "Credit", 3)
        );

        // When
        JournalEntryAggregate entry = JournalEntryAggregate.create(
                tenantId, "JE-202601-000002", "PAYMENT", referenceId,
                null, LocalDate.now(), "Multi-line entry", lines, userId
        );

        // Then
        assertThat(entry.getTotalDebit()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(entry.getTotalCredit()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(entry.getLines()).hasSize(3);
    }

    // -----------------------------------------------------------------------
    // Invariant violations — creation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should throw IllegalArgumentException for unbalanced entry (debits != credits)")
    void shouldRejectUnbalancedEntry() {
        // Given
        List<JournalLine> unbalancedLines = List.of(
                JournalLine.debit(assetAccountId, new BigDecimal("1000.00"), "Debit", 1),
                JournalLine.credit(liabilityAccountId, new BigDecimal("999.00"), "Credit", 2)
        );

        // When / Then
        assertThatThrownBy(() -> JournalEntryAggregate.create(
                tenantId, "JE-BAD", "LOAN", referenceId,
                "TEST", LocalDate.now(), "Unbalanced", unbalancedLines, userId
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unbalanced");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when tenantId is null")
    void shouldRejectNullTenantId() {
        List<JournalLine> lines = List.of(
                JournalLine.debit(assetAccountId, new BigDecimal("100.00"), "D", 1),
                JournalLine.credit(liabilityAccountId, new BigDecimal("100.00"), "C", 2)
        );

        assertThatThrownBy(() -> JournalEntryAggregate.create(
                null, "JE-001", "LOAN", referenceId,
                "TEST", LocalDate.now(), "Desc", lines, userId
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("TenantId");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when only one line provided")
    void shouldRejectSingleLine() {
        List<JournalLine> singleLine = List.of(
                JournalLine.debit(assetAccountId, new BigDecimal("100.00"), "D", 1)
        );

        assertThatThrownBy(() -> JournalEntryAggregate.create(
                tenantId, "JE-001", "LOAN", referenceId,
                "TEST", LocalDate.now(), "Desc", singleLine, userId
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 2 lines");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for blank entry number")
    void shouldRejectBlankEntryNumber() {
        List<JournalLine> lines = List.of(
                JournalLine.debit(assetAccountId, new BigDecimal("100.00"), "D", 1),
                JournalLine.credit(liabilityAccountId, new BigDecimal("100.00"), "C", 2)
        );

        assertThatThrownBy(() -> JournalEntryAggregate.create(
                tenantId, "", "LOAN", referenceId,
                "TEST", LocalDate.now(), "Desc", lines, userId
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Entry number");
    }

    // -----------------------------------------------------------------------
    // State transitions
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should transition from PENDING to POSTED")
    void shouldTransitionToPosted() {
        JournalEntryAggregate entry = createValidEntry();

        entry.markPosted();

        assertThat(entry.getStatus()).isEqualTo(EntryStatus.POSTED);
    }

    @Test
    @DisplayName("Should transition from PENDING to FAILED")
    void shouldTransitionToFailed() {
        JournalEntryAggregate entry = createValidEntry();

        entry.markFailed();

        assertThat(entry.getStatus()).isEqualTo(EntryStatus.FAILED);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when posting a non-PENDING entry")
    void shouldRejectPostingNonPendingEntry() {
        JournalEntryAggregate entry = createValidEntry();
        entry.markPosted();

        assertThatThrownBy(entry::markPosted)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("Should reverse a POSTED entry")
    void shouldReversePostedEntry() {
        JournalEntryAggregate entry = createValidEntry();
        entry.markPosted();

        JournalEntryId reversalId = JournalEntryId.generate();
        entry.reverse(reversalId, "Test reversal");

        assertThat(entry.getStatus()).isEqualTo(EntryStatus.REVERSED);
        assertThat(entry.getReversedByEntryId()).isEqualTo(reversalId);
        assertThat(entry.getReversalReason()).isEqualTo("Test reversal");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when reversing a non-POSTED entry")
    void shouldRejectReversingNonPostedEntry() {
        JournalEntryAggregate entry = createValidEntry(); // PENDING

        assertThatThrownBy(() -> entry.reverse(JournalEntryId.generate(), "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("POSTED");
    }

    // -----------------------------------------------------------------------
    // Fineract sync
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should record Fineract sync with transaction ID")
    void shouldRecordFineractSync() {
        JournalEntryAggregate entry = createValidEntry();
        entry.markPosted();

        entry.recordFineractSync(999L);

        assertThat(entry.isFineractSynced()).isTrue();
        assertThat(entry.getFineractTransactionId()).isEqualTo(999L);
        assertThat(entry.getUncommittedEvents())
                .anyMatch(e -> e instanceof JournalEntryAggregate.JournalEntrySynced);
    }

    @Test
    @DisplayName("Should throw NullPointerException when recording sync without transaction ID")
    void shouldRejectNullFineractTransactionId() {
        JournalEntryAggregate entry = createValidEntry();
        entry.markPosted();

        assertThatThrownBy(() -> entry.recordFineractSync(null))
                .isInstanceOf(NullPointerException.class);
    }

    // -----------------------------------------------------------------------
    // JournalLine value object
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("JournalLine.debit should create debit line")
    void journalLineDebitShouldWork() {
        JournalLine line = JournalLine.debit(assetAccountId, new BigDecimal("500.00"), "Desc", 1);

        assertThat(line.isDebit()).isTrue();
        assertThat(line.isCredit()).isFalse();
        assertThat(line.effectiveAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("JournalLine.credit should create credit line")
    void journalLineCreditShouldWork() {
        JournalLine line = JournalLine.credit(liabilityAccountId, new BigDecimal("200.00"), "Desc", 1);

        assertThat(line.isCredit()).isTrue();
        assertThat(line.isDebit()).isFalse();
        assertThat(line.effectiveAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("JournalLine should reject both debit and credit being positive")
    void journalLineShouldRejectBothPositive() {
        assertThatThrownBy(() -> new JournalLine(
                assetAccountId,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                "Both positive",
                1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("both debit and credit");
    }

    @Test
    @DisplayName("JournalLine should reject both debit and credit being zero")
    void journalLineShouldRejectBothZero() {
        assertThatThrownBy(() -> new JournalLine(
                assetAccountId,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Both zero",
                1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("either a debit or credit");
    }

    // -----------------------------------------------------------------------
    // Events
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should emit JournalEntryPosted event on create")
    void shouldEmitJournalEntryPostedEvent() {
        JournalEntryAggregate entry = createValidEntry();

        assertThat(entry.getUncommittedEvents()).hasSize(1);
        var event = (JournalEntryAggregate.JournalEntryPosted) entry.getUncommittedEvents().get(0);
        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.referenceType()).isEqualTo("LOAN");
    }

    @Test
    @DisplayName("Should clear events after markEventsAsCommitted")
    void shouldClearEventsAfterCommit() {
        JournalEntryAggregate entry = createValidEntry();
        assertThat(entry.getUncommittedEvents()).hasSize(1);

        entry.markEventsAsCommitted();

        assertThat(entry.getUncommittedEvents()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private JournalEntryAggregate createValidEntry() {
        return JournalEntryAggregate.create(
                tenantId,
                "JE-202601-000001",
                "LOAN",
                referenceId,
                "DISBURSEMENT",
                LocalDate.now(),
                "Test balanced entry",
                List.of(
                        JournalLine.debit(assetAccountId, new BigDecimal("500.000000"), "Debit", 1),
                        JournalLine.credit(liabilityAccountId, new BigDecimal("500.000000"), "Credit", 2)
                ),
                userId
        );
    }
}
