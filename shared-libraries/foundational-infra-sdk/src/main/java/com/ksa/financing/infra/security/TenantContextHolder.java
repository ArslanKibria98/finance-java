package com.ksa.financing.infra.security;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class TenantContextHolder {

    private static final ThreadLocal<String> TENANT_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> USER_ID = new InheritableThreadLocal<>();

    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
        log.trace("Tenant context set: {}", tenantId);
    }

    public static String getTenantId() {
        return TENANT_ID.get();
    }

    public static void setUserId(String userId) {
        USER_ID.set(userId);
        log.trace("User context set: {}", userId);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        log.trace("Tenant and user context cleared");
    }
}
