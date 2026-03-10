package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.port.out.CustomerRepository;
import com.ksa.financing.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.mapper.CustomerPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

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
}
