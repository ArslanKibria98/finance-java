package com.ksa.financing.identity.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.identity.domain.model.Employee;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository {
    Employee save(Employee employee);
    Optional<Employee> findById(UUID tenantId, UUID employeeId);
    PageResponse<Employee> findAllByTenant(UUID tenantId, PageQuery query);
    boolean existsByEmail(UUID tenantId, String email);
    Optional<Employee> findByKeycloakUserId(UUID keycloakUserId);
}
