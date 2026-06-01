package com.ksa.financing.onboarding.guest.workflow;

import com.ksa.financing.onboarding.guest.domain.model.GuestBiometricsSignal;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingRequest;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingState;
import com.ksa.financing.onboarding.guest.domain.model.GuestOtpVerifiedSignal;
import com.ksa.financing.onboarding.guest.domain.model.GuestPinSignal;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Country-agnostic guest onboarding workflow.
 *
 * <p>Four steps: dual OTP → biometrics → PIN → COMPLETED. The state carries an
 * {@code onboardingComplete} flag which always remains {@code false} for a guest user —
 * downstream consumers use it to gate any feature that requires full KYC onboarding.</p>
 */
@WorkflowInterface
public interface GuestOnboardingWorkflow {

    @WorkflowMethod
    void execute(GuestOnboardingRequest request);

    @SignalMethod
    void dualOtpVerified(GuestOtpVerifiedSignal signal);

    @SignalMethod
    void biometricsSubmitted(GuestBiometricsSignal signal);

    @SignalMethod
    void pinSubmitted(GuestPinSignal signal);

    @QueryMethod
    GuestOnboardingState getState();
}
