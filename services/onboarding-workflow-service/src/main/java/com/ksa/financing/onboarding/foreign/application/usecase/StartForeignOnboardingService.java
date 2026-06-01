package com.ksa.financing.onboarding.foreign.application.usecase;

import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingRequest;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingState;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;
import com.ksa.financing.onboarding.foreign.domain.port.in.StartForeignOnboardingUseCase;
import com.ksa.financing.onboarding.shared.duplicate.OnboardingDuplicateChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StartForeignOnboardingService implements StartForeignOnboardingUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartForeignOnboardingService.class);

    private final ForeignWorkflowClient client;
    private final OnboardingDuplicateChecker duplicateChecker;

    public StartForeignOnboardingService(ForeignWorkflowClient client,
                                         OnboardingDuplicateChecker duplicateChecker) {
        this.client = client;
        this.duplicateChecker = duplicateChecker;
    }

    @Override
    public StartForeignResult start(ForeignOnboardingRequest request) {
        duplicateChecker.assertNotAlreadyRegistered(request.email(), request.mobileNumber());
        String workflowId = client.start(request);
        ForeignOnboardingState state = client.awaitStep(workflowId, ForeignOnboardingStep.OTP_SENT, 20, 250);
        if (state.getCurrentStep() == ForeignOnboardingStep.FAILED) {
            log.warn("Foreign onboarding initiate failed: workflowId={} reason={}",
                    workflowId, state.getFailureReason());
            return new StartForeignResult(workflowId, ForeignOnboardingStep.FAILED,
                    null, null, null, null, "FAILED", state.getFailureReason());
        }
        return new StartForeignResult(
                workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : ForeignOnboardingStep.OTP_SENT,
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
