package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.model.Employee;
import com.ksa.financing.identity.domain.model.EmployeeStatus;
import com.ksa.financing.identity.domain.port.in.ManageEmployeeUseCase;
import com.ksa.financing.identity.domain.port.out.EmployeeRepository;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.identity.domain.port.out.RoleRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageEmployeeService implements ManageEmployeeUseCase {

    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final KeycloakAdapterPort keycloakAdapter;

    @Value("${keycloak.realm:CompanyRealm}")
    private String keycloakRealm;

    @Override
    @Transactional
    public Employee create(CreateEmployeeCommand command) {
        if (employeeRepository.existsByEmail(command.tenantId(), command.email())) {
            throw new BusinessException(
                    ErrorCodes.Identity.EMPLOYEE_DUPLICATE,
                    "Employee with this email already exists: " + command.email(),
                    command.email());
        }

        // Validate role exists if provided
        if (command.roleId() != null) {
            roleRepository.findById(command.tenantId(), command.roleId())
                    .orElseThrow(() -> NotFoundException.forEntity("Role", command.roleId().toString()));
        }

        // Create user in Keycloak
        var keycloakUser = keycloakAdapter.createUser(
                keycloakRealm, command.email(), command.email(), command.password());

        // Assign role in Keycloak if provided
        if (command.roleId() != null) {
            var role = roleRepository.findById(command.tenantId(), command.roleId()).orElse(null);
            if (role != null) {
                keycloakAdapter.assignRole(keycloakRealm, keycloakUser.keycloakUserId(), role.getRoleCode());
            }
        }

        // Set tenant_id attribute in Keycloak
        keycloakAdapter.setUserAttribute(keycloakRealm, keycloakUser.keycloakUserId(),
                "tenant_id", command.tenantId().toString());

        var employee = new Employee();
        employee.setTenantId(command.tenantId());
        employee.setKeycloakUserId(keycloakUser.keycloakUserId());
        employee.setName(command.name());
        employee.setEmail(command.email());
        employee.setPhone(command.phone());
        employee.setAddress(command.address());
        employee.setRoleId(command.roleId());
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setCreatedAt(Instant.now());
        employee.setUpdatedAt(Instant.now());

        var saved = employeeRepository.save(employee);
        log.info("Employee created: email={} id={} keycloakUserId={} tenant={}",
                saved.getEmail(), saved.getId(), saved.getKeycloakUserId(), saved.getTenantId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getById(UUID tenantId, UUID employeeId) {
        return employeeRepository.findById(tenantId, employeeId)
                .orElseThrow(() -> NotFoundException.forEntity("Employee", employeeId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> listByTenant(UUID tenantId) {
        return employeeRepository.findAllByTenant(tenantId);
    }

    @Override
    @Transactional
    public Employee update(UUID tenantId, UUID employeeId, UpdateEmployeeCommand command) {
        var employee = employeeRepository.findById(tenantId, employeeId)
                .orElseThrow(() -> NotFoundException.forEntity("Employee", employeeId.toString()));

        if (command.name() != null) employee.setName(command.name());
        if (command.phone() != null) employee.setPhone(command.phone());
        if (command.address() != null) employee.setAddress(command.address());
        if (command.roleId() != null) {
            roleRepository.findById(tenantId, command.roleId())
                    .orElseThrow(() -> NotFoundException.forEntity("Role", command.roleId().toString()));
            employee.setRoleId(command.roleId());

            // Update role in Keycloak
            if (employee.getKeycloakUserId() != null) {
                var role = roleRepository.findById(tenantId, command.roleId()).orElse(null);
                if (role != null) {
                    keycloakAdapter.assignRole(keycloakRealm, employee.getKeycloakUserId(), role.getRoleCode());
                }
            }
        }
        if (command.status() != null) {
            try {
                employee.setStatus(EmployeeStatus.valueOf(command.status().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(
                        ErrorCodes.VALIDATION_FAILED,
                        "Invalid status: " + command.status() + ". Valid values: ACTIVE, INACTIVE, SUSPENDED, TERMINATED",
                        command.status());
            }
        } else if (command.active() != null) {
            employee.setStatus(command.active() ? EmployeeStatus.ACTIVE : EmployeeStatus.INACTIVE);
        }
        employee.setUpdatedAt(Instant.now());

        var saved = employeeRepository.save(employee);
        log.info("Employee updated: id={} tenant={}", saved.getId(), tenantId);
        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID employeeId) {
        var employee = employeeRepository.findById(tenantId, employeeId)
                .orElseThrow(() -> NotFoundException.forEntity("Employee", employeeId.toString()));

        employee.setStatus(EmployeeStatus.TERMINATED);
        employee.setUpdatedAt(Instant.now());
        employeeRepository.save(employee);
        log.info("Employee terminated (soft-delete): id={} tenant={}", employeeId, tenantId);
    }
}
