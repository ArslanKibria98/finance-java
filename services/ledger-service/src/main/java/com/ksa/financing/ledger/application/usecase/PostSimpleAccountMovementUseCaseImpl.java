package com.ksa.financing.ledger.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.ledger.domain.model.AccountAggregate;
import com.ksa.financing.ledger.domain.model.AccountId;
import com.ksa.financing.ledger.domain.model.JournalLine;
import com.ksa.financing.ledger.domain.port.in.PostJournalEntryUseCase;
import com.ksa.financing.ledger.domain.port.in.PostJournalEntryUseCase.PostJournalEntryCommand;
import com.ksa.financing.ledger.domain.port.in.PostSimpleAccountMovementUseCase;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import com.ksa.financing.ledger.application.config.LedgerAdminAdjustmentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Builds a balanced journal entry: target COA line + offset COA line from configuration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostSimpleAccountMovementUseCaseImpl implements PostSimpleAccountMovementUseCase {

    private static final String DEFAULT_REFERENCE_TYPE = "DEPOSIT_BY_ADMIN";

    private final PostJournalEntryUseCase postJournalEntryUseCase;
    private final AccountRepository accountRepository;
    private final LedgerAdminAdjustmentProperties adminAdjustmentProperties;

    @Override
    @Transactional
    public SimpleAccountMovementResult post(SimpleAccountMovementCommand command) {
        validateAccountSelector(command);
        String offsetCode = adminAdjustmentProperties.offsetAccountCode();
        if (!StringUtils.hasText(offsetCode)) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_FAILED,
                    "ledger.admin-adjustment.offset-account-code is not configured (set LEDGER_ADMIN_ADJUSTMENT_OFFSET_ACCOUNT_CODE)");
        }

        AccountAggregate target = resolveTargetAccount(command);
        AccountAggregate offset = accountRepository
                .findByCode(command.tenantId(), offsetCode.trim())
                .orElseThrow(() -> NotFoundException.forEntity("Account", offsetCode));

        if (target.getId().equals(offset.getId())) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_FAILED,
                    "Target account cannot be the same as the configured offset account");
        }

        String description = StringUtils.hasText(command.description())
                ? command.description().trim()
                : "Admin account adjustment";

        String refType = StringUtils.hasText(command.referenceType())
                ? command.referenceType().trim()
                : DEFAULT_REFERENCE_TYPE;

        UUID referenceId = command.referenceId() != null ? command.referenceId() : UUID.randomUUID();

        List<JournalLine> lines = switch (command.movement()) {
            case CREDIT_ON_ACCOUNT -> List.of(
                    JournalLine.credit(target.getId(), command.amount(), description, 1),
                    JournalLine.debit(offset.getId(), command.amount(), description, 2)
            );
            case DEBIT_ON_ACCOUNT -> List.of(
                    JournalLine.debit(target.getId(), command.amount(), description, 1),
                    JournalLine.credit(offset.getId(), command.amount(), description, 2)
            );
        };

        var journalCommand = new PostJournalEntryCommand(
                command.tenantId(),
                command.entryDate(),
                refType,
                referenceId,
                refType,
                description,
                lines,
                command.idempotencyKey(),
                command.createdBy()
        );

        var posted = postJournalEntryUseCase.post(journalCommand);
        log.info("Posted simple account movement: entryNumber={} targetCode={} movement={}",
                posted.getEntryNumber(), target.getAccountCode(), command.movement());

        return new SimpleAccountMovementResult(
                posted,
                target.getAccountCode(),
                target.getId().value()
        );
    }

    private void validateAccountSelector(SimpleAccountMovementCommand command) {
        boolean codePresent = StringUtils.hasText(command.accountCode());
        boolean idPresent = command.accountId() != null;
        if (codePresent == idPresent) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_FAILED,
                    "Exactly one of accountCode or accountId must be provided");
        }
    }

    private AccountAggregate resolveTargetAccount(SimpleAccountMovementCommand command) {
        if (StringUtils.hasText(command.accountCode())) {
            return accountRepository
                    .findByCode(command.tenantId(), command.accountCode().trim())
                    .orElseThrow(() -> NotFoundException.forEntity("Account", command.accountCode().trim()));
        }
        return accountRepository
                .findById(command.tenantId(), AccountId.of(command.accountId()))
                .orElseThrow(() -> NotFoundException.forEntity("Account", command.accountId().toString()));
    }
}
