package com.ksa.financing.onboarding.guest.application.usecase;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingState;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingStep;
import com.ksa.financing.onboarding.guest.domain.model.GuestOtpVerifiedSignal;
import com.ksa.financing.onboarding.guest.domain.port.in.VerifyGuestDualOtpUseCase;
import org.springframework.stereotype.Service;

@Service
public class VerifyGuestDualOtpService implements VerifyGuestDualOtpUseCase {

    private final GuestWorkflowClient client;

    public VerifyGuestDualOtpService(GuestWorkflowClient client) {
        this.client = client;
    }

    @Override
    public VerifyGuestOtpResult verify(String workflowId, String mobileOtp, String emailOtp,
                                       DeviceInfo deviceInfo) {
        client.stub(workflowId).dualOtpVerified(new GuestOtpVerifiedSignal(mobileOtp, emailOtp, deviceInfo));
        GuestOnboardingState state = client.awaitStep(workflowId, GuestOnboardingStep.OTP_VERIFIED, 40, 300);
        if (state.getCurrentStep() == GuestOnboardingStep.FAILED) {
            return new VerifyGuestOtpResult(workflowId, GuestOnboardingStep.FAILED,
                    null, null, null, 0L, null, false, null, state.getFailureReason());
        }
        return new VerifyGuestOtpResult(
                workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : GuestOnboardingStep.OTP_VERIFIED,
                state.getKeycloakUserId(),
                state.getAccessToken(),
                state.getRefreshToken(),
                state.getTokenExpiresIn(),
                state.getTokenType(),
                state.isOnboardingComplete(),
                "OTP verified — guest user created",
                null);
    }
}
