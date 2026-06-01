package com.ksa.financing.onboarding.guest.application.usecase;

import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingRequest;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingState;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingStep;
import com.ksa.financing.onboarding.guest.domain.port.in.StartGuestOnboardingUseCase;
import com.ksa.financing.onboarding.shared.duplicate.OnboardingDuplicateChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StartGuestOnboardingService implements StartGuestOnboardingUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartGuestOnboardingService.class);

    private final GuestWorkflowClient client;
    private final OnboardingDuplicateChecker duplicateChecker;

    public StartGuestOnboardingService(GuestWorkflowClient client,
                                       OnboardingDuplicateChecker duplicateChecker) {
        this.client = client;
        this.duplicateChecker = duplicateChecker;
    }

    @Override
    public StartGuestResult start(GuestOnboardingRequest request) {
        duplicateChecker.assertNotAlreadyRegistered(request.email(), request.mobileNumber());
        String workflowId = client.start(request);
        GuestOnboardingState state = client.awaitStep(workflowId, GuestOnboardingStep.OTP_SENT, 20, 250);
        if (state.getCurrentStep() == GuestOnboardingStep.FAILED) {
            log.warn("Guest onboarding initiate failed: workflowId={} reason={}",
                    workflowId, state.getFailureReason());
            return new StartGuestResult(workflowId, GuestOnboardingStep.FAILED,
                    null, null, null, null, "FAILED", state.getFailureReason());
        }
        return new StartGuestResult(
                workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : GuestOnboardingStep.OTP_SENT,
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
