package com.ksa.financing.onboarding.foreign.domain.port.in;

import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingState;

public interface GetForeignStatusUseCase {

    ForeignOnboardingState getStatus(String workflowId);

    ForeignOnboardingState getStatusByEmail(String email);
}
