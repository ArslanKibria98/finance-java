package com.ksa.financing.identity.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.identity.domain.model.Employee;

import java.util.UUID;

public interface ManageEmployeeUseCase {

    Employee create(CreateEmployeeCommand command);

    Employee getById(UUID tenantId, UUID employeeId);

    PageResponse<Employee> listByTenant(UUID tenantId, PageQuery query);

    Employee update(UUID tenantId, UUID employeeId, UpdateEmployeeCommand command);

    void delete(UUID tenantId, UUID employeeId);

    record CreateEmployeeCommand(
            UUID tenantId,
            String name,
            String email,
            String password,
            String phone,
            String address,
            UUID roleId
    ) {}

    record UpdateEmployeeCommand(
            String name,
            String phone,
            String address,
            UUID roleId,
            Boolean active,
            String status
    ) {}
}
