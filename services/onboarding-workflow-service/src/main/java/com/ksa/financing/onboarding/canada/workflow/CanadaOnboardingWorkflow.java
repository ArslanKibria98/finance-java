package com.ksa.financing.onboarding.canada.workflow;

import com.ksa.financing.onboarding.canada.domain.model.BiometricsSignal;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingRequest;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingResult;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;
import com.ksa.financing.onboarding.canada.domain.model.CanadaSetPinSignal;
import com.ksa.financing.onboarding.canada.domain.model.DocConfirmedSignal;
import com.ksa.financing.onboarding.canada.domain.model.DocSelectedSignal;
import com.ksa.financing.onboarding.canada.domain.model.DocSubmittedSignal;
import com.ksa.financing.onboarding.canada.domain.model.DualOtpVerifiedSignal;
import com.ksa.financing.onboarding.canada.domain.model.SelfieSubmittedSignal;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Signal-driven onboarding workflow for Canada customers.
 *
 * <p>Mirrors the structure of the KSA {@code CustomerOnboardingWorkflow} but the steps
 * are Canada-specific:</p>
 * <ol>
 *   <li>Send dual OTP to mobile + email (automated)</li>
 *   <li>Wait for dual OTP verified signal</li>
 *   <li>Wait for document type selection (ID or Passport)</li>
 *   <li>Wait for document image upload → Facia document-verification</li>
 *   <li>Wait for user confirmation of extracted fields</li>
 *   <li>Wait for selfie upload → Facia face-match → customer + wallet creation</li>
 *   <li>Wait for PIN setup</li>
 *   <li>Wait for biometrics enable/skip → COMPLETED</li>
 * </ol>
 */
@WorkflowInterface
public interface CanadaOnboardingWorkflow {

    @WorkflowMethod
    CanadaOnboardingResult execute(CanadaOnboardingRequest request);

    @SignalMethod
    void dualOtpVerified(DualOtpVerifiedSignal signal);

    @SignalMethod
    void documentSelected(DocSelectedSignal signal);

    @SignalMethod
    void documentSubmitted(DocSubmittedSignal signal);

    @SignalMethod
    void documentConfirmed(DocConfirmedSignal signal);

    @SignalMethod
    void selfieSubmitted(SelfieSubmittedSignal signal);

    @SignalMethod
    void pinSubmitted(CanadaSetPinSignal signal);

    @SignalMethod
    void biometricsSubmitted(BiometricsSignal signal);

    @QueryMethod
    CanadaOnboardingState getState();
}
