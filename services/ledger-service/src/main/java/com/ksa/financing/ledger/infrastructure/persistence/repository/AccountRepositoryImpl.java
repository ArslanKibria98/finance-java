package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.domain.model.AccountAggregate;
import com.ksa.financing.ledger.domain.model.AccountId;
import com.ksa.financing.ledger.domain.model.AccountType;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import com.ksa.financing.ledger.infrastructure.persistence.mapper.AccountPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port implementation: persists AccountAggregate via JPA.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private final JpaAccountRepository jpaRepo;
    private final AccountPersistenceMapper mapper;

    @Override
    public AccountAggregate save(AccountAggregate account) {
        var existing = jpaRepo.findByTenantIdAndId(account.getTenantId(), account.getId().value());
        var entity = existing.orElseGet(() -> mapper.toJpaEntity(account));

        // Keep managed entity version for updates to avoid stale-object failures on merge.
        entity.setTenantId(account.getTenantId());
        entity.setAccountCode(account.getAccountCode());
        entity.setAccountName(account.getAccountName());
        entity.setAccountNameAr(account.getAccountNameAr());
        entity.setAccountType(Enum.valueOf(entity.getAccountType().getDeclaringClass(), account.getAccountType().name()));
        entity.setParentAccountId(account.getParentAccountId() != null ? account.getParentAccountId().value() : null);
        entity.setHierarchyLevel(account.getHierarchyLevel());
        entity.setHierarchyPath(account.getHierarchyPath());
        entity.setHeader(account.isHeader());
        entity.setManualEntriesAllowed(account.isManualEntriesAllowed());
        entity.setStatus(Enum.valueOf(entity.getStatus().getDeclaringClass(), account.getStatus().name()));
        entity.setFineractMappingId(account.getFineractMappingId());
        entity.setIban(account.getIban());
        entity.setCreatedAt(account.getCreatedAt());
        entity.setUpdatedAt(account.getUpdatedAt());

        var saved = jpaRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AccountAggregate> findById(UUID tenantId, AccountId accountId) {
        return jpaRepo.findByTenantIdAndId(tenantId, accountId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<AccountAggregate> findByCode(UUID tenantId, String accountCode) {
        return jpaRepo.findByTenantIdAndAccountCode(tenantId, accountCode)
                .map(mapper::toDomain);
    }

    @Override
    public List<AccountAggregate> findAllByTenant(UUID tenantId) {
        return jpaRepo.findAllByTenantId(tenantId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AccountAggregate> findByType(UUID tenantId, AccountType accountType) {
        return jpaRepo.findAllByTenantIdAndAccountType(tenantId, accountType.name()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(UUID tenantId, String accountCode) {
        return jpaRepo.existsByTenantIdAndAccountCode(tenantId, accountCode);
    }
}
