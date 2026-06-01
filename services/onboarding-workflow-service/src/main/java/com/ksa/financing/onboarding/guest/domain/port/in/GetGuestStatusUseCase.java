package com.ksa.financing.onboarding.guest.domain.port.in;

import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingState;

public interface GetGuestStatusUseCase {

    GuestOnboardingState getStatus(String workflowId);

    GuestOnboardingState getStatusByEmail(String email);
}
