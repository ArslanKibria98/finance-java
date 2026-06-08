package com.ksa.financing.onboarding.canada.workflow;

import com.ksa.financing.onboarding.shared.activity.DualOtpActivity;
import com.ksa.financing.onboarding.shared.activity.SullisActivity;
import com.ksa.financing.onboarding.shared.activity.OnboardingProfileActivity;
import com.ksa.financing.onboarding.shared.sullis.SullisContext;
import com.ksa.financing.onboarding.shared.sullis.SullisDocumentResult;
import com.ksa.financing.onboarding.shared.sullis.SullisVerificationResult;
import com.ksa.financing.onboarding.canada.domain.model.BiometricsSignal;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingRequest;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingResult;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.canada.domain.model.CanadaSetPinSignal;
import com.ksa.financing.onboarding.canada.domain.model.DocConfirmedSignal;
import com.ksa.financing.onboarding.canada.domain.model.DocSelectedSignal;
import com.ksa.financing.onboarding.canada.domain.model.DocSubmittedSignal;
import com.ksa.financing.onboarding.canada.domain.model.DualOtpVerifiedSignal;
import com.ksa.financing.onboarding.canada.domain.model.SelfieSubmittedSignal;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Signal-driven implementation of the Canada onboarding workflow.
 *
 * <p><b>NOT a Spring bean.</b> Instantiated by the Temporal Worker. Activity stubs are
 * created via {@link Workflow#newActivityStub} (not via DI). Logger uses
 * {@link Workflow#getLogger} which is replay-safe.</p>
 */
public class CanadaOnboardingWorkflowImpl implements CanadaOnboardingWorkflow {

    private static final Logger log = Workflow.getLogger(CanadaOnboardingWorkflowImpl.class);

    /** Minimum Facia similarity score required to consider the selfie a match. */
    private static final double FACE_MATCH_THRESHOLD = 0.70d;

    // -------------------------------------------------------------------------
    // Queryable state
    // -------------------------------------------------------------------------
    private final CanadaOnboardingState state = new CanadaOnboardingState();

    // -------------------------------------------------------------------------
    // Signal holders
    // -------------------------------------------------------------------------
    private DualOtpVerifiedSignal otpSignal;
    private boolean otpReceived;

    private DocSelectedSignal docSelectedSignal;
    private boolean docSelectedReceived;

    private DocSubmittedSignal docSubmittedSignal;
    private boolean docSubmittedReceived;

    private DocConfirmedSignal docConfirmedSignal;
    private boolean docConfirmedReceived;

    private SelfieSubmittedSignal selfieSignal;
    private boolean selfieReceived;

    private CanadaSetPinSignal pinSignal;
    private boolean pinReceived;

    private BiometricsSignal biometricsSignal;
    private boolean biometricsReceived;

    // -------------------------------------------------------------------------
    // Activity options
    // -------------------------------------------------------------------------
    private final ActivityOptions defaultOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setMaximumInterval(Duration.ofSeconds(30))
                    .setMaximumAttempts(3)
                    .setBackoffCoefficient(2.0)
                    .build())
            .build();

    private final ActivityOptions faciaOptions = ActivityOptions.newBuilder()
            // Must exceed RestTemplate response timeout (5 min) so the HTTP call
            // gets a chance to complete before Temporal cancels the activity.
            .setStartToCloseTimeout(Duration.ofMinutes(6))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(5))
                    .setMaximumInterval(Duration.ofMinutes(1))
                    .setMaximumAttempts(3)
                    .setBackoffCoefficient(2.0)
                    .build())
            .build();

    private final DualOtpActivity otpActivity = Workflow.newActivityStub(DualOtpActivity.class, defaultOptions);
    private final SullisActivity sullisActivity = Workflow.newActivityStub(SullisActivity.class, faciaOptions);
    private final OnboardingProfileActivity profileActivity = Workflow.newActivityStub(OnboardingProfileActivity.class, defaultOptions);

    // -------------------------------------------------------------------------
    // Workflow execution
    // -------------------------------------------------------------------------
    @Override
    public CanadaOnboardingResult execute(CanadaOnboardingRequest request) {
        state.setWorkflowId(Workflow.getInfo().getWorkflowId());
        state.setEmail(request.email());
        state.setMobileNumber(request.mobileNumber());
        state.setTenantId(request.tenantId());
        state.setStartedAt(Instant.ofEpochMilli(Workflow.currentTimeMillis()));
        state.setDeviceTrusted(true);
        if (request.deviceInfo() != null) {
            state.setInitialDeviceId(request.deviceInfo().deviceId());
        }
        setStep(CanadaOnboardingStep.INITIATED);

        // ----- Step 1: send dual OTP -----
        var mobileOtp = otpActivity.sendMobileOtp(request.mobileNumber());
        var emailOtp = otpActivity.sendEmailOtp(request.email());
        if (!mobileOtp.sent() || !emailOtp.sent()) {
            return fail("Failed to send OTP: mobile=" + mobileOtp.failureReason()
                    + " email=" + emailOtp.failureReason());
        }
        state.setMobileOtpRequestId(mobileOtp.otpRequestId());
        state.setEmailOtpRequestId(emailOtp.otpRequestId());
        setStep(CanadaOnboardingStep.OTP_SENT);

        // ----- Step 2: wait for dual OTP verified signal -----
        // Loop on mismatch so user can re-enter OTPs without restarting the workflow.
        while (true) {
            Workflow.await(() -> otpReceived);
            otpReceived = false;
            var mobileVerify = otpActivity.verifyMobileOtp(state.getMobileOtpRequestId(),
                    request.mobileNumber(), otpSignal.mobileOtpCode());
            var emailVerify = otpActivity.verifyEmailOtp(state.getEmailOtpRequestId(),
                    request.email(), otpSignal.emailOtpCode());
            state.setOtpAttempts(state.getOtpAttempts() + 1);
            if (mobileVerify.verified() && emailVerify.verified()) {
                state.setFailureReason(null);
                break;
            }
            state.setFailureReason("OTP mismatch: mobile=" + mobileVerify.failureReason()
                    + " email=" + emailVerify.failureReason());
        }
        var kcResult = profileActivity.createKeycloakUser(request.email(),
                request.mobileNumber(), null, null);
        if (!kcResult.created()) return fail("Keycloak user creation failed: " + kcResult.failureReason());
        state.setKeycloakUserId(kcResult.keycloakUserId());
        var tokenResult = profileActivity.issueTokenForUser(kcResult.keycloakUserId(), request.email(),
                kcResult.tempPassword());
        if (tokenResult.issued()) {
            state.setAccessToken(tokenResult.accessToken());
            state.setRefreshToken(tokenResult.refreshToken());
            state.setTokenExpiresIn(tokenResult.expiresIn());
            state.setTokenType(tokenResult.tokenType());
        }
        setStep(CanadaOnboardingStep.OTP_VERIFIED);

        // ----- Step 3: wait for document type selection -----
        Workflow.await(() -> docSelectedReceived);
        state.setDocumentType(docSelectedSignal.documentType());
        setStep(CanadaOnboardingStep.DOC_SELECTED);

        // ----- Step 4: wait for document upload + Facia verification -----
        // Loop on FACIA decline so user can retry with a clearer / unexpired document.
        // Sullis: one session (3 attempts) for the document phase. First try creates the
        // session; each OCR-failure retry reuses it and starts a NEW attempt (type=ID→
        // NATIONAL_ID / PASSPORT). Session/attempt ids persisted so the selfie phase reuses them.
        SullisDocumentResult docResult;
        while (true) {
            Workflow.await(() -> docSubmittedReceived);
            docSubmittedReceived = false;
            docResult = sullisActivity.verifyDocument(
                    docSubmittedSignal.documentImageBase64(),
                    state.getWorkflowId(),
                    sullisDocType(state.getDocumentType()),
                    state.getSullisSessionId(),
                    new SullisContext(
                            request.mobileNumber(), null, null,
                            state.getWorkflowId(), "ONBOARDING_CANADA",
                            state.getWorkflowId() + ":doc"));
            state.setDocumentAttempts(state.getDocumentAttempts() + 1);
            // Remember the session even on decline so the next retry reuses it (new attempt).
            if (docResult.sessionId() != null) state.setSullisSessionId(docResult.sessionId());
            if (docResult.accepted()) {
                state.setSullisAttemptId(docResult.attemptId());
                state.setFailureReason(null);
                break;
            }
            state.setFailureReason("Document verification declined: " + docResult.declineReason());
        }
        state.setDocumentImageBase64(docSubmittedSignal.documentImageBase64());
        state.setFaciaDocumentReferenceId(docResult.uploadId());
        state.setExtractedData(new LinkedHashMap<>(docResult.extractedData()));
        setStep(CanadaOnboardingStep.DOC_VERIFIED);

        // ----- Step 5: wait for user-confirmed extracted fields -----
        Workflow.await(() -> docConfirmedReceived);
        Map<String, Object> confirmed = new LinkedHashMap<>();
        confirmed.put("surname", docConfirmedSignal.surname());
        confirmed.put("givenName", docConfirmedSignal.givenName());
        confirmed.put("nationality", docConfirmedSignal.nationality());
        confirmed.put("dateOfBirth", docConfirmedSignal.dateOfBirth());
        confirmed.put("documentNumber", docConfirmedSignal.documentNumber());
        confirmed.put("homeAddress", docConfirmedSignal.homeAddress());
        state.setConfirmedData(confirmed);
        setStep(CanadaOnboardingStep.DOC_CONFIRMED);

        // ----- Step 6: wait for selfie + face match + downstream profile + wallet -----
        // Loop on face mismatch so the user can retake the selfie.
        // Sullis: upload selfie + run the verification pipeline using the stored
        // session/attempt. The submit outcome (VERIFIED) + faceMatch score gate the loop.
        SullisVerificationResult faceResult;
        while (true) {
            Workflow.await(() -> selfieReceived);
            selfieReceived = false;
            faceResult = sullisActivity.submitVerification(
                    selfieSignal.selfieImageBase64(),
                    state.getSullisSessionId(), state.getSullisAttemptId(),
                    new SullisContext(
                            request.mobileNumber(), null, null,
                            state.getWorkflowId(), "ONBOARDING_CANADA",
                            state.getWorkflowId() + ":selfie"));
            state.setSelfieAttempts(state.getSelfieAttempts() + 1);
            state.setFaciaFaceMatchReferenceId(faceResult.attemptId());
            state.setFaceMatchScore(faceResult.faceMatchScore());
            // Trust Sullis's own verification outcome (VERIFIED). Sullis already factors in
            // faceMatch + liveness + same-person when deciding the outcome — imposing a
            // separate local faceMatch threshold would override the vendor's decision
            // (e.g. Sullis VERIFIED at faceMatch 0.61 must NOT be declined locally).
            if (faceResult.approved()) {
                state.setFailureReason(null);
                break;
            }
            if (faceResult.maxAttemptsReached()) {
                // Shared 3-attempt session budget (document + selfie) is used up. Mirror the
                // document MAX path: surface the restart message but keep the workflow alive.
                state.setFailureReason("Maximum verification attempts reached. Please restart onboarding.");
            } else {
                // Sullis started a fresh attempt for this failed selfie — upload the next retry to it.
                if (faceResult.attemptId() != null) state.setSullisAttemptId(faceResult.attemptId());
                state.setFailureReason("Verification declined: outcome=" + faceResult.outcome()
                        + " score=" + faceResult.faceMatchScore() + " reason=" + faceResult.reason());
            }
        }
        var profile = profileActivity.createCustomerProfile(request.email(),
                request.mobileNumber(), state.getKeycloakUserId(),
                state.getConfirmedData(), request.tenantId());
        if (!profile.created()) return fail("Customer profile creation failed: " + profile.failureReason());
        state.setCustomerId(profile.customerId());
        state.setGlobalUid(profile.globalUid());
        var wallet = profileActivity.createWalletWithName(profile.customerId(), request.tenantId(),
                composeDisplayName(state.getConfirmedData()));
        if (!wallet.created()) return fail("Wallet creation failed: " + wallet.failureReason());
        state.setWalletId(wallet.walletId());
        setStep(CanadaOnboardingStep.SELFIE_VERIFIED);

        // ----- Step 7: wait for PIN setup -----
        Workflow.await(() -> pinReceived);
        profileActivity.persistPin(state.getKeycloakUserId(), pinSignal.pin());
        state.setPinSet(true);
        setStep(CanadaOnboardingStep.PIN_SETUP);

        // ----- Step 8: wait for biometrics enable/skip -----
        Workflow.await(() -> biometricsReceived);
        state.setBiometricsEnabled(biometricsSignal.enabled() && !biometricsSignal.skipped());
        state.setBiometricsSkipped(biometricsSignal.skipped());
        setStep(CanadaOnboardingStep.BIOMETRICS_SETUP);

        // Persist completion flags onto Keycloak + write final audit row.
        profileActivity.completeOnboarding(state.getKeycloakUserId(),
                "CANADA", state.getMobileNumber(), true);
        setStep(CanadaOnboardingStep.COMPLETED);
        return new CanadaOnboardingResult(state.getWorkflowId(), state.getCustomerId(),
                state.getWalletId(), state.getKeycloakUserId(),
                CanadaOnboardingStep.COMPLETED, null);
    }

    // -------------------------------------------------------------------------
    // Signal handlers
    // -------------------------------------------------------------------------
    @Override
    public void dualOtpVerified(DualOtpVerifiedSignal signal) {
        this.otpSignal = signal;
        this.otpReceived = true;
    }

    @Override
    public void documentSelected(DocSelectedSignal signal) {
        this.docSelectedSignal = signal;
        this.docSelectedReceived = true;
    }

    @Override
    public void documentSubmitted(DocSubmittedSignal signal) {
        this.docSubmittedSignal = signal;
        this.docSubmittedReceived = true;
    }

    @Override
    public void documentConfirmed(DocConfirmedSignal signal) {
        this.docConfirmedSignal = signal;
        this.docConfirmedReceived = true;
    }

    @Override
    public void selfieSubmitted(SelfieSubmittedSignal signal) {
        this.selfieSignal = signal;
        this.selfieReceived = true;
    }

    @Override
    public void pinSubmitted(CanadaSetPinSignal signal) {
        this.pinSignal = signal;
        this.pinReceived = true;
    }

    @Override
    public void biometricsSubmitted(BiometricsSignal signal) {
        this.biometricsSignal = signal;
        this.biometricsReceived = true;
    }

    @Override
    public CanadaOnboardingState getState() {
        return state;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private void setStep(CanadaOnboardingStep step) {
        state.setCurrentStep(step);
        state.setLastUpdatedAt(Instant.ofEpochMilli(Workflow.currentTimeMillis()));
        log.info("Canada onboarding workflow {} -> {}", state.getWorkflowId(), step);
    }

    private CanadaOnboardingResult fail(String reason) {
        state.setFailureReason(reason);
        setStep(CanadaOnboardingStep.FAILED);
        log.warn("Canada onboarding workflow {} FAILED: {}", state.getWorkflowId(), reason);
        throw ApplicationFailure.newFailure(reason, "CanadaOnboardingFailed");
    }

    private static String composeDisplayName(java.util.Map<String, Object> confirmed) {
        if (confirmed == null) return null;
        String first = strOf(confirmed.get("givenName"));
        if (first == null) first = strOf(confirmed.get("first_name"));
        if (first == null) first = strOf(confirmed.get("firstName"));
        String last = strOf(confirmed.get("surname"));
        if (last == null) last = strOf(confirmed.get("last_name"));
        if (last == null) last = strOf(confirmed.get("lastName"));
        String full = strOf(confirmed.get("full_name"));
        if (full == null) full = strOf(confirmed.get("fullName"));
        if (full != null && !full.isBlank()) return full.trim();
        String joined = ((first != null ? first.trim() : "") + " "
                + (last != null ? last.trim() : "")).trim();
        return joined.isEmpty() ? null : joined;
    }

    private static String strOf(Object v) {
        return v == null ? null : v.toString();
    }

    /**
     * Map the document type the user selected at {@code /select-document} to the Sullis
     * {@code type} query param: PASSPORT → PASSPORT, everything else (ID / NATIONAL_ID) →
     * NATIONAL_ID. Foreign flow is passport-only and hardcodes PASSPORT separately.
     */
    private static String sullisDocType(com.ksa.financing.onboarding.canada.domain.model.DocumentType selected) {
        return selected == com.ksa.financing.onboarding.canada.domain.model.DocumentType.PASSPORT
                ? "PASSPORT" : "NATIONAL_ID";
    }
}
