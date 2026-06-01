package com.ksa.financing.onboarding.shared.downstream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST client for the four internal endpoints that onboarding-workflow-service
 * calls to persist a freshly-completed onboarding into the real downstream services:
 *
 * <ul>
 *   <li>{@code POST {globalProfileUrl}/internal/profiles} — zero-PII customer 360 row</li>
 *   <li>{@code POST {piiVaultUrl}/internal/pii/individual} — encrypted PII (only when present)</li>
 *   <li>{@code POST {customerServiceUrl}/internal/customers/lead} — LEAD customer row</li>
 *   <li>{@code POST {walletServiceUrl}/internal/wallets} — wallet record</li>
 * </ul>
 *
 * <p>Each downstream call returns its own ID. If a call fails the activity that uses
 * this client surfaces the error so Temporal can retry per its RetryOptions policy.</p>
 */
@Component
public class OnboardingDownstreamClient {

    private static final Logger log = LoggerFactory.getLogger(OnboardingDownstreamClient.class);

    private final RestTemplate restTemplate;
    private final String customerServiceUrl;
    private final String walletServiceUrl;
    private final String piiVaultUrl;
    private final String globalProfileUrl;
    private final String identityServiceUrl;

    public OnboardingDownstreamClient(RestTemplate restTemplate,
                                      @Value("${app.services.customer-service-url:http://localhost:8084}") String customerServiceUrl,
                                      @Value("${app.services.wallet-service-url:http://localhost:8088}") String walletServiceUrl,
                                      @Value("${app.services.pii-vault-url:http://localhost:8086}") String piiVaultUrl,
                                      @Value("${app.services.global-profile-url:http://localhost:8085}") String globalProfileUrl,
                                      @Value("${app.services.identity-url:http://localhost:8083}") String identityServiceUrl) {
        this.restTemplate = restTemplate;
        this.customerServiceUrl = stripSlash(customerServiceUrl);
        this.walletServiceUrl = stripSlash(walletServiceUrl);
        this.piiVaultUrl = stripSlash(piiVaultUrl);
        this.globalProfileUrl = stripSlash(globalProfileUrl);
        this.identityServiceUrl = stripSlash(identityServiceUrl);
    }

    // -------------------------------------------------------------------------
    // global-profile-service
    // -------------------------------------------------------------------------

    /**
     * Creates a zero-PII customer 360 row and returns the assigned {@code globalUid}
     * plus the email/mobile hashes derived by global-profile-service.
     */
    @SuppressWarnings("unchecked")
    public GlobalProfileResult createGlobalProfile(String email, String mobile, String primaryCountryCode) {
        String url = globalProfileUrl + "/internal/profiles";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("mobile", mobile);
        body.put("primaryCountryCode", primaryCountryCode != null ? primaryCountryCode : "SA");

        ResponseEntity<Map> response = postOrFail(url, asJson(body), "create global profile");
        Map<String, Object> data = unwrap(response.getBody());
        if (data == null || data.get("globalUid") == null) {
            throw new IllegalStateException("global-profile returned empty body");
        }
        UUID globalUid = UUID.fromString(data.get("globalUid").toString());
        String emailHash = (String) data.get("emailHash");
        String mobileHash = (String) data.get("mobileHash");
        log.info("global-profile created: globalUid={}", globalUid);
        return new GlobalProfileResult(globalUid, emailHash, mobileHash);
    }

    // -------------------------------------------------------------------------
    // pii-vault-service
    // -------------------------------------------------------------------------

