package com.ksa.financing.onboarding.shared.activity.impl;

import com.ksa.financing.onboarding.shared.activity.OnboardingProfileActivity;
import com.ksa.financing.onboarding.shared.audit.OnboardingSessionRecorder;
import com.ksa.financing.onboarding.shared.audit.OnboardingSessionRecorder.RefType;
import com.ksa.financing.onboarding.shared.downstream.OnboardingDownstreamClient;
import com.ksa.financing.onboarding.shared.keycloak.OnboardingKeycloakClient;
import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Onboarding profile activity used by every workflow.
 *
 * <p><b>Phase 2B reality:</b>
 * <ul>
 *   <li>Keycloak user + token — real Keycloak admin REST</li>
 *   <li>PIN — bcrypt-hashed and stored as Keycloak user attribute {@code pin_hash}</li>
 *   <li>{@code onboarding_complete}/{@code onboarding_flow}/{@code mobile_number} — Keycloak attrs</li>
 *   <li><b>Customer profile</b> — real REST call to customer-service /internal/customers/lead</li>
 *   <li><b>Wallet</b> — real REST call to wallet-service /internal/wallets</li>
 *   <li><b>Global profile</b> — real call to global-profile-service /internal/profiles</li>
 *   <li><b>PII vault</b> — real call to pii-vault-service when confirmed PII is available</li>
 * </ul>
 */
public class OnboardingProfileActivityImpl implements OnboardingProfileActivity {

