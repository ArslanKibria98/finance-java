package com.ksa.financing.identity.infrastructure.persistence.mapper;

import com.ksa.financing.identity.domain.model.Employee;
import com.ksa.financing.identity.domain.model.EmployeeStatus;
import com.ksa.financing.identity.infrastructure.persistence.entity.EmployeeJpaEntity;
import com.ksa.financing.identity.infrastructure.persistence.entity.EmployeeJpaEntity.EmployeeStatusEnum;

public final class EmployeePersistenceMapper {

    private EmployeePersistenceMapper() {}

    public static Employee toDomain(EmployeeJpaEntity entity) {
        var employee = new Employee();
        employee.setId(entity.getId());
        employee.setTenantId(entity.getTenantId());
        employee.setKeycloakUserId(entity.getKeycloakUserId());
        employee.setName(entity.getName());
        employee.setEmail(entity.getEmail());
        employee.setPhone(entity.getPhone());
        employee.setAddress(entity.getAddress());
        employee.setRoleId(entity.getRoleId());
        employee.setStatus(EmployeeStatus.valueOf(entity.getStatus().name()));
        employee.setCreatedAt(entity.getCreatedAt().toInstant());
        employee.setUpdatedAt(entity.getUpdatedAt().toInstant());
        employee.setVersion(entity.getVersion());
        return employee;
    }

    public static EmployeeJpaEntity toEntity(Employee employee) {
        var entity = new EmployeeJpaEntity();
        entity.setId(employee.getId());
        entity.setTenantId(employee.getTenantId());
        entity.setKeycloakUserId(employee.getKeycloakUserId());
        entity.setName(employee.getName());
        entity.setEmail(employee.getEmail());
        entity.setPhone(employee.getPhone());
        entity.setAddress(employee.getAddress());
        entity.setRoleId(employee.getRoleId());
        entity.setStatus(EmployeeStatusEnum.valueOf(employee.getStatus().name()));
        if (employee.getCreatedAt() != null) {
            entity.setCreatedAt(employee.getCreatedAt().atOffset(java.time.ZoneOffset.UTC));
        }
        if (employee.getUpdatedAt() != null) {
            entity.setUpdatedAt(employee.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC));
        }
        entity.setVersion(employee.getVersion());
        return entity;
    }
}
