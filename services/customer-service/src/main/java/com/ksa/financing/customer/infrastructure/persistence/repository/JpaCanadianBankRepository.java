package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.CanadianBankOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JpaCanadianBankRepository extends JpaRepository<CanadianBankOptionJpaEntity, UUID>,
        JpaSpecificationExecutor<CanadianBankOptionJpaEntity> {
    Optional<CanadianBankOptionJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
}
