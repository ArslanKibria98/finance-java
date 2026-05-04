package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.identity.domain.model.Employee;
import com.ksa.financing.identity.domain.port.out.EmployeeRepository;
import com.ksa.financing.identity.infrastructure.persistence.entity.EmployeeJpaEntity;
import com.ksa.financing.identity.infrastructure.persistence.mapper.EmployeePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EmployeeRepositoryImpl implements EmployeeRepository {

    private final JpaEmployeeRepository jpaEmployeeRepository;

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("department", "jobTitle", "status");
    private static final Set<String> SEARCHABLE_FIELDS = Set.of("fullName", "email", "mobileNumber");

    @Override
    public Employee save(Employee employee) {
        var entity = EmployeePersistenceMapper.toEntity(employee);
        var saved = jpaEmployeeRepository.save(entity);
        return EmployeePersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Employee> findById(UUID tenantId, UUID employeeId) {
        return jpaEmployeeRepository.findByTenantIdAndId(tenantId, employeeId)
                .map(EmployeePersistenceMapper::toDomain);
    }

    @Override
    public PageResponse<Employee> findAllByTenant(UUID tenantId, PageQuery query) {
        Specification<EmployeeJpaEntity> tenantSpec = (root, q, cb) -> cb.equal(root.get("tenantId"), tenantId);
        
        Specification<EmployeeJpaEntity> dynamic = SpecificationBuilder.<EmployeeJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<EmployeeJpaEntity> page = jpaEmployeeRepository.findAll(tenantSpec.and(dynamic), query.toPageable());
        return PageResponse.from(page, EmployeePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(UUID tenantId, String email) {
        return jpaEmployeeRepository.existsByTenantIdAndEmail(tenantId, email);
    }

    @Override
    public Optional<Employee> findByKeycloakUserId(UUID keycloakUserId) {
        return jpaEmployeeRepository.findByKeycloakUserId(keycloakUserId)
                .map(EmployeePersistenceMapper::toDomain);
    }
}
