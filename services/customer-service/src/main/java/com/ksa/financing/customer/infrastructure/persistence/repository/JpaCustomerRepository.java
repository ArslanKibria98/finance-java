package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaCustomerRepository extends JpaRepository<CustomerJpaEntity, UUID>, JpaSpecificationExecutor<CustomerJpaEntity> {

    Optional<CustomerJpaEntity> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<CustomerJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
    
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from CustomerJpaEntity c where c.id = :id and c.deletedAt is null")
    Optional<CustomerJpaEntity> findWithLockById(UUID id);

    Optional<CustomerJpaEntity> findByCifNumberAndTenantIdAndDeletedAtIsNull(String cifNumber, UUID tenantId);

    Optional<CustomerJpaEntity> findByNationalIdAndTenantIdAndDeletedAtIsNull(String nationalId, UUID tenantId);

    Optional<CustomerJpaEntity> findByNationalIdAndDeletedAtIsNull(String nationalId);

    Optional<CustomerJpaEntity> findByKeycloakUserId(UUID keycloakUserId);

    Optional<CustomerJpaEntity> findByTenantIdAndIdempotencyKeyAndDeletedAtIsNull(UUID tenantId, String idempotencyKey);

    boolean existsByNationalIdAndTenantIdAndDeletedAtIsNull(String nationalId, UUID tenantId);

    Optional<CustomerJpaEntity> findByMobileNumberAndDeletedAtIsNull(String mobileNumber);

    List<CustomerJpaEntity> findByDeletedAtIsNull();

    List<CustomerJpaEntity> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    List<CustomerJpaEntity> findByTenantIdAndLifecycleStageAndDeletedAtIsNull(UUID tenantId, String lifecycleStage);

    List<CustomerJpaEntity> findByTenantIdAndKycStatusAndDeletedAtIsNull(UUID tenantId, String kycStatus);
}
