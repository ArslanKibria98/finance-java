package com.ksa.financing.onboarding.domain.port.in;

import com.ksa.financing.onboarding.domain.model.OnboardingState;

import java.util.List;

public interface GetOnboardingStatusUseCase {
    OnboardingState getStatus(String workflowId);
    OnboardingState getStatusByMobile(String mobileNumber);
    List<OnboardingState> listActiveOnboardings();
}
