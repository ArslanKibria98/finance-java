package com.ksa.financing.onboarding.foreign.workflow;

import com.ksa.financing.onboarding.foreign.domain.model.ForeignBiometricsSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignDataConfirmedSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingRequest;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingResult;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingState;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOtpVerifiedSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignPassportSubmittedSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignPinSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignSelfieSubmittedSignal;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Foreign national onboarding workflow. Country-agnostic — passport-only KYC, captures
 * country of origin + country of residence in addition to the standard fields.
 *
 * <ol>
 *   <li>Send dual OTP to mobile + email</li>
 *   <li>Wait for OTP verified signal → create Keycloak user + issue token</li>
 *   <li>Wait for passport image → Facia document-verification</li>
 *   <li>Wait for user confirmation of extracted fields</li>
 *   <li>Wait for selfie → Facia face-match → customer + wallet</li>
 *   <li>Wait for PIN</li>
 *   <li>Wait for biometrics → COMPLETED (onboardingComplete=true)</li>
 * </ol>
 */
@WorkflowInterface
public interface ForeignOnboardingWorkflow {

    @WorkflowMethod
    ForeignOnboardingResult execute(ForeignOnboardingRequest request);

    @SignalMethod
    void dualOtpVerified(ForeignOtpVerifiedSignal signal);

    @SignalMethod
    void passportSubmitted(ForeignPassportSubmittedSignal signal);

    @SignalMethod
    void dataConfirmed(ForeignDataConfirmedSignal signal);

    @SignalMethod
    void selfieSubmitted(ForeignSelfieSubmittedSignal signal);

    @SignalMethod
    void pinSubmitted(ForeignPinSignal signal);

    @SignalMethod
    void biometricsSubmitted(ForeignBiometricsSignal signal);

    @QueryMethod
    ForeignOnboardingState getState();
}
