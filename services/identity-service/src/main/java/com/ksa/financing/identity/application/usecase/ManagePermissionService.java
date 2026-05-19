package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.identity.domain.model.Permission;
import com.ksa.financing.identity.domain.port.in.ManagePermissionUseCase;
import com.ksa.financing.identity.domain.port.out.ModuleRepository;
import com.ksa.financing.identity.domain.port.out.PermissionRepository;
import com.ksa.financing.identity.domain.port.out.PolicyEnforcerPort;
import com.ksa.financing.identity.domain.port.out.RoleRepository;
import com.ksa.financing.identity.infrastructure.casbin.PolicyRedisSyncService;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagePermissionService implements ManagePermissionUseCase {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final ModuleRepository moduleRepository;
    private final PolicyEnforcerPort policyEnforcer;
    private final PolicyRedisSyncService redisSyncService;

    @Override
    @Transactional
    public Permission create(CreatePermissionCommand command) {
        if (permissionRepository.existsByCode(command.tenantId(), command.permissionCode())) {
            throw new BusinessException(
                    ErrorCodes.Identity.PERMISSION_DUPLICATE,
                    "Permission with code already exists: " + command.permissionCode(),
                    command.permissionCode());
        }

        var perm = new Permission();
        perm.setTenantId(command.tenantId());
        perm.setPermissionCode(command.permissionCode());
        perm.setPermissionName(command.permissionName());
        perm.setDescription(command.description());
        perm.setResourceType(command.resourceType());
        // Auto-resolve moduleId from resourceType
        if (command.resourceType() != null) {
            moduleRepository.findByCode(command.tenantId(), command.resourceType())
                    .ifPresent(module -> perm.setModuleId(module.getId()));
        }
        perm.setAction(command.action());
        perm.setActive(true);
        perm.setCreatedAt(Instant.now());
        perm.setUpdatedAt(Instant.now());

        var saved = permissionRepository.save(perm);
        log.info("Permission created: code={} id={} tenant={}", saved.getPermissionCode(), saved.getId(), saved.getTenantId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Permission getById(UUID tenantId, UUID permissionId) {
        return permissionRepository.findById(tenantId, permissionId)
                .orElseThrow(() -> NotFoundException.forEntity("Permission", permissionId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Permission> listByTenant(UUID tenantId, PageQuery query) {
        return permissionRepository.findAllByTenant(tenantId, query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> listByRole(UUID tenantId, UUID roleId) {
        roleRepository.findById(tenantId, roleId)
                .orElseThrow(() -> NotFoundException.forEntity("Role", roleId.toString()));
        return permissionRepository.findByRoleId(tenantId, roleId);
    }

    @Override
    @Transactional
    public Permission update(UUID tenantId, UUID permissionId, UpdatePermissionCommand command) {
        var perm = permissionRepository.findById(tenantId, permissionId)
                .orElseThrow(() -> NotFoundException.forEntity("Permission", permissionId.toString()));

        if (command.permissionName() != null) perm.setPermissionName(command.permissionName());
        if (command.description() != null) perm.setDescription(command.description());
        if (command.active() != null) perm.setActive(command.active());
        perm.setUpdatedAt(Instant.now());

        var saved = permissionRepository.save(perm);
        log.info("Permission updated: id={} tenant={}", saved.getId(), tenantId);
        return saved;
    }

    @Override
    @Transactional
    public void assignToRole(UUID tenantId, UUID roleId, UUID permissionId) {
        roleRepository.findById(tenantId, roleId)
                .orElseThrow(() -> NotFoundException.forEntity("Role", roleId.toString()));
        permissionRepository.findById(tenantId, permissionId)
                .orElseThrow(() -> NotFoundException.forEntity("Permission", permissionId.toString()));

        permissionRepository.assignToRole(tenantId, roleId, permissionId);
        log.info("Permission {} assigned to role {} in tenant {}", permissionId, roleId, tenantId);
    }

    @Override
    @Transactional
    public void removeFromRole(UUID tenantId, UUID roleId, UUID permissionId) {
        permissionRepository.removeFromRole(tenantId, roleId, permissionId);
        log.info("Permission {} removed from role {} in tenant {}", permissionId, roleId, tenantId);
    }

    @Override
    @Transactional
    public void syncRolePermissions(UUID tenantId, UUID roleId, List<UUID> permissionIds) {
        var role = roleRepository.findById(tenantId, roleId)
                .orElseThrow(() -> NotFoundException.forEntity("Role", roleId.toString()));

        // Remove all existing permissions for this role
        permissionRepository.removeAllFromRole(roleId);

        // Collect resource types for Casbin policy generation
        var resourceTypes = new java.util.HashSet<String>();

        // Assign new permissions
        for (var permissionId : permissionIds) {
            var perm = permissionRepository.findById(tenantId, permissionId)
                    .orElseThrow(() -> NotFoundException.forEntity("Permission", permissionId.toString()));
            permissionRepository.assignToRole(tenantId, roleId, permissionId);
            resourceTypes.add(perm.getResourceType() + ":" + perm.getAction());
        }

        log.info("Synced {} permissions to role {} in tenant {}", permissionIds.size(), roleId, tenantId);

        // Auto-generate Casbin policies from permissions
        syncCasbinPoliciesFromPermissions(role.getRoleCode(), resourceTypes);
    }

    /**
     * Maps permission resource types to Casbin obj+act policies and syncs to Redis.
     * Called automatically when permissions are synced to a role.
     */
    private void syncCasbinPoliciesFromPermissions(String roleCode, java.util.Set<String> resourceTypes) {
        // Permission resource type → Casbin obj+act mapping
        var mapping = new java.util.HashMap<String, String[][]>();

        // Customer
        mapping.put("CUSTOMER:GET", new String[][]{
                {"customers", "read"}, {"customers.bank-accounts", "read"},
                {"customers.employment", "read"}, {"reference-data", "read"},
                {"reference-data.occupation", "read"}});
        mapping.put("CUSTOMER:POST", new String[][]{
                {"customers", "create"}, {"customers.bank-accounts", "create"},
                {"customers.employment", "create"}, {"reference-data", "create"},
                {"reference-data.occupation", "create"}});
        mapping.put("CUSTOMER:PUT", new String[][]{
                {"customers", "update"}, {"customers.kyc-status", "update"},
                {"reference-data", "update"},
                {"reference-data.occupation", "update"}});
        mapping.put("CUSTOMER:DELETE", new String[][]{
                {"reference-data", "delete"},
                {"reference-data.occupation", "delete"}});

        // Product
        mapping.put("PRODUCT:GET", new String[][]{
                {"products", "read"}, {"product-categories", "read"},
                {"product-documents", "read"}, {"countries", "read"},
                {"partners", "read"}, {"contract-templates", "read"},
                {"template-types", "read"}, {"approval-condition-fields", "read"},
                {"credit-scoring-fields", "read"}});
        mapping.put("PRODUCT:POST", new String[][]{
                {"products", "create"}, {"products", "manage"},
                {"product-categories", "create"}, {"product-documents", "create"},
                {"product-partners", "create"}, {"partners", "create"},
                {"partners", "manage"}, {"countries", "create"},
                {"contract-templates", "create"}, {"template-types", "create"},
                {"approval-condition-fields", "create"}});
        mapping.put("PRODUCT:PUT", new String[][]{
                {"products", "update"}, {"product-categories", "update"},
                {"product-documents", "update"}, {"product-settings", "update"},
                {"partners", "update"}, {"countries", "update"},
                {"contract-templates", "update"}, {"template-types", "update"},
                {"approval-condition-fields", "update"}});
        mapping.put("PRODUCT:DELETE", new String[][]{
                {"products", "delete"}, {"product-categories", "delete"},
                {"product-documents", "delete"}, {"product-partners", "delete"},
                {"countries", "delete"}, {"contract-templates", "delete"},
                {"template-types", "delete"}, {"approval-condition-fields", "delete"}});

        // Role / Permission / Policy (obj+act for other services + path-based for IDS)
        mapping.put("ROLE:GET", new String[][]{{"roles", "read"}, {"/api/v1/roles/**", "GET"}, {"/api/v1/roles", "GET"}});
        mapping.put("ROLE:POST", new String[][]{{"roles", "*"}, {"/api/v1/roles/**", "*"}, {"/api/v1/roles", "*"}});
        mapping.put("PERMISSION:GET", new String[][]{{"permissions", "read"}, {"/api/v1/permissions/**", "GET"}, {"/api/v1/permissions", "GET"}});
        mapping.put("PERMISSION:POST", new String[][]{{"permissions", "*"}, {"/api/v1/permissions/**", "*"}, {"/api/v1/permissions", "*"}});
        mapping.put("POLICY:GET", new String[][]{{"policies", "read"}, {"/api/v1/policies/**", "GET"}, {"/api/v1/policies", "GET"}});
        mapping.put("POLICY:POST", new String[][]{{"policies", "*"}, {"/api/v1/policies/**", "*"}, {"/api/v1/policies", "*"}});

        // Wallet
        mapping.put("WALLET:GET", new String[][]{{"wallets", "read"}});
        mapping.put("WALLET:POST", new String[][]{{"wallets", "*"}});

        // Risk
        mapping.put("RISK:GET", new String[][]{
                {"risk", "read"}, {"risk.assessment", "read"},
                {"risk.audit", "read"}, {"risk.blacklist", "read"},
                {"risk.credit-scoring", "read"}, {"risk.credit-scoring.field-definitions", "read"},
                {"risk.devices", "read"}, {"risk.entity-status", "read"},
                {"risk.fraud-rules", "read"}, {"risk.lov", "read"},
                {"risk.parameters", "read"}, {"risk.reviews", "read"},
                {"risk.scenarios", "read"}, {"risk.tenant-config", "read"},
                {"risk.thresholds", "read"}, {"fraud.rules", "read"}});
        mapping.put("RISK:POST", new String[][]{
                {"risk", "create"}, {"risk.assessment", "create"}, {"risk.assessment", "manage"},
                {"risk.blacklist", "create"}, {"risk.blacklist", "check"},
                {"risk.credit-scoring", "create"}, {"risk.credit-scoring.field-definitions", "create"},
                {"risk.devices", "create"}, {"risk.entity-status", "manage"},
                {"risk.fraud-rules", "create"}, {"risk.lov", "create"}, {"risk.lov", "manage"},
                {"risk.parameters", "create"}, {"risk.parameters", "manage"},
                {"risk.reviews", "manage"}, {"risk.scenarios", "create"}, {"risk.scenarios", "manage"},
                {"risk.tenant-config", "create"}, {"risk.tenant-config", "manage"},
                {"risk.thresholds", "create"}, {"risk.thresholds", "manage"}});
        mapping.put("RISK:PUT", new String[][]{
                {"risk.credit-scoring.field-definitions", "update"}, {"risk.devices", "update"},
                {"risk.fraud-rules", "update"}, {"risk.lov", "update"},
                {"risk.parameters", "update"}, {"risk.scenarios", "update"},
                {"risk.tenant-config", "update"}, {"risk.thresholds", "update"}});
        mapping.put("RISK:DELETE", new String[][]{
                {"risk.blacklist", "delete"}, {"risk.credit-scoring", "delete"},
                {"risk.credit-scoring.field-definitions", "delete"}, {"risk.devices", "delete"},
                {"risk.fraud-rules", "delete"}});

        // Admin (obj+act for other services + path-based for IDS)
        mapping.put("ADMIN:GET", new String[][]{
                {"employees", "read"}, {"dashboard", "read"},
                {"/api/v1/employees/**", "GET"}, {"/api/v1/employees", "GET"}});
        mapping.put("ADMIN:POST", new String[][]{
                {"employees", "*"}, {"dashboard", "read"},
                {"/api/v1/employees/**", "*"}, {"/api/v1/employees", "*"}});

        // Profile
        mapping.put("PROFILE:GET", new String[][]{
                {"profiles", "read"}, {"profiles.regional", "read"},
                {"profiles.access-tokens", "read"}, {"profiles.me", "read"}});
        mapping.put("PROFILE:POST", new String[][]{
                {"profiles", "*"}, {"profiles.regional", "*"},
                {"profiles.access-tokens", "*"}, {"profiles.me", "update"}});

        // Workflow (legacy — covers onboarding + lending)
        mapping.put("WORKFLOW:GET", new String[][]{
                {"onboarding", "status"}, {"loan-applications", "read"},
                {"loan-applications.tracker", "read"}, {"loans", "read"},
                {"loans.overview", "read"}, {"loans.installments", "read"},
                {"loans.contract", "read"}, {"loans.receipts", "read"},
                {"banks", "read"}, {"purpose-of-finance", "read"},
                {"finance.calculator", "read"}, {"finance.eligibility", "check"}});
        mapping.put("WORKFLOW:POST", new String[][]{
                {"onboarding", "*"}, {"loan-applications", "*"}, {"loans", "*"}});
        mapping.put("WORKFLOW:DELETE", new String[][]{{"loan-applications", "delete"}});

        // Dashboard
        mapping.put("DASHBOARD:GET", new String[][]{{"dashboard", "read"}});

        // KYC
        mapping.put("KYC:GET", new String[][]{
                {"kyc.tahakuk", "verify"}, {"kyc.nafath", "status"},
                {"kyc.yakeen", "verify"}, {"kyc.screening", "check"},
                {"kyc.gosi", "fetch"}});
        mapping.put("KYC:POST", new String[][]{
                {"kyc.tahakuk", "verify"}, {"kyc.nafath", "initiate"},
                {"kyc.nafath", "status"}, {"kyc.yakeen", "verify"},
                {"kyc.screening", "check"}, {"kyc.gosi", "fetch"}});

        // Lending
        mapping.put("LENDING:GET", new String[][]{
                {"loan-applications", "read"}, {"loan-applications.tracker", "read"},
                {"loans", "read"}, {"loans.overview", "read"},
                {"loans.installments", "read"}, {"loans.contract", "read"},
                {"loans.receipts", "read"}, {"banks", "read"},
                {"purpose-of-finance", "read"}, {"eligibility-fields", "read"},
                {"finance.calculator", "read"}, {"finance.eligibility", "check"},
                {"dashboard", "read"}});
        mapping.put("LENDING:POST", new String[][]{
                {"loan-applications", "*"}, {"loan-applications", "create"},
                {"loan-applications", "manage"}, {"loans", "*"},
                {"purpose-of-finance", "create"}, {"eligibility-fields", "create"},
                {"eligibility-fields", "manage"}});
        mapping.put("LENDING:PUT", new String[][]{
                {"loan-applications", "manage"}, {"purpose-of-finance", "update"},
                {"eligibility-fields", "update"}, {"eligibility-fields", "manage"}});
        mapping.put("LENDING:DELETE", new String[][]{
                {"purpose-of-finance", "delete"}, {"eligibility-fields", "delete"},
                {"eligibility-fields", "manage"}});

        // Onboarding
        mapping.put("ONBOARDING:GET", new String[][]{
                {"onboarding", "status"}});
        mapping.put("ONBOARDING:POST", new String[][]{
                {"onboarding", "*"}, {"onboarding", "create"},
                {"onboarding", "update"}, {"onboarding", "manage"}});

        // Middleware
        mapping.put("MIDDLEWARE:GET", new String[][]{
                {"middleware.providers", "read"}, {"middleware.provider-apis", "read"},
                {"middleware.clients", "read"}, {"middleware.client-access", "read"},
                {"middleware.env-configs", "read"}, {"middleware.callbacks", "read"},
                {"middleware.logs", "read"}});
        mapping.put("MIDDLEWARE:POST", new String[][]{
                {"middleware.providers", "*"}, {"middleware.provider-apis", "*"},
                {"middleware.clients", "*"}, {"middleware.client-access", "*"},
                {"middleware.env-configs", "*"}, {"middleware.callbacks", "*"},
                {"middleware.logs", "*"}});

        // PII Vault
        mapping.put("PII:GET", new String[][]{{"pii", "read"}});
        mapping.put("PII:POST", new String[][]{{"pii", "*"}});

        // Bank
        mapping.put("BANK:GET", new String[][]{{"banks", "read"}});

        // Partner
        mapping.put("PARTNER:GET", new String[][]{{"partners", "read"}});
        mapping.put("PARTNER:POST", new String[][]{{"partners", "create"}, {"partners", "manage"}});
        mapping.put("PARTNER:PUT", new String[][]{{"partners", "update"}});

        // Employee
        mapping.put("EMPLOYEE:GET", new String[][]{
                {"employees", "read"}, {"/api/v1/employees/**", "GET"}, {"/api/v1/employees", "GET"}});
        mapping.put("EMPLOYEE:POST", new String[][]{
                {"employees", "create"}, {"/api/v1/employees/**", "POST"}, {"/api/v1/employees", "POST"}});
        mapping.put("EMPLOYEE:PUT", new String[][]{
                {"employees", "update"}, {"/api/v1/employees/**", "PUT"}});
        mapping.put("EMPLOYEE:DELETE", new String[][]{
                {"employees", "delete"}, {"/api/v1/employees/**", "DELETE"}});

        // Fraud
        mapping.put("FRAUD:GET", new String[][]{{"fraud.rules", "read"}});
        mapping.put("FRAUD:PUT", new String[][]{{"fraud.rules", "update"}});

        // LOV — Source of Wealth, Funds, Income, Purpose of Finance, Net Worth Ranges,
        //        Credit Scoring Field Definitions, Approval Condition Fields
        mapping.put("LOV:GET", new String[][]{
                {"reference-data.source-of-wealth", "read"},
                {"reference-data.source-of-funds", "read"},
                {"reference-data.source-of-income", "read"},
                {"reference-data.occupation", "read"},
                {"reference-data.purpose-of-finance", "read"},
                {"reference-data.net-worth-ranges", "read"},
                {"risk.credit-scoring.field-definitions", "read"},
                {"approval-condition-fields", "read"}});
        mapping.put("LOV:POST", new String[][]{
                {"reference-data.source-of-wealth", "create"},
                {"reference-data.source-of-funds", "create"},
                {"reference-data.source-of-income", "create"},
                {"reference-data.occupation", "create"},
                {"reference-data.purpose-of-finance", "create"},
                {"reference-data.net-worth-ranges", "create"},
                {"risk.credit-scoring.field-definitions", "create"},
                {"approval-condition-fields", "create"}});
        mapping.put("LOV:PUT", new String[][]{
                {"reference-data.source-of-wealth", "update"},
                {"reference-data.source-of-funds", "update"},
                {"reference-data.source-of-income", "update"},
                {"reference-data.occupation", "update"},
                {"reference-data.purpose-of-finance", "update"},
                {"reference-data.net-worth-ranges", "update"},
                {"risk.credit-scoring.field-definitions", "update"},
                {"approval-condition-fields", "update"}});
        mapping.put("LOV:DELETE", new String[][]{
                {"reference-data.source-of-wealth", "delete"},
                {"reference-data.source-of-funds", "delete"},
                {"reference-data.source-of-income", "delete"},
                {"reference-data.occupation", "delete"},
                {"reference-data.purpose-of-finance", "delete"},
                {"reference-data.net-worth-ranges", "delete"},
                {"risk.credit-scoring.field-definitions", "delete"},
                {"approval-condition-fields", "delete"}});

        // Remove existing Casbin policies for this role first
        var existing = policyEnforcer.getPoliciesForRole(roleCode);
        for (var policy : existing) {
            policyEnforcer.removePolicy(policy.get(0), policy.get(1), policy.get(2));
        }

        // Add new policies based on assigned permissions
        int count = 0;
        for (var resourceType : resourceTypes) {
            var policies = mapping.get(resourceType);
            if (policies != null) {
                for (var p : policies) {
                    policyEnforcer.addPolicy(roleCode, p[0], p[1]);
                    count++;
                }
            }
        }

        redisSyncService.syncPoliciesToRedis();
        log.info("Auto-synced {} Casbin policies for role '{}' from {} permissions",
                count, roleCode, resourceTypes.size());
    }
}
