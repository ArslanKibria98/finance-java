package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.CustomerBlockJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaCustomerBlockRepository extends JpaRepository<CustomerBlockJpaEntity, UUID> {
    List<CustomerBlockJpaEntity> findAllByCustomerIdAndActiveTrue(UUID customerId);
    List<CustomerBlockJpaEntity> findAllByCustomerId(UUID customerId);
}
