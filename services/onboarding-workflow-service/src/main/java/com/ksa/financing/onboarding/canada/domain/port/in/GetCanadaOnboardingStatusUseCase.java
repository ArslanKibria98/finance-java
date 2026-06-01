package com.ksa.financing.onboarding.canada.domain.port.in;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;

public interface GetCanadaOnboardingStatusUseCase {

    CanadaOnboardingState getStatus(String workflowId);

    CanadaOnboardingState getStatusByEmail(String email);
}
