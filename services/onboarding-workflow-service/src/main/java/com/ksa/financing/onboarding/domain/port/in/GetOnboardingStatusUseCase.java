package com.ksa.financing.onboarding.domain.port.in;

import com.ksa.financing.onboarding.domain.model.OnboardingState;

public interface GetOnboardingStatusUseCase {
    OnboardingState getStatus(String workflowId);
}
