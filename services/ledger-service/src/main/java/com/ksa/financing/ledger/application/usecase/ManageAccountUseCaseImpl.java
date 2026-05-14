package com.ksa.financing.ledger.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.ledger.domain.model.AccountAggregate;
import com.ksa.financing.ledger.domain.model.AccountId;
import com.ksa.financing.ledger.domain.port.in.ManageAccountUseCase;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import com.ksa.financing.ledger.domain.port.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case implementation for managing GL Chart of Accounts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManageAccountUseCaseImpl implements ManageAccountUseCase {

    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public AccountAggregate create(CreateAccountCommand command) {
        log.info("Creating GL account: code={} type={} tenant={}",
                command.accountCode(), command.accountType(), command.tenantId());

        // Guard: duplicate code
        if (accountRepository.existsByCode(command.tenantId(), command.accountCode())) {
            throw new BusinessException(
                    ErrorCodes.CONFLICT,
                    "GL account with code already exists: " + command.accountCode());
        }

        // Resolve parent account if provided
        AccountId parentId = null;
        if (command.parentAccountCode() != null && !command.parentAccountCode().isBlank()) {
            var parent = accountRepository.findByCode(command.tenantId(), command.parentAccountCode())
                    .orElseThrow(() -> NotFoundException.forEntity("Account", command.parentAccountCode()));
            parentId = parent.getId();
        }

        AccountAggregate account = AccountAggregate.create(
                command.tenantId(),
                command.accountCode(),
                command.accountName(),
                command.accountType(),
                parentId,
                command.isHeader()
        );

        // Set Arabic name
        if (command.accountNameAr() != null) {
            account.updateName(command.accountName(), command.accountNameAr());
        }

        // Set IBAN if provided
        if (command.iban() != null && !command.iban().isBlank()) {
            account.setIban(command.iban());
        }

        AccountAggregate saved = accountRepository.save(account);
        log.info("GL account created: id={} code={}", saved.getId(), saved.getAccountCode());

        eventPublisher.publishAll(saved.getUncommittedEvents());
        saved.markEventsAsCommitted();

        return saved;
    }

    @Override
    @Transactional
    public AccountAggregate update(UUID tenantId, AccountId accountId, UpdateAccountCommand command) {
        AccountAggregate account = accountRepository.findById(tenantId, accountId)
                .orElseThrow(() -> NotFoundException.forEntity("Account", accountId.toString()));

        account.updateName(command.accountName(), command.accountNameAr());
        return accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountAggregate getById(UUID tenantId, AccountId accountId) {
        return accountRepository.findById(tenantId, accountId)
                .orElseThrow(() -> NotFoundException.forEntity("Account", accountId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountAggregate getByCode(UUID tenantId, String accountCode) {
        return accountRepository.findByCode(tenantId, accountCode)
                .orElseThrow(() -> NotFoundException.forEntity("Account", accountCode));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccountAggregate> listByTenant(UUID tenantId, PageQuery query) {
        return accountRepository.findAllByTenant(tenantId, query);
    }

    @Override
    @Transactional
    public void deactivate(UUID tenantId, AccountId accountId) {
        AccountAggregate account = accountRepository.findById(tenantId, accountId)
                .orElseThrow(() -> NotFoundException.forEntity("Account", accountId.toString()));
        account.deactivate();
        accountRepository.save(account);
        log.info("GL account deactivated: id={}", accountId);
    }

    @Override
    @Transactional
    public void activate(UUID tenantId, AccountId accountId) {
        AccountAggregate account = accountRepository.findById(tenantId, accountId)
                .orElseThrow(() -> NotFoundException.forEntity("Account", accountId.toString()));
        account.activate();
        accountRepository.save(account);
        log.info("GL account activated: id={}", accountId);
    }
}
