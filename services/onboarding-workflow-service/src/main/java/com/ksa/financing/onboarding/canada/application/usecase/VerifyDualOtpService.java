package com.ksa.financing.onboarding.canada.application.usecase;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.canada.domain.model.DualOtpVerifiedSignal;
import com.ksa.financing.onboarding.canada.domain.port.in.VerifyDualOtpUseCase;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import org.springframework.stereotype.Service;

@Service
public class VerifyDualOtpService implements VerifyDualOtpUseCase {

    private final CanadaWorkflowClient client;

    public VerifyDualOtpService(CanadaWorkflowClient client) {
        this.client = client;
    }

    @Override
    public VerifyDualOtpResult verify(String workflowId, String mobileOtp, String emailOtp,
                                      DeviceInfo deviceInfo) {
        CanadaOnboardingState before = client.getState(workflowId);
        int beforeAttempts = before.getOtpAttempts();

        client.stub(workflowId).dualOtpVerified(new DualOtpVerifiedSignal(mobileOtp, emailOtp, deviceInfo));
        CanadaOnboardingState state = client.awaitOtpAttempt(workflowId, beforeAttempts, 40, 300);
        if (state.getCurrentStep() == CanadaOnboardingStep.FAILED) {
            return new VerifyDualOtpResult(workflowId, CanadaOnboardingStep.FAILED,
                    null, null, null, 0L, null, null, state.getFailureReason());
        }
        if (state.getFailureReason() != null
                && state.getCurrentStep() != CanadaOnboardingStep.OTP_VERIFIED) {
            return new VerifyDualOtpResult(workflowId, CanadaOnboardingStep.OTP_SENT,
                    null, null, null, 0L, null,
                    "OTP verification failed", state.getFailureReason());
        }
        // OTP verified — the workflow mints the Keycloak token AFTER incrementing
        // otpAttempts and only then sets step=OTP_VERIFIED. awaitOtpAttempt returns on the
        // attempt increment, so the token is not yet on the state. Wait for OTP_VERIFIED to
        // avoid returning a null accessToken.
        state = client.awaitStep(workflowId, CanadaOnboardingStep.OTP_VERIFIED, 40, 300);
        return new VerifyDualOtpResult(
                workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : CanadaOnboardingStep.OTP_VERIFIED,
                state.getKeycloakUserId(),
                state.getAccessToken(),
                state.getRefreshToken(),
                state.getTokenExpiresIn(),
                state.getTokenType(),
                "OTP verified",
                null);
    }
}
