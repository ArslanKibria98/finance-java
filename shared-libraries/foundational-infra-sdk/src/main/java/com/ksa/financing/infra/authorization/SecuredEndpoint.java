package com.ksa.financing.infra.authorization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as a secured endpoint with Casbin-based authorization.
 * The middleware filter reads this annotation, extracts obj + act,
 * then checks Redis for matching policies against the user's role (subject).
 *
 * <p>Example usage:</p>
 * <pre>
 * {@literal @}SecuredEndpoint(obj = "customers", act = "create")
 * {@literal @}PostMapping
 * public ResponseEntity&lt;?&gt; createCustomer(...) { }
 * </pre>
 *
 * <p>Casbin policy rule in DB: ('p', 'admin', 'customers', 'create')</p>
 * <p>Redis key: "casbin:policies:admin" → contains {obj:"customers", act:"create", effect:"ALLOW"}</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecuredEndpoint {

    /**
     * The resource object (e.g., "customers", "products", "wallets").
     * Maps to Casbin policy v1 (obj).
     */
    String obj();

    /**
     * The action on the resource (e.g., "create", "read", "update", "delete", "manage").
     * Maps to Casbin policy v2 (act).
     */
    String act();
}