    /**
     * Stores encrypted PII keyed by {@code globalUid}. Returns the recorded globalUid
     * (for convenience / audit) — pii-vault decides nothing else useful for the caller.
     */
    @SuppressWarnings("unchecked")
    public UUID storePii(UUID globalUid,
                         String nationalId, String nationalIdType,
                         String fullName, String firstName, String middleName, String lastName,
                         String fullNameAr,
                         String dateOfBirth, String gender,
                         String nationalityCode, String mobile, String email,
                         String countryCode) {
        String url = piiVaultUrl + "/internal/pii/individual";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("globalUid", globalUid);
        body.put("nationalId", nationalId);
        body.put("nationalIdType", nationalIdType);
        body.put("fullName", fullName);
        body.put("firstName", firstName);
        body.put("middleName", middleName);
        body.put("lastName", lastName);
        body.put("fullNameAr", fullNameAr);
        body.put("dateOfBirth", dateOfBirth);
        body.put("gender", gender);
        body.put("nationalityCode", nationalityCode);
        body.put("mobile", mobile);
        body.put("email", email);
        body.put("countryCode", countryCode);

        ResponseEntity<Map> response = postOrFail(url, asJson(body), "store PII vault");
        Map<String, Object> data = unwrap(response.getBody());
        log.info("pii-vault stored: globalUid={}", globalUid);
        return globalUid;
    }

