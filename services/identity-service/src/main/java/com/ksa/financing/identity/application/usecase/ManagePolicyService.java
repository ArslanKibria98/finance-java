package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.port.in.ManagePolicyUseCase;
import com.ksa.financing.identity.domain.port.out.PolicyEnforcerPort;
import com.ksa.financing.identity.infrastructure.casbin.PolicyRedisSyncService;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagePolicyService implements ManagePolicyUseCase {

    private final PolicyEnforcerPort policyEnforcer;
    private final PolicyRedisSyncService redisSyncService;

    @Override
    public void addPolicy(String role, String resource, String action) {
        boolean added = policyEnforcer.addPolicy(role, resource, action);
        if (!added) {
            throw new BusinessException(
                    ErrorCodes.Identity.POLICY_ALREADY_EXISTS,
                    "Policy rule already exists for this role and resource");
        }
        redisSyncService.syncPoliciesToRedis();
    }

    @Override
    public void removePolicy(String role, String resource, String action) {
        boolean removed = policyEnforcer.removePolicy(role, resource, action);
        if (!removed) {
            throw new BusinessException(
                    ErrorCodes.Identity.POLICY_NOT_FOUND,
                    "Policy rule not found");
        }
        redisSyncService.syncPoliciesToRedis();
    }

    @Override
    public List<PolicyRule> listPolicies() {
        return policyEnforcer.getAllPolicies().stream()
                .map(p -> new PolicyRule(p.get(0), p.get(1), p.get(2)))
                .toList();
    }

    @Override
    public List<PolicyRule> listPoliciesForRole(String role) {
        return policyEnforcer.getPoliciesForRole(role).stream()
                .map(p -> new PolicyRule(p.get(0), p.get(1), p.get(2)))
                .toList();
    }

    @Override
    public void addRoleGrouping(String user, String role) {
        policyEnforcer.addRoleForUser(user, role);
        log.info("Role grouping added: user={}, role={}", user, role);
    }

    @Override
    public void removeRoleGrouping(String user, String role) {
        policyEnforcer.removeRoleForUser(user, role);
        log.info("Role grouping removed: user={}, role={}", user, role);
    }

    @Override
    public List<String> getRolesForUser(String user) {
        return policyEnforcer.getRolesForUser(user);
    }

    @Override
    public void reloadPolicies() {
        policyEnforcer.reloadPolicy();
        redisSyncService.syncPoliciesToRedis();
        log.info("Casbin policies reloaded and synced to Redis");
    }
}
