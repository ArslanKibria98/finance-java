package com.ksa.financing.ledger.unit.application;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.ledger.application.usecase.PostSimpleAccountMovementUseCaseImpl;
import com.ksa.financing.ledger.domain.model.AccountAggregate;
import com.ksa.financing.ledger.domain.model.AccountType;
import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;
import com.ksa.financing.ledger.domain.model.JournalEntryId;
import com.ksa.financing.ledger.domain.model.JournalLine;
import com.ksa.financing.ledger.domain.port.in.PostJournalEntryUseCase;
import com.ksa.financing.ledger.domain.port.in.PostJournalEntryUseCase.PostJournalEntryCommand;
import com.ksa.financing.ledger.domain.port.in.PostSimpleAccountMovementUseCase;
import com.ksa.financing.ledger.domain.port.in.PostSimpleAccountMovementUseCase.SimpleAccountMovementCommand;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import com.ksa.financing.ledger.application.config.LedgerAdminAdjustmentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostSimpleAccountMovementUseCase")
class PostSimpleAccountMovementUseCaseTest {

    private static final String OFFSET_CODE = "3999";

    @Mock
    private PostJournalEntryUseCase postJournalEntryUseCase;

    @Mock
    private AccountRepository accountRepository;

    private LedgerAdminAdjustmentProperties properties;

    private PostSimpleAccountMovementUseCaseImpl useCase;

    private UUID tenantId;
    private UUID userId;
    private AccountAggregate target;
    private AccountAggregate offset;

    @BeforeEach
    void setUp() {
        properties = new LedgerAdminAdjustmentProperties(OFFSET_CODE);
        useCase = new PostSimpleAccountMovementUseCaseImpl(postJournalEntryUseCase, accountRepository, properties);

        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        target = AccountAggregate.create(tenantId, "1200", "Loans Receivable", AccountType.ASSET, null, false);
        offset = AccountAggregate.create(tenantId, OFFSET_CODE, "Suspense / Offset", AccountType.LIABILITY, null, false);
    }

    @Test
    @DisplayName("CREDIT_ON_ACCOUNT credits target and debits offset")
    void creditOnAccount_pairsLines() {
        when(accountRepository.findByCode(tenantId, "1200")).thenReturn(Optional.of(target));
        when(accountRepository.findByCode(tenantId, OFFSET_CODE)).thenReturn(Optional.of(offset));

        JournalEntryAggregate posted = JournalEntryAggregate.create(
                tenantId,
                "JE-1",
                "DEPOSIT_BY_ADMIN",
                UUID.randomUUID(),
                "DEPOSIT_BY_ADMIN",
                LocalDate.now(),
                "test",
                List.of(
                        JournalLine.credit(target.getId(), new BigDecimal("50000"), "test", 1),
                        JournalLine.debit(offset.getId(), new BigDecimal("50000"), "test", 2)
                ),
                userId
        );
        when(postJournalEntryUseCase.post(any())).thenReturn(posted);

        var cmd = new SimpleAccountMovementCommand(
                tenantId,
                "1200",
                null,
                PostSimpleAccountMovementUseCase.Movement.CREDIT_ON_ACCOUNT,
                new BigDecimal("50000"),
                LocalDate.now(),
                "idem-credit-1",
                null,
                null,
                null,
                userId
        );

        var result = useCase.post(cmd);

        assertThat(result.targetAccountCode()).isEqualTo("1200");
        assertThat(result.targetAccountId()).isEqualTo(target.getId().value());

        ArgumentCaptor<PostJournalEntryCommand> captor = ArgumentCaptor.forClass(PostJournalEntryCommand.class);
        verify(postJournalEntryUseCase).post(captor.capture());
        List<JournalLine> lines = captor.getValue().lines();
        assertThat(lines).hasSize(2);
        assertThat(lines.getFirst().isCredit()).isTrue();
        assertThat(lines.getFirst().effectiveAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(lines.getFirst().accountId()).isEqualTo(target.getId());
        assertThat(lines.get(1).isDebit()).isTrue();
        assertThat(lines.get(1).accountId()).isEqualTo(offset.getId());
    }

    @Test
    @DisplayName("DEBIT_ON_ACCOUNT debits target and credits offset")
    void debitOnAccount_pairsLines() {
        when(accountRepository.findByCode(tenantId, "1200")).thenReturn(Optional.of(target));
        when(accountRepository.findByCode(tenantId, OFFSET_CODE)).thenReturn(Optional.of(offset));

        JournalEntryAggregate posted = JournalEntryAggregate.create(
                tenantId,
                "JE-2",
                "DEPOSIT_BY_ADMIN",
                UUID.randomUUID(),
                "DEPOSIT_BY_ADMIN",
                LocalDate.now(),
                "test",
                List.of(
                        JournalLine.debit(target.getId(), new BigDecimal("100"), "test", 1),
                        JournalLine.credit(offset.getId(), new BigDecimal("100"), "test", 2)
                ),
                userId
        );
        when(postJournalEntryUseCase.post(any())).thenReturn(posted);

        var cmd = new SimpleAccountMovementCommand(
                tenantId,
                "1200",
                null,
                PostSimpleAccountMovementUseCase.Movement.DEBIT_ON_ACCOUNT,
                new BigDecimal("100"),
                LocalDate.now(),
                "idem-debit-1",
                null,
                null,
                null,
                userId
        );

        useCase.post(cmd);

        ArgumentCaptor<PostJournalEntryCommand> captor = ArgumentCaptor.forClass(PostJournalEntryCommand.class);
        verify(postJournalEntryUseCase).post(captor.capture());
        List<JournalLine> lines = captor.getValue().lines();
        assertThat(lines.getFirst().isDebit()).isTrue();
        assertThat(lines.get(1).isCredit()).isTrue();
    }

    @Test
    @DisplayName("throws when offset account code is not configured")
    void rejectsMissingOffsetConfig() {
        useCase = new PostSimpleAccountMovementUseCaseImpl(
                postJournalEntryUseCase,
                accountRepository,
                new LedgerAdminAdjustmentProperties(""));

        assertThatThrownBy(() -> useCase.post(new SimpleAccountMovementCommand(
                tenantId, "1200", null,
                PostSimpleAccountMovementUseCase.Movement.CREDIT_ON_ACCOUNT,
                BigDecimal.ONE, LocalDate.now(), "k", null, null, null, userId)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("offset-account-code");
    }

    @Test
    @DisplayName("throws when target equals offset account")
    void rejectsSameTargetAndOffset() {
        var sameAsOffset = AccountAggregate.create(tenantId, OFFSET_CODE, "Same", AccountType.LIABILITY, null, false);
        when(accountRepository.findByCode(tenantId, OFFSET_CODE)).thenReturn(Optional.of(sameAsOffset));

        assertThatThrownBy(() -> useCase.post(new SimpleAccountMovementCommand(
                tenantId,
                OFFSET_CODE,
                null,
                PostSimpleAccountMovementUseCase.Movement.CREDIT_ON_ACCOUNT,
                BigDecimal.ONE,
                LocalDate.now(),
                "k",
                null,
                null,
                null,
                userId)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("same as the configured offset");
    }

    @Test
    @DisplayName("throws when account selector is invalid")
    void rejectsInvalidAccountSelector() {
        assertThatThrownBy(() -> useCase.post(new SimpleAccountMovementCommand(
                tenantId,
                null,
                null,
                PostSimpleAccountMovementUseCase.Movement.CREDIT_ON_ACCOUNT,
                BigDecimal.ONE,
                LocalDate.now(),
                "k",
                null,
                null,
                null,
                userId)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Exactly one");
    }
}