    private static final Logger log = LoggerFactory.getLogger(OnboardingProfileActivityImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder PIN_ENCODER = new BCryptPasswordEncoder();

    private final OnboardingKeycloakClient keycloak;
    private final OnboardingSessionRecorder recorder;
    private final OnboardingDownstreamClient downstream;
    private final String customerRole;

    public OnboardingProfileActivityImpl(OnboardingKeycloakClient keycloak,
                                         OnboardingSessionRecorder recorder,
                                         OnboardingDownstreamClient downstream,
                                         String customerRole) {
        this.keycloak = keycloak;
        this.recorder = recorder;
        this.downstream = downstream;
        this.customerRole = customerRole;
    }

    @Override
    public KeycloakUserResult createKeycloakUser(String email, String mobileNumber,
                                                 String firstName, String lastName) {
        String workflowId = currentWorkflowId();
        try {
            String tempPassword = generateTempPassword();
            String keycloakUserId = keycloak.createUserWithPassword(email, tempPassword, firstName, lastName);
            keycloak.assignRealmRole(keycloakUserId, customerRole);
            if (mobileNumber != null && !mobileNumber.isBlank()) {
                keycloak.setUserAttributes(keycloakUserId, Map.of("mobile_number", mobileNumber));
            }
            log.info("Keycloak user provisioned: kcUserId={} email={}", keycloakUserId, email);
            recorder.recordExternalRef(workflowId, RefType.KEYCLOAK, keycloakUserId,
                    Map.of("event", "user_created"));
            return new KeycloakUserResult(true, keycloakUserId, tempPassword, null);
        } catch (Exception e) {
            log.error("Failed to provision Keycloak user for {}: {}", email, e.getMessage(), e);
            recorder.recordStep(workflowId, "KEYCLOAK_USER_FAILED", null, e.getMessage());
            return new KeycloakUserResult(false, null, null, e.getMessage());
        }
    }

    @Override
    public TokenResult issueTokenForUser(String keycloakUserId, String email, String password) {
        String workflowId = currentWorkflowId();
        try {
            OnboardingKeycloakClient.TokenPayload token = keycloak.passwordGrant(email, password);
            log.info("Keycloak token issued for kcUserId={} email={}", keycloakUserId, email);
            recorder.recordStep(workflowId, "TOKEN_ISSUED",
                    Map.of("kcUserId", keycloakUserId, "tokenType", token.tokenType(),
                            "expiresIn", token.expiresIn()), null);
            return new TokenResult(true, token.accessToken(), token.refreshToken(),
                    token.expiresIn(), token.tokenType(), null);
        } catch (Exception e) {
            log.error("Failed to issue Keycloak token for {}: {}", email, e.getMessage(), e);
            recorder.recordStep(workflowId, "TOKEN_ISSUE_FAILED", null, e.getMessage());
            return new TokenResult(false, null, null, 0L, "Bearer", e.getMessage());
        }
    }

    @Override
    public CustomerProfileResult createCustomerProfile(String email, String mobileNumber,
                                                       String keycloakUserId,
                                                       Map<String, Object> confirmedDocumentData,
                                                       String tenantId) {
        String workflowId = currentWorkflowId();
        String flowType = deriveFlowType(workflowId);
        try {
            UUID tenantUuid = parseUuid(tenantId);
            UUID kcUserUuid = parseUuid(keycloakUserId);
            String countryCode = pickCountryCode(confirmedDocumentData, flowType);

            // 1. Global profile (zero-PII customer 360) — always
            var gp = downstream.createGlobalProfile(email, mobileNumber, countryCode);
            recorder.recordExternalRef(workflowId, RefType.GLOBAL_PROFILE, gp.globalUid().toString(),
                    Map.of("emailHash", String.valueOf(gp.emailHash()),
                           "mobileHash", String.valueOf(gp.mobileHash())));

            // 2. PII vault (encrypted PII) — only when there's confirmed PII to vault
            boolean hasPii = confirmedDocumentData != null && !confirmedDocumentData.isEmpty();
            if (hasPii) {
                String firstName = strOf(confirmedDocumentData.get("givenName"));
                String lastName  = strOf(confirmedDocumentData.get("surname"));
                String fullName  = (firstName != null ? firstName : "")
                        + (lastName != null ? " " + lastName : "");
                String dob       = strOf(confirmedDocumentData.get("dateOfBirth"));
                String nat       = strOf(confirmedDocumentData.get("nationality"));
                String docNumber = firstNonNull(strOf(confirmedDocumentData.get("documentNumber")),
                                                strOf(confirmedDocumentData.get("passportNumber")));
                String docType   = confirmedDocumentData.containsKey("passportNumber") ? "PASSPORT" : "ID";

                downstream.storePii(gp.globalUid(),
                        docNumber, docType,
                        fullName.trim(), firstName, null, lastName,
                        null, dob, null,
                        nat, mobileNumber, email, countryCode);
                recorder.recordExternalRef(workflowId, RefType.PII_VAULT, gp.globalUid().toString(),
                        Map.of("docType", docType));
            } else {
                log.info("Skipping pii-vault — no confirmed PII (guest flow): workflowId={}", workflowId);
            }

            // 3. customer-service LEAD
            String firstName = hasPii ? strOf(confirmedDocumentData.get("givenName")) : null;
            String lastName  = hasPii ? strOf(confirmedDocumentData.get("surname"))   : null;
            String dob       = hasPii ? strOf(confirmedDocumentData.get("dateOfBirth")) : null;
            String nat       = hasPii ? strOf(confirmedDocumentData.get("nationality")) : null;
            String docNumber = hasPii
                    ? firstNonNull(strOf(confirmedDocumentData.get("documentNumber")),
                                   strOf(confirmedDocumentData.get("passportNumber")))
                    : null;
            String docType   = hasPii
                    ? (confirmedDocumentData.containsKey("passportNumber") ? "PASSPORT" : "ID")
                    : null;
            String idempotencyKey = "onb-" + workflowId;

            var lead = downstream.createLeadCustomer(tenantUuid, kcUserUuid, flowType,
                    email, mobileNumber, gp.globalUid(),
                    docNumber, docType,
                    firstName, null, lastName,
                    dob, null,
                    nat, null,
                    idempotencyKey);
            recorder.recordExternalRef(workflowId, RefType.CUSTOMER, lead.customerId().toString(),
                    Map.of("cifNumber", String.valueOf(lead.cifNumber()),
                           "lifecycleStage", lead.lifecycleStage(),
                           "flowType", flowType));

            // 4. identity-service mapping (so mobile / customerId lookups resolve)
            downstream.registerIdentityMapping(tenantUuid, kcUserUuid,
                    keycloak.realm(), email, mobileNumber, lead.customerId());

            return new CustomerProfileResult(true, lead.customerId().toString(),
                    gp.globalUid().toString(), null);
        } catch (Exception e) {
            log.error("Failed to create LEAD customer profile for {}: {}", email, e.getMessage(), e);
            recorder.recordStep(workflowId, "CUSTOMER_PROFILE_FAILED", null, e.getMessage());
            return new CustomerProfileResult(false, null, null, e.getMessage());
        }
    }

    @Override
    public WalletResult createWallet(String customerId, String tenantId) {
        return createWalletWithName(customerId, tenantId, null);
    }

    @Override
    public WalletResult createWalletWithName(String customerId, String tenantId, String displayName) {
        String workflowId = currentWorkflowId();
        try {
            var w = downstream.createWallet(parseUuid(tenantId), parseUuid(customerId), "SAR", displayName);
            recorder.recordExternalRef(workflowId, RefType.WALLET, w.walletId().toString(),
                    Map.of("walletNumber", String.valueOf(w.walletNumber()),
                           "iban", String.valueOf(w.iban()),
                           "displayName", displayName != null ? displayName : ""));
            return new WalletResult(true, w.walletId().toString(), null);
        } catch (Exception e) {
            log.error("Failed to create wallet for customerId={}: {}", customerId, e.getMessage(), e);
            recorder.recordStep(workflowId, "WALLET_CREATE_FAILED", null, e.getMessage());
            return new WalletResult(false, null, e.getMessage());
        }
    }

    @Override
    public void persistPin(String keycloakUserId, String pin) {
        String workflowId = currentWorkflowId();
        try {
            String pinHash = PIN_ENCODER.encode(pin);
            keycloak.setUserAttributes(keycloakUserId, Map.of("pin_hash", pinHash));
            log.info("PIN bcrypt-hashed and stored on Keycloak user kcUserId={}", keycloakUserId);
            recorder.recordStep(workflowId, "PIN_PERSISTED",
                    Map.of("kcUserId", keycloakUserId, "storage", "keycloak_attribute"), null);
        } catch (Exception e) {
            log.error("Failed to persist PIN for kcUserId={}: {}", keycloakUserId, e.getMessage(), e);
            recorder.recordStep(workflowId, "PIN_PERSIST_FAILED", null, e.getMessage());
            throw new RuntimeException("PIN persistence failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void completeOnboarding(String keycloakUserId, String flowType, String mobileNumber,
                                   boolean onboardingComplete) {
        String workflowId = currentWorkflowId();
        try {
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("onboarding_complete", String.valueOf(onboardingComplete));
            attrs.put("onboarding_flow", flowType);
            if (mobileNumber != null && !mobileNumber.isBlank()) {
                attrs.put("mobile_number", mobileNumber);
            }
            keycloak.setUserAttributes(keycloakUserId, attrs);
            log.info("Onboarding completion flags set on Keycloak: kcUserId={} flow={} complete={}",
                    keycloakUserId, flowType, onboardingComplete);
            recorder.recordSessionComplete(workflowId, onboardingComplete);
        } catch (Exception e) {
            log.error("Failed to complete onboarding for kcUserId={}: {}",
                    keycloakUserId, e.getMessage(), e);
            recorder.recordStep(workflowId, "COMPLETION_FAILED", null, e.getMessage());
            throw new RuntimeException("Onboarding completion failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String generateTempPassword() {
        byte[] buf = new byte[18];
        RANDOM.nextBytes(buf);
        return "Ob!" + Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String currentWorkflowId() {
        try {
            return Activity.getExecutionContext().getInfo().getWorkflowId();
        } catch (Exception e) {
            return "unknown-workflow";
        }
    }

    private static String deriveFlowType(String workflowId) {
        if (workflowId == null) return "UNKNOWN";
        if (workflowId.startsWith("canada-onboarding-")) return "CANADA";
        if (workflowId.startsWith("foreign-onboarding-")) return "FOREIGN";
        if (workflowId.startsWith("guest-onboarding-")) return "GUEST";
        if (workflowId.startsWith("onboarding-")) return "KSA";
        return "UNKNOWN";
    }

    private static String pickCountryCode(Map<String, Object> data, String flowType) {
        if (data != null) {
            Object res = data.get("residentialCountry");
            if (res != null) return res.toString();
            Object nat = data.get("nationality");
            if (nat != null) return nat.toString();
        }
        return switch (flowType) {
            case "CANADA"  -> "CA";
            case "FOREIGN" -> "CA";
            case "GUEST"   -> "SA";
            case "KSA"     -> "SA";
            default        -> "SA";
        };
    }

    private static UUID parseUuid(String s) {
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    private static String strOf(Object o) {
        return o == null ? null : o.toString();
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
