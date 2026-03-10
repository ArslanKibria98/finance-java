package com.ksa.financing.onboarding.workflow;

import com.ksa.financing.onboarding.domain.model.*;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Signal-driven customer onboarding workflow for the KSA Islamic Financing Platform.
 *
 * <p>This workflow orchestrates the complete customer onboarding journey as a multi-step
 * state machine driven by external signals from the mobile app and backend callbacks:</p>
 *
 * <ol>
 *   <li><b>Tahakuk + OTP Send</b> -- Mobile verification and OTP dispatch</li>
 *   <li><b>OTP Verify (signal wait)</b> -- Wait for customer to enter OTP code</li>
 *   <li><b>Terms Acceptance (signal wait)</b> -- Wait for customer to accept T&amp;C</li>
 *   <li><b>Nafath (signal wait + callback)</b> -- Initiate Nafath, wait for callback</li>
 *   <li><b>Auto: Yakeen + Sanctions + Profile</b> -- Identity verification and profile creation</li>
 *   <li><b>Additional Info (signal wait)</b> -- Wait for employment/banking details</li>
 *   <li><b>Auto: Update + Salary + Wallet + Notification</b> -- Final enrichment</li>
 * </ol>
 *
 * <p>Each signal wait enforces device trust validation to prevent session hijacking.
 * The workflow supports query methods for real-time status monitoring by the mobile app.</p>
 */
@WorkflowInterface
public interface CustomerOnboardingWorkflow {

    @WorkflowMethod
    OnboardingResult execute(OnboardingRequest request);

    @SignalMethod
    void otpVerified(OtpVerifiedSignal signal);

    @SignalMethod
    void termsAccepted(TermsAcceptedSignal signal);

    @SignalMethod
    void nafathInitiate(NafathInitiateSignal signal);

    @SignalMethod
    void nafathCallback(NafathCallbackSignal signal);

    @SignalMethod
    void additionalInfoSubmitted(AdditionalInfoSignal signal);

    @SignalMethod
    void eddFormSubmitted(EddFormSignal signal);

    @SignalMethod
    void setPinSubmitted(SetPinSignal signal);

    @QueryMethod
    OnboardingState getState();

    @QueryMethod
    OnboardingStatus getStatus();
}
