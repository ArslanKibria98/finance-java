package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.port.out.CustomerRepository;
import com.ksa.financing.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.mapper.CustomerPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CustomerRepositoryImpl implements CustomerRepository {

    private final JpaCustomerRepository jpaRepository;

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of(
            "kycStatus", "lifecycleStage", "gender", "nationality", "residencyType", "country", "nafathVerified"
    );

    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "fullName", "firstName", "lastName", "nationalId", "mobileNumber", "email", "cifNumber"
    );

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
    public Optional<Customer> findByMobileNumber(String mobileNumber) {
        log.debug("Finding customer by mobile number (cross-tenant)");
        return jpaRepository.findByMobileNumberAndDeletedAtIsNull(mobileNumber)
                .map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByNationalId(UUID tenantId, String nationalId) {
        return jpaRepository.existsByNationalIdAndTenantIdAndDeletedAtIsNull(nationalId, tenantId);
    }

    @Override
    public PageResponse<Customer> findAll(PageQuery query) {
        log.debug("Finding all customers (cross-tenant) page={} size={}", query.page(), query.size());
        
        Specification<CustomerJpaEntity> notDeleted = (root, q, cb) -> cb.isNull(root.get("deletedAt"));
        
        Specification<CustomerJpaEntity> dynamic = SpecificationBuilder.<CustomerJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<CustomerJpaEntity> page = jpaRepository.findAll(notDeleted.and(dynamic), query.toPageable());
        return PageResponse.from(page, CustomerPersistenceMapper::toDomain);
    }

    @Override
    public PageResponse<Customer> findAllByTenantId(UUID tenantId, PageQuery query) {
        log.debug("Finding all customers for tenant: {} page={} size={}", tenantId, query.page(), query.size());
        
        Specification<CustomerJpaEntity> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.isNull(root.get("deletedAt"))
        );
        
        Specification<CustomerJpaEntity> dynamic = SpecificationBuilder.<CustomerJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<CustomerJpaEntity> page = jpaRepository.findAll(spec.and(dynamic), query.toPageable());
        return PageResponse.from(page, CustomerPersistenceMapper::toDomain);
    }

    @Override
    public PageResponse<Customer> findByLifecycleStage(UUID tenantId, String lifecycleStage, PageQuery query) {
        log.debug("Finding customers by lifecycle stage: {} for tenant: {} page={} size={}", 
                lifecycleStage, tenantId, query.page(), query.size());
        
        Specification<CustomerJpaEntity> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.equal(root.get("lifecycleStage"), lifecycleStage),
                cb.isNull(root.get("deletedAt"))
        );
        
        Specification<CustomerJpaEntity> dynamic = SpecificationBuilder.<CustomerJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<CustomerJpaEntity> page = jpaRepository.findAll(spec.and(dynamic), query.toPageable());
        return PageResponse.from(page, CustomerPersistenceMapper::toDomain);
    }

    @Override
    public PageResponse<Customer> findByKycStatus(UUID tenantId, String kycStatus, PageQuery query) {
        log.debug("Finding customers by KYC status: {} for tenant: {} page={} size={}", 
                kycStatus, tenantId, query.page(), query.size());
        
        Specification<CustomerJpaEntity> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.equal(root.get("kycStatus"), kycStatus),
                cb.isNull(root.get("deletedAt"))
        );
        
        Specification<CustomerJpaEntity> dynamic = SpecificationBuilder.<CustomerJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<CustomerJpaEntity> page = jpaRepository.findAll(spec.and(dynamic), query.toPageable());
        return PageResponse.from(page, CustomerPersistenceMapper::toDomain);
    }
}
