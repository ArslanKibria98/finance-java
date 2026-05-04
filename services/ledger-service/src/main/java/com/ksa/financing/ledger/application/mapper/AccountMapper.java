package com.ksa.financing.ledger.application.mapper;

import com.ksa.financing.ledger.application.dto.AccountResponse;
import com.ksa.financing.ledger.domain.model.AccountAggregate;
import org.springframework.stereotype.Component;

/**
 * Application-layer mapper for GL Accounts.
 */
@Component
public class AccountMapper {

    public AccountResponse toResponse(AccountAggregate account) {
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
}
