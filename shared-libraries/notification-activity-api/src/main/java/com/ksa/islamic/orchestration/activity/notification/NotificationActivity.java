package com.ksa.islamic.orchestration.activity.notification;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for sending welcome notifications to newly onboarded customers.
 *
 * Currently implemented as a STUB that logs the notification and returns success.
 * In production, this will integrate with an SMS gateway (e.g., Unifonic, Twilio)
 * to send a welcome SMS to the customer's verified mobile number.
 */
@ActivityInterface
public interface NotificationActivity {

    @ActivityMethod
    NotificationResult sendWelcomeNotification(NotificationInput input);

    record NotificationInput(
        String customerId,
        String mobileNumber,
        String fullName,
        String tenantId
    ) {}

    record NotificationResult(
        boolean sent,
        String channel
    ) {}
}
