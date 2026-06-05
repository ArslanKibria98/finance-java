package com.ksa.financing.identity.domain.port.out;

import java.util.List;

public interface PolicyEnforcerPort {

    boolean enforce(String subject, String resource, String action);

    boolean addPolicy(String role, String resource, String action);
    boolean removePolicy(String role, String resource, String action);

    List<List<String>> getAllPolicies();
    List<List<String>> getPoliciesForRole(String role);

    boolean addRoleForUser(String user, String role);
    boolean removeRoleForUser(String user, String role);
    List<String> getRolesForUser(String user);

    void reloadPolicy();
}
