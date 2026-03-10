package com.ksa.financing.onboarding.workflow.activity.impl;

import com.ksa.islamic.orchestration.activity.notification.NotificationActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub implementation of the notification activity.
 *
 * This activity is NOT a Spring bean. It is instantiated manually and registered
 * with the Temporal Worker. Dependencies are provided via constructor injection.
 *
 * Currently a stub that logs the notification intent and returns success.
 * In production, this will be replaced with an actual SMS gateway integration
 * (e.g., Unifonic for KSA, or Twilio as a fallback).
 */
public class NotificationActivityImpl implements NotificationActivity {

    private static final Logger log = LoggerFactory.getLogger(NotificationActivityImpl.class);

    private static final String CHANNEL_SMS = "SMS";

    public NotificationActivityImpl() {
        // No dependencies required for stub implementation
    }

    @Override
    public NotificationResult sendWelcomeNotification(NotificationInput input) {
        String maskedMobile = maskMobile(input.mobileNumber());
        log.info("Sending welcome SMS to customer {} at mobile {}", input.customerId(), maskedMobile);

        try {
            // STUB: In production, this would call an SMS gateway API
            // Example integration points:
            // - Unifonic (KSA local provider)
            // - Twilio (international fallback)
            // - Internal notification service via Kafka event

            log.info("Welcome notification sent successfully to customer {} via {} (STUB)",
                input.customerId(), CHANNEL_SMS);
            return new NotificationResult(true, CHANNEL_SMS);

        } catch (Exception e) {
            log.error("Failed to send welcome notification to customer {} at mobile {}",
                input.customerId(), maskedMobile, e);
            return new NotificationResult(false, null);
        }
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return "****";
        }
        return "****" + mobile.substring(mobile.length() - 4);
    }
}
