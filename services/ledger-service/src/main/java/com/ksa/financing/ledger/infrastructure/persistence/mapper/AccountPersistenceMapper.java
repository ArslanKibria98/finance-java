package com.ksa.financing.ledger.infrastructure.persistence.mapper;

import com.ksa.financing.ledger.domain.model.AccountAggregate;
import com.ksa.financing.ledger.domain.model.AccountId;
import com.ksa.financing.ledger.domain.model.AccountStatus;
import com.ksa.financing.ledger.domain.model.AccountType;
import com.ksa.financing.ledger.infrastructure.persistence.entity.AccountJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Maps between AccountAggregate (domain) and AccountJpaEntity (persistence).
 */
@Component
public class AccountPersistenceMapper {

    public AccountJpaEntity toJpaEntity(AccountAggregate domain) {
        return AccountJpaEntity.builder()
                .id(domain.getId().value())
                .tenantId(domain.getTenantId())
                .accountCode(domain.getAccountCode())
                .accountName(domain.getAccountName())
                .accountNameAr(domain.getAccountNameAr())
                .accountType(AccountJpaEntity.AccountTypeDb.valueOf(domain.getAccountType().name()))
                .parentAccountId(domain.getParentAccountId() != null
                        ? domain.getParentAccountId().value() : null)
                .hierarchyLevel(domain.getHierarchyLevel())
                .hierarchyPath(domain.getHierarchyPath())
                .isHeader(domain.isHeader())
                .isManualEntriesAllowed(domain.isManualEntriesAllowed())
                .status(AccountJpaEntity.AccountStatusDb.valueOf(domain.getStatus().name()))
                .fineractMappingId(domain.getFineractMappingId())
                .iban(domain.getIban())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public AccountAggregate toDomain(AccountJpaEntity entity) {
        return AccountAggregate.builder()
                .id(AccountId.of(entity.getId()))
                .tenantId(entity.getTenantId())
                .accountCode(entity.getAccountCode())
                .accountName(entity.getAccountName())
                .accountNameAr(entity.getAccountNameAr())
                .accountType(AccountType.valueOf(entity.getAccountType().name()))
                .parentAccountId(entity.getParentAccountId() != null
                        ? AccountId.of(entity.getParentAccountId()) : null)
                .hierarchyLevel(entity.getHierarchyLevel())
                .hierarchyPath(entity.getHierarchyPath())
                .isHeader(entity.isHeader())
                .isManualEntriesAllowed(entity.isManualEntriesAllowed())
                .status(AccountStatus.valueOf(entity.getStatus().name()))
                .fineractMappingId(entity.getFineractMappingId())
                .iban(entity.getIban())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
