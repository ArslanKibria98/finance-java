package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.infrastructure.persistence.entity.RescheduleConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaRescheduleConfigRepository extends JpaRepository<RescheduleConfigJpaEntity, UUID> {
    
    List<RescheduleConfigJpaEntity> findByTenantIdAndActiveTrue(UUID tenantId);
    
    Optional<RescheduleConfigJpaEntity> findByTenantIdAndRescheduleType(UUID tenantId, String rescheduleType);
}
