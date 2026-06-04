package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.IbftBeneficiaryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaIbftBeneficiaryRepository extends JpaRepository<IbftBeneficiaryJpaEntity, UUID> {

    Optional<IbftBeneficiaryJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<IbftBeneficiaryJpaEntity> findByTenantIdAndCustomerIdAndInstitutionNumberAndTransitAndAccountNumber(
            UUID tenantId, UUID customerId, String institutionNumber, String transit, String accountNumber);

    List<IbftBeneficiaryJpaEntity> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(UUID tenantId, UUID customerId);
}