    // -------------------------------------------------------------------------
    // customer-service
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code LEAD} customer in customer-service. Returns the assigned
     * customerId + CIF number.
     */
    @SuppressWarnings("unchecked")
    public LeadCustomerResult createLeadCustomer(UUID tenantId, UUID keycloakUserId,
                                                 String flowType,
                                                 String email, String mobileNumber,
                                                 UUID globalUid,
                                                 String nationalId, String nationalIdType,
                                                 String firstName, String middleName, String lastName,
                                                 String dateOfBirth, String gender,
                                                 String nationality, String residencyType,
                                                 String idempotencyKey) {
        String url = customerServiceUrl + "/internal/customers/lead";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenantId", tenantId);
        body.put("keycloakUserId", keycloakUserId);
        body.put("flowType", flowType);
        body.put("email", email);
        body.put("mobileNumber", mobileNumber);
        body.put("globalUid", globalUid);
        body.put("nationalId", nationalId);
        body.put("nationalIdType", nationalIdType);
        body.put("firstName", firstName);
        body.put("middleName", middleName);
        body.put("lastName", lastName);
        body.put("dateOfBirth", dateOfBirth);
        body.put("gender", gender);
        body.put("nationality", nationality);
        body.put("residencyType", residencyType);
        body.put("idempotencyKey", idempotencyKey);

        ResponseEntity<Map> response = postOrFail(url, asJson(body), "create LEAD customer");
        Map<String, Object> data = unwrap(response.getBody());
        if (data == null || data.get("customerId") == null) {
            throw new IllegalStateException("customer-service returned empty body");
        }
        UUID customerId = UUID.fromString(data.get("customerId").toString());
        String cifNumber = (String) data.get("cifNumber");
        String lifecycleStage = (String) data.getOrDefault("lifecycleStage", "LEAD");
        log.info("customer-service LEAD created: customerId={} cif={}", customerId, cifNumber);
        return new LeadCustomerResult(customerId, cifNumber, lifecycleStage);
    }

    // -------------------------------------------------------------------------
    // wallet-service
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public WalletCreatedResult createWallet(UUID tenantId, UUID customerId, String currency) {
        return createWallet(tenantId, customerId, currency, null);
    }

    @SuppressWarnings("unchecked")
    public WalletCreatedResult createWallet(UUID tenantId, UUID customerId, String currency, String displayName) {
        String url = walletServiceUrl + "/internal/wallets";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenantId", tenantId);
        body.put("customerId", customerId);
        body.put("currency", currency != null ? currency : "SAR");
        if (displayName != null && !displayName.isBlank()) {
            body.put("displayName", displayName.trim());
        }

        ResponseEntity<Map> response = postOrFail(url, asJson(body), "create wallet");
        Map<String, Object> data = unwrap(response.getBody());
        if (data == null || data.get("walletId") == null) {
            throw new IllegalStateException("wallet-service returned empty body");
        }
        UUID walletId = UUID.fromString(data.get("walletId").toString());
        String walletNumber = (String) data.get("walletNumber");
        String iban = (String) data.get("iban");
        log.info("wallet-service wallet created: walletId={}", walletId);
        return new WalletCreatedResult(walletId, walletNumber, iban);
    }

    // -------------------------------------------------------------------------
    // identity-service
    // -------------------------------------------------------------------------

    /**
     * Upserts a {@code user_identity_mapping} row so identity-service can resolve
     * the user by mobile (and customerId / keycloakUserId). Required because
     * Canada / Foreign / Guest flows create the Keycloak user directly via the
     * admin REST client instead of going through {@code /auth/register}, which
     * leaves the mapping table empty and breaks every mobile-based lookup
     * (wallet recipient resolution, login-by-mobile, etc.).
     *
     * <p>Failure is logged but NOT thrown — we don't want a downstream identity
     * outage to roll back a completed onboarding when Keycloak user + customer
     * row already exist. A background reconciler can replay missed mappings.</p>
     */
    public void registerIdentityMapping(UUID tenantId,
                                        UUID keycloakUserId,
                                        String keycloakRealm,
                                        String email,
                                        String mobileNumber,
                                        UUID customerId) {
        if (identityServiceUrl == null || keycloakUserId == null || tenantId == null) {
            log.warn("Skipping identity mapping upsert — missing required fields");
            return;
        }
        String url = identityServiceUrl + "/internal/users/register-onboarded";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenantId", tenantId);
        body.put("keycloakUserId", keycloakUserId);
        if (keycloakRealm != null) body.put("keycloakRealm", keycloakRealm);
        if (email != null) body.put("keycloakUsername", email);
        if (mobileNumber != null) body.put("mobileNumber", mobileNumber);
        if (customerId != null) body.put("internalCustomerId", customerId);

        try {
            restTemplate.postForEntity(url, asJson(body), Map.class);
            log.info("identity-service mapping upserted: keycloakUserId={} customerId={} mobile=****{}",
                    keycloakUserId, customerId, tail(mobileNumber));
        } catch (Exception e) {
            log.error("identity-service mapping upsert FAILED (non-fatal): keycloakUserId={} err={}",
                    keycloakUserId, e.getMessage());
        }
    }

    private static String tail(String value) {
        if (value == null || value.length() < 4) return value;
        return value.substring(value.length() - 4);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpEntity<Map<String, Object>> asJson(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Map<String, Object> envelope) {
        if (envelope == null) return null;
        Object dataNode = envelope.get("data");
        if (dataNode instanceof Map<?, ?> data) {
            return (Map<String, Object>) data;
        }
        return envelope;
    }

    private static String stripSlash(String url) {
        return url == null ? null : url.replaceAll("/+$", "");
    }

    /**
     * Wraps an inter-service POST so any 4xx/5xx response is re-thrown with the
     * downstream's structured error message ({@code code} + {@code message}) instead
     * of the raw Spring stack-trace. Lets workflow {@code failureReason} surface the
     * real problem (e.g. "Invalid value for residencyType: 'XYZ'") instead of a
     * generic "500 Internal Server Error".
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private ResponseEntity<Map> postOrFail(String url, HttpEntity<Map<String, Object>> request, String operation) {
        try {
            return restTemplate.postForEntity(url, request, Map.class);
        } catch (HttpStatusCodeException e) {
            String detail = extractErrorDetail(e.getResponseBodyAsString());
            throw new IllegalStateException(operation + " failed (" + e.getStatusCode().value() + "): " + detail, e);
        }
    }

    private static final ObjectMapper ERROR_MAPPER = new ObjectMapper();

    @SuppressWarnings("unchecked")
    private static String extractErrorDetail(String body) {
        if (body == null || body.isBlank()) return "empty response body";
        try {
            Map<String, Object> parsed = ERROR_MAPPER.readValue(body, Map.class);
            Object code = parsed.get("code");
            Object message = parsed.get("message");
            if (message != null) {
                return (code != null ? code + " — " : "") + message;
            }
        } catch (Exception ignored) {
            // fall through — return raw body
        }
        return body.length() > 500 ? body.substring(0, 500) + "..." : body;
    }

    // -------------------------------------------------------------------------
    // Result records
    // -------------------------------------------------------------------------

    public record GlobalProfileResult(UUID globalUid, String emailHash, String mobileHash) {}

    public record LeadCustomerResult(UUID customerId, String cifNumber, String lifecycleStage) {}

    public record WalletCreatedResult(UUID walletId, String walletNumber, String iban) {}
}
