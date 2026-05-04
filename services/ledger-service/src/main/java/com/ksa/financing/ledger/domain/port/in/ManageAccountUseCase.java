package com.ksa.financing.ledger.domain.port.in;

import com.ksa.financing.ledger.domain.model.AccountAggregate;
import com.ksa.financing.ledger.domain.model.AccountId;
import com.ksa.financing.ledger.domain.model.AccountType;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import java.util.List;
import java.util.UUID;

/**
 * Input port: Manage Chart of Accounts (COA) entries.
 */
public interface ManageAccountUseCase {

    AccountAggregate create(CreateAccountCommand command);

    AccountAggregate update(UUID tenantId, AccountId accountId, UpdateAccountCommand command);

    AccountAggregate getById(UUID tenantId, AccountId accountId);

    AccountAggregate getByCode(UUID tenantId, String accountCode);

    PageResponse<AccountAggregate> listByTenant(UUID tenantId, PageQuery query);

    void deactivate(UUID tenantId, AccountId accountId);

    // -----------------------------------------------------------------------
    // Commands
    // -----------------------------------------------------------------------

    record CreateAccountCommand(
            UUID tenantId,
            String accountCode,
            String accountName,
            String accountNameAr,
            AccountType accountType,
            String parentAccountCode,
            boolean isHeader,
            String iban
    ) {}

    record UpdateAccountCommand(
            String accountName,
            String accountNameAr
    ) {}
}
