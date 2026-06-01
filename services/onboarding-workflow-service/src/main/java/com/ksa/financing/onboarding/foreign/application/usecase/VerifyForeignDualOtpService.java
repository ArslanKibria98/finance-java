package com.ksa.financing.onboarding.foreign.application.usecase;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingState;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOtpVerifiedSignal;
import com.ksa.financing.onboarding.foreign.domain.port.in.VerifyForeignDualOtpUseCase;
import org.springframework.stereotype.Service;

@Service
public class VerifyForeignDualOtpService implements VerifyForeignDualOtpUseCase {

    private final ForeignWorkflowClient client;

    public VerifyForeignDualOtpService(ForeignWorkflowClient client) {
        this.client = client;
    }

    @Override
    public VerifyForeignOtpResult verify(String workflowId, String mobileOtp, String emailOtp,
                                         DeviceInfo deviceInfo) {
        // Counter-based polling so OTP mismatch can be reported without killing the
        // workflow — user can re-enter the codes as many times as they like.
        ForeignOnboardingState before = client.getState(workflowId);
        int beforeAttempts = before.getOtpAttempts();

        client.stub(workflowId).dualOtpVerified(new ForeignOtpVerifiedSignal(mobileOtp, emailOtp, deviceInfo));
        ForeignOnboardingState state = client.awaitOtpAttempt(workflowId, beforeAttempts, 40, 300);
        if (state.getCurrentStep() == ForeignOnboardingStep.FAILED) {
            return new VerifyForeignOtpResult(workflowId, ForeignOnboardingStep.FAILED,
                    null, null, null, 0L, null, null, state.getFailureReason());
        }
        if (state.getFailureReason() != null
                && state.getCurrentStep() != ForeignOnboardingStep.OTP_VERIFIED) {
            return new VerifyForeignOtpResult(workflowId, ForeignOnboardingStep.OTP_SENT,
                    null, null, null, 0L, null,
                    "OTP verification failed", state.getFailureReason());
        }
        // OTP verified — the workflow mints the Keycloak token AFTER incrementing
        // otpAttempts and only then sets step=OTP_VERIFIED. awaitOtpAttempt returns on the
        // attempt increment, so the token is not yet on the state. Wait for OTP_VERIFIED to
        // avoid returning a null accessToken.
        state = client.awaitStep(workflowId, ForeignOnboardingStep.OTP_VERIFIED, 40, 300);
        return new VerifyForeignOtpResult(
                workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : ForeignOnboardingStep.OTP_VERIFIED,
                state.getKeycloakUserId(),
                state.getAccessToken(),
                state.getRefreshToken(),
                state.getTokenExpiresIn(),
                state.getTokenType(),
                "OTP verified",
                null);
    }
}
