package com.ksa.financing.ledger.domain.port.out;

import com.ksa.financing.ledger.domain.model.AccountAggregate;
import com.ksa.financing.ledger.domain.model.AccountId;
import com.ksa.financing.ledger.domain.model.AccountType;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: persistence contract for AccountAggregate (COA).
 * Implementations live in infrastructure.persistence.
 */
public interface AccountRepository {

    AccountAggregate save(AccountAggregate account);

    Optional<AccountAggregate> findById(UUID tenantId, AccountId accountId);

    Optional<AccountAggregate> findByCode(UUID tenantId, String accountCode);

    PageResponse<AccountAggregate> findAllByTenant(UUID tenantId, PageQuery query);

    List<AccountAggregate> findByType(UUID tenantId, AccountType accountType);

    boolean existsByCode(UUID tenantId, String accountCode);
}
