package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.infrastructure.persistence.entity.BankJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaBankRepository extends JpaRepository<BankJpaEntity, UUID> {

    List<BankJpaEntity> findByTenantIdAndActiveTrueOrderBySortOrder(UUID tenantId);
}
