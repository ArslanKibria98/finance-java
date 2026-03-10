package com.ksa.financing.middleware.domain.model;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class ApiClient {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SECRET_KEY_BYTES = 32;

    private UUID id;
    private UUID tenantId;
    private String name;
    private String code;
    private String description;
    private String secretKey;
    private String callbackUrl;
    private List<String> ipWhitelist;
    private ClientStatus status;
    private AccessEnvironment environment;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private Instant deletedAt;
    private int version;

    public ApiClient() {}

    public static ApiClient create(UUID tenantId, String name, String code, String description,
                                    String callbackUrl, AccessEnvironment environment, UUID createdBy) {
        var client = new ApiClient();
        client.tenantId = tenantId;
        client.name = name;
        client.code = code;
        client.description = description;
        client.secretKey = generateSecret();
        client.callbackUrl = callbackUrl;
        client.status = ClientStatus.ACTIVE;
        client.environment = environment;
        client.createdBy = createdBy;
        return client;
    }

    public String regenerateSecretKey() {
        this.secretKey = generateSecret();
        return this.secretKey;
    }

    private static String generateSecret() {
        byte[] bytes = new byte[SECRET_KEY_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.status = ClientStatus.INACTIVE;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public String getSecretKey() { return secretKey; }
    public String getCallbackUrl() { return callbackUrl; }
    public List<String> getIpWhitelist() { return ipWhitelist; }
    public ClientStatus getStatus() { return status; }
    public AccessEnvironment getEnvironment() { return environment; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getDeletedAt() { return deletedAt; }
    public int getVersion() { return version; }

    // Setters for mapper
    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setName(String name) { this.name = name; }
    public void setCode(String code) { this.code = code; }
    public void setDescription(String description) { this.description = description; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    public void setIpWhitelist(List<String> ipWhitelist) { this.ipWhitelist = ipWhitelist; }
    public void setStatus(ClientStatus status) { this.status = status; }
    public void setEnvironment(AccessEnvironment environment) { this.environment = environment; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public void setVersion(int version) { this.version = version; }
}
