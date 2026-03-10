package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.identity.domain.model.Employee;
import com.ksa.financing.identity.domain.port.out.EmployeeRepository;
import com.ksa.financing.identity.infrastructure.persistence.mapper.EmployeePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EmployeeRepositoryImpl implements EmployeeRepository {

    private final JpaEmployeeRepository jpaEmployeeRepository;

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
    public List<Employee> findAllByTenant(UUID tenantId) {
        return jpaEmployeeRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(EmployeePersistenceMapper::toDomain)
                .toList();
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
