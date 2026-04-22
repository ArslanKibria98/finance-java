package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.CustomerPepAnswerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCustomerPepAnswerRepository extends JpaRepository<CustomerPepAnswerJpaEntity, UUID> {

    Optional<CustomerPepAnswerJpaEntity> findByCustomerIdAndTenantId(UUID customerId, UUID tenantId);
}
