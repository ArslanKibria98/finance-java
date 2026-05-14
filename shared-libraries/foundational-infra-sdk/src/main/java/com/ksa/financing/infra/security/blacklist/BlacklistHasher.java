package com.ksa.financing.infra.security.blacklist;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes raw blacklist values with SHA-256 → lower-case hex.
 * Used as Redis key suffix to avoid storing PII in keys.
 */
public final class BlacklistHasher {

    private BlacklistHasher() {}

    public static String sha256(String value) {
        if (value == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String normalizeMobile(String mobile) {
        if (mobile == null) return null;
        return mobile.replaceAll("[\\s\\-()]", "").trim();
    }
}
