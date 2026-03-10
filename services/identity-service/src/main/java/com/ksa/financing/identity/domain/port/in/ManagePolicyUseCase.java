package com.ksa.financing.identity.domain.port.in;

import java.util.List;

public interface ManagePolicyUseCase {

    record PolicyRule(String role, String resource, String action) {}

    void addPolicy(String role, String resource, String action);
    void removePolicy(String role, String resource, String action);
    List<PolicyRule> listPolicies();
    List<PolicyRule> listPoliciesForRole(String role);
    void addRoleGrouping(String user, String role);
    void removeRoleGrouping(String user, String role);
    List<String> getRolesForUser(String user);
    void reloadPolicies();
}
