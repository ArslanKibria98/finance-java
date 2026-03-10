package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.EmploymentInfoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaEmploymentInfoRepository extends JpaRepository<EmploymentInfoJpaEntity, UUID> {

    Optional<EmploymentInfoJpaEntity> findByCustomerIdAndCurrentTrue(UUID customerId);

    List<EmploymentInfoJpaEntity> findAllByCustomerId(UUID customerId);
}
