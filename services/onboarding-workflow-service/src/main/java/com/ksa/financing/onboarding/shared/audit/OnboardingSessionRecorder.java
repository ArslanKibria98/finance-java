package com.ksa.financing.onboarding.shared.audit;

import com.ksa.financing.onboarding.shared.audit.entity.OnboardingExternalRefJpaEntity;
import com.ksa.financing.onboarding.shared.audit.entity.OnboardingSessionJpaEntity;
import com.ksa.financing.onboarding.shared.audit.entity.OnboardingStepAuditJpaEntity;
import com.ksa.financing.onboarding.shared.audit.repository.OnboardingExternalRefRepository;
import com.ksa.financing.onboarding.shared.audit.repository.OnboardingSessionRepository;
import com.ksa.financing.onboarding.shared.audit.repository.OnboardingStepAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Central audit recorder for onboarding workflows.
 *
 * <p>All activities + workflow clients call this service to persist:
 * <ul>
 *   <li>A {@link OnboardingSessionJpaEntity} per workflow execution</li>
 *   <li>A {@link OnboardingStepAuditJpaEntity} per step transition (full state-machine trail)</li>
 *   <li>An {@link OnboardingExternalRefJpaEntity} per external ID generated (Keycloak, Facia,
 *       customer, wallet, OTP, risk, …)</li>
 * </ul>
 *
 * <p>PII is never stored in clear text — only SHA-256 hashes + masked forms.</p>
 */
@Service
public class OnboardingSessionRecorder {

    private static final Logger log = LoggerFactory.getLogger(OnboardingSessionRecorder.class);

    public enum FlowType { KSA, CANADA, FOREIGN, GUEST }

    public enum RefType {
        KEYCLOAK, FACIA_DOC, FACIA_SELFIE, CUSTOMER, WALLET,
        OTP_MOBILE, OTP_EMAIL, RISK, PII_VAULT, GLOBAL_PROFILE
    }

    private final OnboardingSessionRepository sessions;
    private final OnboardingStepAuditRepository steps;
    private final OnboardingExternalRefRepository refs;

    public OnboardingSessionRecorder(OnboardingSessionRepository sessions,
                                     OnboardingStepAuditRepository steps,
                                     OnboardingExternalRefRepository refs) {
        this.sessions = sessions;
        this.steps = steps;
        this.refs = refs;
    }

    /**
     * Creates the session row at workflow start. Idempotent — returns the existing row if
     * a session for {@code workflowId} already exists.
     */
    @Transactional
    public OnboardingSessionJpaEntity recordSessionStart(String workflowId, FlowType flowType,
                                                         UUID tenantId, String email, String mobile,
                                                         String nationalId) {
        Optional<OnboardingSessionJpaEntity> existing = sessions.findByWorkflowId(workflowId);
        if (existing.isPresent()) {
            return existing.get();
        }
        OnboardingSessionJpaEntity session = new OnboardingSessionJpaEntity();
        session.setWorkflowId(workflowId);
        session.setFlowType(flowType.name());
        session.setTenantId(tenantId);
        session.setEmailHash(sha256(email));
        session.setMobileHash(sha256(mobile));
        session.setNationalIdHash(sha256(nationalId));
        session.setMaskedEmail(maskEmail(email));
        session.setMaskedMobile(maskMobile(mobile));
        session.setCurrentStep("INITIATED");
        session.setOnboardingComplete(false);
        OnboardingSessionJpaEntity saved = sessions.save(session);
        recordStep(workflowId, "INITIATED", null, null);
        log.info("Onboarding session created: workflowId={} flow={} maskedEmail={}",
                workflowId, flowType, session.getMaskedEmail());
        return saved;
    }

    /**
     * Records a step transition. Writes a row in {@code onboarding_step_audit} and updates
     * {@code current_step} on the session row.
     */
    @Transactional
    public void recordStep(String workflowId, String step, Map<String, Object> payload, String failureReason) {
        Optional<OnboardingSessionJpaEntity> session = sessions.findByWorkflowId(workflowId);
        if (session.isEmpty()) {
            log.warn("recordStep called but no session for workflowId={} (step={})", workflowId, step);
            return;
        }
        OnboardingSessionJpaEntity s = session.get();

        OnboardingStepAuditJpaEntity audit = new OnboardingStepAuditJpaEntity();
        audit.setSessionId(s.getId());
        audit.setWorkflowId(workflowId);
        audit.setStep(step);
        audit.setStatus(failureReason == null ? "OK" : "FAILED");
        audit.setPayload(payload);
        audit.setFailureReason(failureReason);
        steps.save(audit);

        s.setCurrentStep(step);
        if ("FAILED".equals(step)) {
            s.setFailedAt(Instant.now());
            s.setFailureReason(failureReason);
        }
        sessions.save(s);
    }

    /**
     * Records an external system reference (Keycloak user, Facia transaction, customer ID,
     * wallet ID, etc.) for the given workflow.
     */
    @Transactional
    public void recordExternalRef(String workflowId, RefType refType, String refId,
                                  Map<String, Object> metadata) {
        Optional<OnboardingSessionJpaEntity> session = sessions.findByWorkflowId(workflowId);
        if (session.isEmpty()) {
            log.warn("recordExternalRef called but no session for workflowId={} (refType={})",
                    workflowId, refType);
            return;
        }
        OnboardingSessionJpaEntity s = session.get();

        OnboardingExternalRefJpaEntity ref = new OnboardingExternalRefJpaEntity();
        ref.setSessionId(s.getId());
        ref.setWorkflowId(workflowId);
        ref.setRefType(refType.name());
        ref.setRefId(refId);
        ref.setMetadata(metadata);
        refs.save(ref);

        // Convenience: also denormalise common IDs onto the session row so /status is fast.
        boolean dirty = false;
        switch (refType) {
            case KEYCLOAK -> { s.setKeycloakUserId(refId); dirty = true; }
            case CUSTOMER -> { s.setCustomerId(refId); dirty = true; }
            case WALLET   -> { s.setWalletId(refId); dirty = true; }
            case GLOBAL_PROFILE -> { s.setGlobalUid(refId); dirty = true; }
            default -> { /* refs table is the source of truth for the rest */ }
        }
        if (dirty) sessions.save(s);
    }

    /**
     * Marks the session complete with the final {@code onboardingComplete} flag.
     * Writes a final {@code COMPLETED} step row.
     */
    @Transactional
    public void recordSessionComplete(String workflowId, boolean onboardingComplete) {
        Optional<OnboardingSessionJpaEntity> session = sessions.findByWorkflowId(workflowId);
        if (session.isEmpty()) {
            log.warn("recordSessionComplete called but no session for workflowId={}", workflowId);
            return;
        }
        OnboardingSessionJpaEntity s = session.get();
        s.setCurrentStep("COMPLETED");
        s.setOnboardingComplete(onboardingComplete);
        s.setCompletedAt(Instant.now());
        sessions.save(s);
        recordStep(workflowId, "COMPLETED", null, null);
    }

    /** Reads the session for a workflow (used by /status fast-path). */
    @Transactional(readOnly = true)
    public Optional<OnboardingSessionJpaEntity> findByWorkflowId(String workflowId) {
        return sessions.findByWorkflowId(workflowId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String sha256(String input) {
        if (input == null || input.isBlank()) return null;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return null;
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return "**" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    private static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return null;
        return "****" + mobile.substring(mobile.length() - 4);
    }
}
