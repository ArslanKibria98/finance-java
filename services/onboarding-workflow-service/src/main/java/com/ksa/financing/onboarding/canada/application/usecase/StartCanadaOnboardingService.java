package com.ksa.financing.onboarding.canada.application.usecase;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingRequest;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.canada.domain.port.in.StartCanadaOnboardingUseCase;
import com.ksa.financing.onboarding.shared.duplicate.OnboardingDuplicateChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StartCanadaOnboardingService implements StartCanadaOnboardingUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartCanadaOnboardingService.class);

    private final CanadaWorkflowClient client;
    private final OnboardingDuplicateChecker duplicateChecker;

    public StartCanadaOnboardingService(CanadaWorkflowClient client,
                                        OnboardingDuplicateChecker duplicateChecker) {
        this.client = client;
        this.duplicateChecker = duplicateChecker;
    }

    @Override
    public StartCanadaOnboardingResult start(CanadaOnboardingRequest request) {
        duplicateChecker.assertNotAlreadyRegistered(request.email(), request.mobileNumber());
        String workflowId = client.start(request);
        CanadaOnboardingState state = client.awaitStep(workflowId, CanadaOnboardingStep.OTP_SENT, 20, 250);
        if (state.getCurrentStep() == CanadaOnboardingStep.FAILED) {
            log.warn("Canada onboarding initiate failed: workflowId={} reason={}",
                    workflowId, state.getFailureReason());
            return new StartCanadaOnboardingResult(workflowId, CanadaOnboardingStep.FAILED,
                    null, null, null, null, "FAILED", state.getFailureReason());
        }
        return new StartCanadaOnboardingResult(
                workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : CanadaOnboardingStep.OTP_SENT,
                state.getMobileOtpRequestId(),
                state.getEmailOtpRequestId(),
                maskMobile(state.getMobileNumber()),
                maskEmail(state.getEmail()),
                "OTP_SENT",
                null);
    }

    private static String maskMobile(String m) {
        if (m == null || m.length() < 4) return "****";
        return "****" + m.substring(m.length() - 4);
    }

    private static String maskEmail(String e) {
        if (e == null || !e.contains("@")) return "****";
        int at = e.indexOf('@');
        String local = e.substring(0, at);
        String domain = e.substring(at);
        if (local.length() <= 2) return "**" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
