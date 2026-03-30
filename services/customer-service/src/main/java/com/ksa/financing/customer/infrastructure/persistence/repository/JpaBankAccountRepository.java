package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.BankAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaBankAccountRepository extends JpaRepository<BankAccountJpaEntity, UUID> {

    List<BankAccountJpaEntity> findByCustomerIdAndTenantIdAndDeletedAtIsNull(UUID customerId, UUID tenantId);

    List<BankAccountJpaEntity> findByCustomerIdAndDeletedAtIsNull(UUID customerId);

    java.util.Optional<BankAccountJpaEntity> findByCustomerIdAndIbanAndDeletedAtIsNull(UUID customerId, String iban);
}
