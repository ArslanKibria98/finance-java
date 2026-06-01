package com.ksa.financing.onboarding.foreign.workflow;

import com.ksa.financing.onboarding.shared.activity.DualOtpActivity;
import com.ksa.financing.onboarding.shared.activity.FaciaActivity;
import com.ksa.financing.onboarding.shared.activity.OnboardingProfileActivity;
import com.ksa.financing.onboarding.shared.facia.FaciaDocumentResult;
import com.ksa.financing.onboarding.shared.facia.FaciaFaceMatchResult;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignBiometricsSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignDataConfirmedSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingRequest;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingResult;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingState;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOtpVerifiedSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignPassportSubmittedSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignPinSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignSelfieSubmittedSignal;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class ForeignOnboardingWorkflowImpl implements ForeignOnboardingWorkflow {

    private static final Logger log = Workflow.getLogger(ForeignOnboardingWorkflowImpl.class);

    private static final double FACE_MATCH_THRESHOLD = 0.70d;

    private final ForeignOnboardingState state = new ForeignOnboardingState();

    private ForeignOtpVerifiedSignal otpSignal;
    private boolean otpReceived;

    private ForeignPassportSubmittedSignal passportSignal;
    private boolean passportReceived;

    private ForeignDataConfirmedSignal dataConfirmedSignal;
    private boolean dataConfirmedReceived;

    private ForeignSelfieSubmittedSignal selfieSignal;
    private boolean selfieReceived;

    private ForeignPinSignal pinSignal;
    private boolean pinReceived;

    private ForeignBiometricsSignal biometricsSignal;
    private boolean biometricsReceived;

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
    private final FaciaActivity faciaActivity = Workflow.newActivityStub(FaciaActivity.class, faciaOptions);
    private final OnboardingProfileActivity profileActivity = Workflow.newActivityStub(OnboardingProfileActivity.class, defaultOptions);

    @Override
    public ForeignOnboardingResult execute(ForeignOnboardingRequest request) {
        state.setWorkflowId(Workflow.getInfo().getWorkflowId());
        state.setEmail(request.email());
        state.setMobileNumber(request.mobileNumber());
        state.setCountryOfOrigin(request.countryOfOrigin());
        state.setResidentialCountry(request.residentialCountry());
        state.setTenantId(request.tenantId());
        state.setStartedAt(Instant.ofEpochMilli(Workflow.currentTimeMillis()));
        state.setOnboardingComplete(false);
        if (request.deviceInfo() != null) {
            state.setInitialDeviceId(request.deviceInfo().deviceId());
        }
        setStep(ForeignOnboardingStep.INITIATED);

        // ----- Step 1: send dual OTP -----
        var mobileOtp = otpActivity.sendMobileOtp(request.mobileNumber());
        var emailOtp = otpActivity.sendEmailOtp(request.email());
        if (!mobileOtp.sent() || !emailOtp.sent()) {
            return fail("Failed to send OTP: mobile=" + mobileOtp.failureReason()
                    + " email=" + emailOtp.failureReason());
        }
        state.setMobileOtpRequestId(mobileOtp.otpRequestId());
        state.setEmailOtpRequestId(emailOtp.otpRequestId());
        setStep(ForeignOnboardingStep.OTP_SENT);

        // ----- Step 2: wait for OTP, create Keycloak user + token -----
        // Loop until both OTPs verify. Each wrong attempt bumps otpAttempts so the
        // service can surface the mismatch reason without killing the workflow —
        // user can keep retrying.
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
        var tokenResult = profileActivity.issueTokenForUser(kcResult.keycloakUserId(),
                request.email(), kcResult.tempPassword());
        if (tokenResult.issued()) {
            state.setAccessToken(tokenResult.accessToken());
            state.setRefreshToken(tokenResult.refreshToken());
            state.setTokenExpiresIn(tokenResult.expiresIn());
            state.setTokenType(tokenResult.tokenType());
        }
        setStep(ForeignOnboardingStep.OTP_VERIFIED);

        // ----- Step 3: wait for passport image, run Facia document-verification -----
        // Loop until FACIA accepts. Each declined attempt bumps passportAttempts +
        // sets failureReason so the service can return the decline reason and the
        // user can retry without restarting the whole workflow.
        FaciaDocumentResult docResult;
        while (true) {
            Workflow.await(() -> passportReceived);
            passportReceived = false;
            docResult = faciaActivity.verifyDocumentWithContext(
                    passportSignal.passportImageBase64(),
                    new com.ksa.financing.onboarding.shared.facia.FaciaContext(
                            request.mobileNumber(), null, null,
                            state.getWorkflowId(), "ONBOARDING_FOREIGN",
                            state.getWorkflowId() + ":passport"));
            state.setPassportAttempts(state.getPassportAttempts() + 1);
            if (docResult.accepted()) {
                state.setFailureReason(null);
                break;
            }
            state.setFailureReason("Passport verification declined: " + docResult.declineReason());
        }
        state.setPassportImageBase64(passportSignal.passportImageBase64());
        state.setFaciaPassportReferenceId(docResult.referenceId());
        state.setExtractedData(new LinkedHashMap<>(docResult.extractedData()));
        // Capture passport dates from Facia OCR if present
        Object issueDate = docResult.extractedData().get("issue_date");
        Object expiryDate = docResult.extractedData().get("expiry_date");
        if (issueDate != null) state.setPassportIssueDate(issueDate.toString());
        if (expiryDate != null) state.setPassportExpiryDate(expiryDate.toString());
        setStep(ForeignOnboardingStep.PASSPORT_UPLOADED);

        // ----- Step 4: wait for user-confirmed extracted fields -----
        Workflow.await(() -> dataConfirmedReceived);
        Map<String, Object> confirmed = new LinkedHashMap<>();
        confirmed.put("surname", dataConfirmedSignal.surname());
        confirmed.put("givenName", dataConfirmedSignal.givenName());
        confirmed.put("nationality", dataConfirmedSignal.nationality());
        confirmed.put("dateOfBirth", dataConfirmedSignal.dateOfBirth());
        confirmed.put("passportNumber", dataConfirmedSignal.passportNumber());
        confirmed.put("issueDate", dataConfirmedSignal.issueDate());
        confirmed.put("expiryDate", dataConfirmedSignal.expiryDate());
        confirmed.put("homeAddress", dataConfirmedSignal.homeAddress());
        confirmed.put("countryOfOrigin", dataConfirmedSignal.countryOfOrigin());
        confirmed.put("residentialCountry", dataConfirmedSignal.residentialCountry());
        state.setConfirmedData(confirmed);
        // Allow the user to correct any field — including the two captured at initiate
        if (dataConfirmedSignal.countryOfOrigin() != null && !dataConfirmedSignal.countryOfOrigin().isBlank()) {
            state.setCountryOfOrigin(dataConfirmedSignal.countryOfOrigin());
        }
        if (dataConfirmedSignal.residentialCountry() != null && !dataConfirmedSignal.residentialCountry().isBlank()) {
            state.setResidentialCountry(dataConfirmedSignal.residentialCountry());
        }
        if (dataConfirmedSignal.issueDate() != null && !dataConfirmedSignal.issueDate().isBlank()) {
            state.setPassportIssueDate(dataConfirmedSignal.issueDate());
        }
        if (dataConfirmedSignal.expiryDate() != null && !dataConfirmedSignal.expiryDate().isBlank()) {
            state.setPassportExpiryDate(dataConfirmedSignal.expiryDate());
        }
        setStep(ForeignOnboardingStep.DATA_CONFIRMED);

        // ----- Step 5: wait for selfie, face-match, create customer + wallet -----
        // Loop until face matches. User can re-take selfie if FACIA rejects.
        FaciaFaceMatchResult faceResult;
        while (true) {
            Workflow.await(() -> selfieReceived);
            selfieReceived = false;
            faceResult = faciaActivity.faceMatchWithContext(
                    selfieSignal.selfieImageBase64(), state.getPassportImageBase64(),
                    new com.ksa.financing.onboarding.shared.facia.FaciaContext(
                            request.mobileNumber(), null, null,
                            state.getWorkflowId(), "ONBOARDING_FOREIGN",
                            state.getWorkflowId() + ":selfie"));
            state.setSelfieAttempts(state.getSelfieAttempts() + 1);
            state.setFaciaFaceMatchReferenceId(faceResult.referenceId());
            state.setFaceMatchScore(faceResult.similarityScore());
            if (faceResult.match()
                    && faceResult.similarityScore() != null
                    && faceResult.similarityScore() >= FACE_MATCH_THRESHOLD) {
                state.setFailureReason(null);
                break;
            }
            state.setFailureReason("Face match failed: score="
                    + faceResult.similarityScore() + " reason=" + faceResult.failureReason());
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
        setStep(ForeignOnboardingStep.SELFIE_VERIFIED);

        // ----- Step 6: wait for PIN -----
        Workflow.await(() -> pinReceived);
        profileActivity.persistPin(state.getKeycloakUserId(), pinSignal.pin());
        state.setPinSet(true);
        setStep(ForeignOnboardingStep.PIN_SETUP);

        // ----- Step 7: wait for biometrics enable/skip → COMPLETED -----
        Workflow.await(() -> biometricsReceived);
        state.setBiometricsEnabled(biometricsSignal.enabled() && !biometricsSignal.skipped());
        state.setBiometricsSkipped(biometricsSignal.skipped());
        setStep(ForeignOnboardingStep.BIOMETRICS_SETUP);

        // Foreign national finished full KYC — flip the flag + persist to Keycloak + audit.
        state.setOnboardingComplete(true);
        profileActivity.completeOnboarding(state.getKeycloakUserId(),
                "FOREIGN", state.getMobileNumber(), true);
        setStep(ForeignOnboardingStep.COMPLETED);

        return new ForeignOnboardingResult(state.getWorkflowId(), state.getCustomerId(),
                state.getWalletId(), state.getKeycloakUserId(),
                ForeignOnboardingStep.COMPLETED, null);
    }

    @Override
    public void dualOtpVerified(ForeignOtpVerifiedSignal signal) {
        this.otpSignal = signal;
        this.otpReceived = true;
    }

    @Override
    public void passportSubmitted(ForeignPassportSubmittedSignal signal) {
        this.passportSignal = signal;
        this.passportReceived = true;
    }

    @Override
    public void dataConfirmed(ForeignDataConfirmedSignal signal) {
        this.dataConfirmedSignal = signal;
        this.dataConfirmedReceived = true;
    }

    @Override
    public void selfieSubmitted(ForeignSelfieSubmittedSignal signal) {
        this.selfieSignal = signal;
        this.selfieReceived = true;
    }

    @Override
    public void pinSubmitted(ForeignPinSignal signal) {
        this.pinSignal = signal;
        this.pinReceived = true;
    }

    @Override
    public void biometricsSubmitted(ForeignBiometricsSignal signal) {
        this.biometricsSignal = signal;
        this.biometricsReceived = true;
    }

    @Override
    public ForeignOnboardingState getState() {
        return state;
    }

    private void setStep(ForeignOnboardingStep step) {
        state.setCurrentStep(step);
        state.setLastUpdatedAt(Instant.ofEpochMilli(Workflow.currentTimeMillis()));
        log.info("Foreign onboarding workflow {} -> {}", state.getWorkflowId(), step);
    }

    private ForeignOnboardingResult fail(String reason) {
        state.setFailureReason(reason);
        setStep(ForeignOnboardingStep.FAILED);
        log.warn("Foreign onboarding workflow {} FAILED: {}", state.getWorkflowId(), reason);
        throw ApplicationFailure.newFailure(reason, "ForeignOnboardingFailed");
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
}
