package com.ksa.financing.infra.authorization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Represents a single Casbin policy rule cached in Redis.
 * Stored as JSON inside a Redis hash: "casbin:policies:{role}".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyRecord(
        String sub,     // subject (role): "admin", "csa", "customer"
        String obj,     // object (resource): "customers", "products", "wallets"
        String act,     // action: "create", "read", "update", "delete", "*"
        String effect   // "ALLOW" or "DENY"
) implements Serializable {

    public boolean matches(String targetObj, String targetAct) {
        boolean objMatch = "*".equals(this.obj) || this.obj.equals(targetObj);
        boolean actMatch = "*".equals(this.act) || this.act.equals(targetAct);
        return objMatch && actMatch;
    }

    public boolean isAllow() {
        return "ALLOW".equals(effect);
    }
}
