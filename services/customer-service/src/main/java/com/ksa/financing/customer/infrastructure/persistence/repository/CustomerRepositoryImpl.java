package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.port.out.CustomerRepository;
import com.ksa.financing.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.mapper.CustomerPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CustomerRepositoryImpl implements CustomerRepository {

    private final JpaCustomerRepository jpaRepository;

    @Override
    public Customer save(Customer customer) {
        log.debug("Saving customer with CIF: {}", customer.getCifNumber());
        CustomerJpaEntity entity = CustomerPersistenceMapper.toEntity(customer);
        CustomerJpaEntity saved = jpaRepository.save(entity);
        return CustomerPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Customer> findById(UUID tenantId, UUID id) {
        log.debug("Finding customer by ID: {} for tenant: {}", id, tenantId);
        return jpaRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        log.debug("Finding customer by ID: {} (cross-tenant)", id);
        return jpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByCifNumber(UUID tenantId, String cifNumber) {
        log.debug("Finding customer by CIF: {} for tenant: {}", cifNumber, tenantId);
        return jpaRepository.findByCifNumberAndTenantIdAndDeletedAtIsNull(cifNumber, tenantId)
                .map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByNationalId(UUID tenantId, String nationalId) {
        log.debug("Finding customer by national ID for tenant: {}", tenantId);
        return jpaRepository.findByNationalIdAndTenantIdAndDeletedAtIsNull(nationalId, tenantId)
                .map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByNationalId(String nationalId) {
        log.debug("Finding customer by national ID (cross-tenant)");
        return jpaRepository.findByNationalIdAndDeletedAtIsNull(nationalId)
                .map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByKeycloakUserId(UUID keycloakUserId) {
        log.debug("Finding customer by Keycloak user ID: {}", keycloakUserId);
        return jpaRepository.findByKeycloakUserId(keycloakUserId)
                .map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        log.debug("Finding customer by idempotency key for tenant: {}", tenantId);
        return jpaRepository.findByTenantIdAndIdempotencyKeyAndDeletedAtIsNull(tenantId, idempotencyKey)
                .map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByNationalId(UUID tenantId, String nationalId) {
        return jpaRepository.existsByNationalIdAndTenantIdAndDeletedAtIsNull(nationalId, tenantId);
    }

    @Override
    public List<Customer> findAll() {
        log.debug("Finding all customers (cross-tenant)");
        return jpaRepository.findByDeletedAtIsNull().stream()
                .map(CustomerPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Customer> findAllByTenantId(UUID tenantId) {
        log.debug("Finding all customers for tenant: {}", tenantId);
        return jpaRepository.findByTenantIdAndDeletedAtIsNull(tenantId).stream()
                .map(CustomerPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Customer> findByLifecycleStage(UUID tenantId, String lifecycleStage) {
        log.debug("Finding customers by lifecycle stage: {} for tenant: {}", lifecycleStage, tenantId);
        return jpaRepository.findByTenantIdAndLifecycleStageAndDeletedAtIsNull(tenantId, lifecycleStage).stream()
                .map(CustomerPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Customer> findByKycStatus(UUID tenantId, String kycStatus) {
        log.debug("Finding customers by KYC status: {} for tenant: {}", kycStatus, tenantId);
        return jpaRepository.findByTenantIdAndKycStatusAndDeletedAtIsNull(tenantId, kycStatus).stream()
                .map(CustomerPersistenceMapper::toDomain)
                .toList();
    }
}
