package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.IbanBeneficiaryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaIbanBeneficiaryRepository extends JpaRepository<IbanBeneficiaryJpaEntity, UUID> {

    Optional<IbanBeneficiaryJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<IbanBeneficiaryJpaEntity> findByTenantIdAndCustomerIdAndIban(UUID tenantId, UUID customerId, String iban);

    List<IbanBeneficiaryJpaEntity> findByTenantIdAndCustomerIdAndActiveTrueOrderByCreatedAtDesc(UUID tenantId, UUID customerId);
}
