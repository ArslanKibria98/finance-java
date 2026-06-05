package com.ksa.financing.customer.domain.port.out;

import java.util.List;
import java.util.Map;

/**
 * Output port for fetching onboarding workflow status from Onboarding Workflow Service.
 */
public interface OnboardingServicePort {

    /**
     * Get onboarding workflow status and steps for a national ID.
     */
    Map<String, Object> getOnboardingStatus(String nationalId, String accessToken);
}
