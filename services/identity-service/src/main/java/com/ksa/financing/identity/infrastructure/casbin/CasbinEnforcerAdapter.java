package com.ksa.financing.identity.infrastructure.casbin;

import com.ksa.financing.identity.domain.port.out.PolicyEnforcerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CasbinEnforcerAdapter implements PolicyEnforcerPort {

    private final Enforcer enforcer;

    @Override
    public boolean enforce(String subject, String resource, String action) {
        boolean allowed = enforcer.enforce(subject, resource, action);
        log.debug("Casbin enforce: sub={}, obj={}, act={} => {}", subject, resource, action, allowed);
        return allowed;
    }

    @Override
    public boolean addPolicy(String role, String resource, String action) {
        boolean added = enforcer.addPolicy(role, resource, action);
        if (added) {
            enforcer.savePolicy();
            log.info("Policy added: role={}, resource={}, action={}", role, resource, action);
        }
        return added;
    }

    @Override
    public boolean removePolicy(String role, String resource, String action) {
        boolean removed = enforcer.removePolicy(role, resource, action);
        if (removed) {
            enforcer.savePolicy();
            log.info("Policy removed: role={}, resource={}, action={}", role, resource, action);
        }
        return removed;
    }

    @Override
    public List<List<String>> getAllPolicies() {
        return enforcer.getPolicy();
    }

    @Override
    public List<List<String>> getPoliciesForRole(String role) {
        return enforcer.getFilteredPolicy(0, role);
    }

    @Override
    public boolean addRoleForUser(String user, String role) {
        boolean added = enforcer.addGroupingPolicy(user, role);
        if (added) {
            enforcer.savePolicy();
            log.info("Role grouping added: user={}, role={}", user, role);
        }
        return added;
    }

    @Override
    public boolean removeRoleForUser(String user, String role) {
        boolean removed = enforcer.deleteRoleForUser(user, role);
        if (removed) {
            enforcer.savePolicy();
            log.info("Role grouping removed: user={}, role={}", user, role);
        }
        return removed;
    }

    @Override
    public List<String> getRolesForUser(String user) {
        return enforcer.getRolesForUser(user);
    }

    @Override
    public void reloadPolicy() {
        enforcer.loadPolicy();
        log.info("Casbin policies reloaded from database, total: {}", enforcer.getPolicy().size());
    }
}
