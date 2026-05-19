package com.ksa.financing.notification.infrastructure.messaging;

import com.ksa.financing.notification.application.service.NotificationOrchestrator;
import com.ksa.financing.notification.infrastructure.external.NovuClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationKafkaListener {

    private final NotificationOrchestrator orchestrator;
    private final NovuClient novuClient;

    private static final Map<String, String> TOPIC_TO_EVENT_TYPE = Map.ofEntries(
            Map.entry("financing.payment.completed",            "PAYMENT_COMPLETED"),
            Map.entry("financing.loan.loan-application-approved","LOAN_APPROVED"),
            Map.entry("financing.loan.approved",                "LOAN_APPROVED"),
            Map.entry("financing.loan.loan-disbursed",          "LOAN_DISBURSED"),
            Map.entry("financing.loan.loan-rescheduled",        "LOAN_RESCHEDULED"),
            Map.entry("financing.loan.rescheduled",             "LOAN_RESCHEDULED"),
            Map.entry("financing.delinquency.assessment",       "DUNNING_STEP"),
            Map.entry("financing.fraud.detected",               "FRAUD_DETECTED"),
            Map.entry("financing.fraud.alert",                  "FRAUD_ALERT"),
            Map.entry("financing.blacklist.added",              "BLACKLIST_ADDED"),
            Map.entry("islamic-financing.identity.user-registered", "USER_REGISTERED")
    );

    @KafkaListener(topics = {
            "${kafka.topics.payment-completed:financing.payment.completed}",
            "${kafka.topics.loan-approved:financing.loan.loan-application-approved}",
            "${kafka.topics.loan-disbursed:financing.loan.loan-disbursed}",
            "${kafka.topics.loan-rescheduled:financing.loan.loan-rescheduled}",
            "${kafka.topics.delinquency-assessment:financing.delinquency.assessment}",
            "${kafka.topics.fraud-detected:financing.fraud.detected}",
            "${kafka.topics.fraud-alert:financing.fraud.alert}",
            "${kafka.topics.blacklist-added:financing.blacklist.added}",
            "${kafka.topics.user-registered:islamic-financing.identity.user-registered}"
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
                    if (customerId != null && fcmToken != null) {
                        log.info("Auto-registering FCM device for new user: {}", customerId);
                        novuClient.setSubscriberCredentials(customerId, fcmToken);
                    }
                }
                return;
            }

            orchestrator.processEvent(eventType, message);

        } catch (Exception e) {
            log.error("Error processing event from topic {}: {}", topic, e.getMessage(), e);
        }
    }
}
