package com.ksa.financing.onboarding.guest.workflow;

import com.ksa.financing.onboarding.shared.activity.DualOtpActivity;
import com.ksa.financing.onboarding.shared.activity.OnboardingProfileActivity;
import com.ksa.financing.onboarding.guest.domain.model.GuestBiometricsSignal;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingRequest;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingState;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingStep;
import com.ksa.financing.onboarding.guest.domain.model.GuestOtpVerifiedSignal;
import com.ksa.financing.onboarding.guest.domain.model.GuestPinSignal;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;

public class GuestOnboardingWorkflowImpl implements GuestOnboardingWorkflow {

    private static final Logger log = Workflow.getLogger(GuestOnboardingWorkflowImpl.class);

    private final GuestOnboardingState state = new GuestOnboardingState();

    private GuestOtpVerifiedSignal otpSignal;
    private boolean otpReceived;

    private GuestBiometricsSignal biometricsSignal;
    private boolean biometricsReceived;

    private GuestPinSignal pinSignal;
    private boolean pinReceived;

    private final ActivityOptions defaultOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setMaximumInterval(Duration.ofSeconds(30))
                    .setMaximumAttempts(3)
                    .setBackoffCoefficient(2.0)
                    .build())
            .build();

    private final DualOtpActivity otpActivity = Workflow.newActivityStub(DualOtpActivity.class, defaultOptions);
    private final OnboardingProfileActivity profileActivity = Workflow.newActivityStub(OnboardingProfileActivity.class, defaultOptions);

    @Override
    public void execute(GuestOnboardingRequest request) {
        state.setWorkflowId(Workflow.getInfo().getWorkflowId());
        state.setEmail(request.email());
        state.setMobileNumber(request.mobileNumber());
        state.setTenantId(request.tenantId());
        state.setStartedAt(Instant.ofEpochMilli(Workflow.currentTimeMillis()));
        state.setOnboardingComplete(false);
        if (request.deviceInfo() != null) {
            state.setInitialDeviceId(request.deviceInfo().deviceId());
        }
        setStep(GuestOnboardingStep.INITIATED);

        // ----- Step 1: send dual OTP -----
        var mobileOtp = otpActivity.sendMobileOtp(request.mobileNumber());
        var emailOtp = otpActivity.sendEmailOtp(request.email());
        if (!mobileOtp.sent() || !emailOtp.sent()) {
            fail("Failed to send OTP: mobile=" + mobileOtp.failureReason() + " email=" + emailOtp.failureReason());
            return;
        }
        state.setMobileOtpRequestId(mobileOtp.otpRequestId());
        state.setEmailOtpRequestId(emailOtp.otpRequestId());
        setStep(GuestOnboardingStep.OTP_SENT);

        // ----- Step 2: wait for OTP, create Keycloak user + token -----
        // Loop on mismatch so user can re-enter OTPs without restarting the workflow.
        while (true) {
            Workflow.await(() -> otpReceived);
            otpReceived = false;
            var mobileVerify = otpActivity.verifyMobileOtp(state.getMobileOtpRequestId(),
                    request.mobileNumber(), otpSignal.mobileOtpCode());
            var emailVerify = otpActivity.verifyEmailOtp(state.getEmailOtpRequestId(),
                    request.email(), otpSignal.emailOtpCode());
            if (mobileVerify.verified() && emailVerify.verified()) {
                state.setFailureReason(null);
                break;
            }
            state.setFailureReason("OTP mismatch: mobile=" + mobileVerify.failureReason()
                    + " email=" + emailVerify.failureReason());
        }
        var kcResult = profileActivity.createKeycloakUser(request.email(),
                request.mobileNumber(), null, null);
        if (!kcResult.created()) { fail("Keycloak user creation failed: " + kcResult.failureReason()); return; }
        state.setKeycloakUserId(kcResult.keycloakUserId());
        var tokenResult = profileActivity.issueTokenForUser(kcResult.keycloakUserId(),
                request.email(), kcResult.tempPassword());
        if (tokenResult.issued()) {
            state.setAccessToken(tokenResult.accessToken());
            state.setRefreshToken(tokenResult.refreshToken());
            state.setTokenExpiresIn(tokenResult.expiresIn());
            state.setTokenType(tokenResult.tokenType());
        }
        // Guest gets a LEAD customer record + wallet right after OTP (no document/selfie).
        // PII vault is skipped automatically (confirmedDocumentData is empty).
        var profile = profileActivity.createCustomerProfile(request.email(),
                request.mobileNumber(), kcResult.keycloakUserId(),
                java.util.Map.of(), request.tenantId());
        if (profile.created()) {
            state.setCustomerId(profile.customerId());
            state.setGlobalUid(profile.globalUid());
            var wallet = profileActivity.createWallet(profile.customerId(), request.tenantId());
            if (wallet.created()) state.setWalletId(wallet.walletId());
        }
        setStep(GuestOnboardingStep.OTP_VERIFIED);

        // ----- Step 3: wait for biometrics enable/skip -----
        Workflow.await(() -> biometricsReceived);
        state.setBiometricsEnabled(biometricsSignal.enabled() && !biometricsSignal.skipped());
        state.setBiometricsSkipped(biometricsSignal.skipped());
        setStep(GuestOnboardingStep.BIOMETRICS_SETUP);

        // ----- Step 4: wait for PIN -----
        Workflow.await(() -> pinReceived);
        profileActivity.persistPin(state.getKeycloakUserId(), pinSignal.pin());
        state.setPinSet(true);
        setStep(GuestOnboardingStep.PIN_SETUP);

        // Guest journey done — the user can reach the dashboard but their full KYC
        // onboarding is still incomplete. The flag stays false on the session and on Keycloak.
        state.setOnboardingComplete(false);
        profileActivity.completeOnboarding(state.getKeycloakUserId(),
                "GUEST", state.getMobileNumber(), false);
        setStep(GuestOnboardingStep.COMPLETED);
    }

    @Override
    public void dualOtpVerified(GuestOtpVerifiedSignal signal) {
        this.otpSignal = signal;
        this.otpReceived = true;
    }

    @Override
    public void biometricsSubmitted(GuestBiometricsSignal signal) {
        this.biometricsSignal = signal;
        this.biometricsReceived = true;
    }

    @Override
    public void pinSubmitted(GuestPinSignal signal) {
        this.pinSignal = signal;
        this.pinReceived = true;
    }

    @Override
    public GuestOnboardingState getState() {
        return state;
    }

    private void setStep(GuestOnboardingStep step) {
        state.setCurrentStep(step);
        state.setLastUpdatedAt(Instant.ofEpochMilli(Workflow.currentTimeMillis()));
        log.info("Guest workflow {} -> {}", state.getWorkflowId(), step);
    }

    private void fail(String reason) {
        state.setFailureReason(reason);
        setStep(GuestOnboardingStep.FAILED);
        log.warn("Guest workflow {} FAILED: {}", state.getWorkflowId(), reason);
        throw ApplicationFailure.newFailure(reason, "GuestOnboardingFailed");
    }
}
