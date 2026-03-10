package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.BankAccount;
import com.ksa.financing.customer.domain.port.out.BankAccountRepository;
import com.ksa.financing.customer.infrastructure.persistence.entity.BankAccountJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.mapper.CustomerPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BankAccountRepositoryImpl implements BankAccountRepository {

    private final JpaBankAccountRepository jpaRepository;

    @Override
    public BankAccount save(BankAccount bankAccount) {
        log.debug("Saving bank account for customer: {}", bankAccount.getCustomerId());
        BankAccountJpaEntity entity = CustomerPersistenceMapper.toEntity(bankAccount);
        BankAccountJpaEntity saved = jpaRepository.save(entity);
        return CustomerPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<BankAccount> findById(UUID id) {
        log.debug("Finding bank account by ID: {}", id);
        return jpaRepository.findById(id)
                .map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public List<BankAccount> findByCustomerId(UUID tenantId, UUID customerId) {
        log.debug("Finding bank accounts for customer: {} in tenant: {}", customerId, tenantId);
        return jpaRepository.findByCustomerIdAndTenantIdAndDeletedAtIsNull(customerId, tenantId)
                .stream()
                .map(CustomerPersistenceMapper::toDomain)
                .toList();
    }
}
