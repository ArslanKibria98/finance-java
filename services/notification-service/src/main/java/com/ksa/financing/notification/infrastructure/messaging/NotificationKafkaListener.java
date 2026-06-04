package com.ksa.financing.notification.infrastructure.messaging;

import com.ksa.financing.notification.application.service.NotificationOrchestrator;
import com.ksa.financing.notification.infrastructure.external.NovuClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationKafkaListener {

    private final NotificationOrchestrator orchestrator;
    private final NovuClient novuClient;

    @Value("${notification.thresholds.large-payment-sar:50000}")
    private BigDecimal largePaymentThreshold;

    private static final Map<String, String> TOPIC_TO_EVENT_TYPE = Map.ofEntries(
            Map.entry("financing.payment.completed",             "PAYMENT_COMPLETED"),
            Map.entry("financing.payment.failed",                "PAYMENT_FAILED"),
            Map.entry("financing.loan.overdue",                  "PAYMENT_OVERDUE"),
            Map.entry("financing.loan.loan-application-approved", "LOAN_APPROVED"),
            Map.entry("financing.loan.approved",                 "LOAN_APPROVED"),
            Map.entry("financing.loan.loan-disbursed",           "LOAN_DISBURSED"),
            Map.entry("financing.loan.loan-rescheduled",         "LOAN_RESCHEDULED"),
            Map.entry("financing.loan.rescheduled",              "LOAN_RESCHEDULED"),
            Map.entry("financing.loan.approval.required",        "MANUAL_APPROVAL_REQUIRED"),
            Map.entry("financing.loan.approval.sla-breached",    "APPROVAL_SLA_BREACHED"),
            Map.entry("financing.installment.due-soon",          "PAYMENT_DUE"),
            Map.entry("financing.delinquency.assessment",        "DUNNING_STEP"),
            Map.entry("financing.fraud.detected",                "FRAUD_DETECTED"),
            Map.entry("financing.fraud.alert",                   "FRAUD_ALERT"),
            Map.entry("financing.blacklist.added",               "BLACKLIST_ADDED"),
            Map.entry("islamic-financing.identity.user-registered", "USER_REGISTERED"),
            Map.entry("islamic-financing.identity.user-login",      "USER_LOGIN"),
            Map.entry("financing.wallet.transfer.completed",        "FUNDS_SENT"),
            Map.entry("financing.wallet.transfer.received",         "FUNDS_RECEIVED")
    );

    @KafkaListener(topics = {
            "${kafka.topics.payment-completed:financing.payment.completed}",
            "${kafka.topics.payment-failed:financing.payment.failed}",
            "${kafka.topics.loan-overdue:financing.loan.overdue}",
            "${kafka.topics.loan-approved:financing.loan.loan-application-approved}",
            "${kafka.topics.loan-disbursed:financing.loan.loan-disbursed}",
            "${kafka.topics.loan-rescheduled:financing.loan.loan-rescheduled}",
            "${kafka.topics.approval-required:financing.loan.approval.required}",
            "${kafka.topics.approval-sla-breached:financing.loan.approval.sla-breached}",
            "${kafka.topics.installment-due-soon:financing.installment.due-soon}",
            "${kafka.topics.delinquency-assessment:financing.delinquency.assessment}",
            "${kafka.topics.fraud-detected:financing.fraud.detected}",
            "${kafka.topics.fraud-alert:financing.fraud.alert}",
            "${kafka.topics.blacklist-added:financing.blacklist.added}",
            "${kafka.topics.user-registered:islamic-financing.identity.user-registered}",
            "${kafka.topics.user-login:islamic-financing.identity.user-login}",
            "${kafka.topics.wallet-transfer-completed:financing.wallet.transfer.completed}",
            "${kafka.topics.wallet-transfer-received:financing.wallet.transfer.received}"
    }, groupId = "${spring.application.name}")
    public void onBusinessEvent(@Payload Map<String, Object> message,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received event topic={} payload={}", topic, message);

        try {
            String eventType = TOPIC_TO_EVENT_TYPE.getOrDefault(topic,
                    (String) message.getOrDefault("eventType", "GENERIC_EVENT"));

            if ("USER_REGISTERED".equals(eventType)) {
                Map<String, Object> payload = (Map<String, Object>) message.get("payload");
                if (payload != null) {
                    String customerId = (String) payload.get("userId");
                    String fcmToken = (String) payload.get("fcmToken");
                    String providerId = (String) payload.get("providerId");
                    if (customerId != null && fcmToken != null) {
                        log.info("Auto-registering FCM device for new user: {} provider: {}", customerId, providerId);
                        novuClient.setSubscriberCredentials(customerId, fcmToken, providerId);
                    }
                }
                return;
            }

            // Identity events ship payload nested under "payload"; orchestrator expects flat keys.
            Map<String, Object> orchestratorPayload = message;
            if ("USER_LOGIN".equals(eventType) && message.get("payload") instanceof Map<?, ?> nested) {
                orchestratorPayload = (Map<String, Object>) nested;
            }

            orchestrator.processEvent(eventType, orchestratorPayload);

            // Derived admin event: LARGE_PAYMENT when PaymentCompleted amount exceeds threshold
            if ("PAYMENT_COMPLETED".equals(eventType)) {
                BigDecimal amount = extractAmount(message);
                if (amount != null && amount.compareTo(largePaymentThreshold) > 0) {
                    log.info("Large payment detected (amount={} > threshold={}), triggering admin event",
                            amount, largePaymentThreshold);
                    orchestrator.processEvent("LARGE_PAYMENT", message);
                }
            }

        } catch (Exception e) {
            log.error("Error processing event from topic {}: {}", topic, e.getMessage(), e);
        }
    }

    /**
     * Syncs the Novu subscriber's identity (email, name) from customer profile events so Novu's
     * email channel step has a recipient. Separate listener — these events do NOT drive
     * customer-facing notifications themselves (no routing rules), only subscriber upkeep.
     * Subscriber key = customerId (matches the trigger flow target).
     */
    @KafkaListener(topics = {
            "${kafka.topics.customer-created:islamic-financing.customer.customer-created}",
            "${kafka.topics.customer-updated:islamic-financing.customer.customer-updated}"
    }, groupId = "${spring.application.name}")
    public void onCustomerProfileEvent(@Payload Map<String, Object> message,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            Object rawPayload = message.get("payload");
            if (!(rawPayload instanceof Map<?, ?> payload)) {
                log.debug("customer profile event on {} has no payload — skipping subscriber sync", topic);
                return;
            }
            String customerId = asString(payload.get("customerId"));
            String email = asString(payload.get("email"));
            String firstNameField = asString(payload.get("firstName"));
            String fullName = asString(payload.get("fullName"));
            if (customerId == null) {
                log.debug("customer profile event on {} missing customerId — skipping", topic);
                return;
            }

            String firstName = null, lastName = null;
            if (firstNameField != null && !firstNameField.isBlank()) {
                // Preferred: customer's own name only. last_name holds the father's name and must
                // not be stored on the Novu subscriber — leave lastName null.
                firstName = firstNameField;
            } else if (fullName != null && !fullName.isBlank()) {
                // Fallback for older events that only carried fullName.
                String[] parts = fullName.trim().split("\\s+", 2);
                firstName = parts[0];
                lastName = parts.length > 1 ? parts[1] : null;
            }
            novuClient.upsertSubscriber(customerId, email, null, firstName, lastName);
        } catch (Exception e) {
            log.error("Error syncing Novu subscriber from topic {}: {}", topic, e.getMessage(), e);
        }
    }

    private static String asString(Object val) {
        return val == null ? null : val.toString();
    }

    private BigDecimal extractAmount(Map<String, Object> message) {
        Object amt = message.get("amount");
        if (amt == null) return null;
        if (amt instanceof BigDecimal bd) return bd;
        if (amt instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(amt.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
