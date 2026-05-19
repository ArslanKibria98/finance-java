package com.ksa.financing.fraud.infrastructure.messaging;

import com.ksa.financing.fraud.domain.model.fraud.FraudAlert;
import com.ksa.financing.fraud.domain.model.fraud.FraudDecision;
import com.ksa.financing.fraud.domain.model.fraud.FraudEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.FraudNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@Primary
@Slf4j
@RequiredArgsConstructor
public class KafkaFraudNotificationAdapter implements FraudNotificationPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.fraud-detected:financing.fraud.detected}")
    private String fraudDetectedTopic;

    @Value("${kafka.topics.fraud-alert:financing.fraud.alert}")
    private String fraudAlertTopic;

    @Value("${kafka.topics.blacklist-added:financing.blacklist.added}")
    private String blacklistAddedTopic;

    @Override
    public void notifyAlert(FraudAlert alert) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "FRAUD_ALERT");
        event.put("tenantId", alert.tenantId() != null ? alert.tenantId().toString() : null);
        event.put("customerId", alert.customerId());
        event.put("alertId", alert.id() != null ? alert.id().toString() : null);
        event.put("evaluationId", alert.evaluationId() != null ? alert.evaluationId().toString() : null);
        event.put("priority", alert.priority() != null ? alert.priority().name() : null);
        event.put("status", alert.status() != null ? alert.status().name() : null);
        event.put("decision", alert.decision() != null ? alert.decision().name() : null);
        event.put("summary", alert.summary());
        event.put("triggeringRuleId", alert.triggeringRuleId());
        event.put("occurredAt", Instant.now().toString());

        publish(fraudAlertTopic, alert.customerId(), event);
    }

    @Override
    public void notifyBlock(FraudEvaluationResult result) {
        Map<String, Object> event = new HashMap<>();
        boolean isBlocked = result.decision() == FraudDecision.BLOCK
                || result.decision() == FraudDecision.HOLD;
        event.put("eventType", isBlocked ? "FRAUD_DETECTED" : "FRAUD_ALERT");
        event.put("tenantId", result.tenantId() != null ? result.tenantId().toString() : null);
        event.put("customerId", result.customerId());
        event.put("evaluationId", result.id() != null ? result.id().toString() : null);
        event.put("eventId", result.eventId());
        event.put("decision", result.decision() != null ? result.decision().name() : null);
        event.put("blockType", result.blockType() != null ? result.blockType().name() : null);
        event.put("riskScore", result.compositeRiskScore());
        event.put("riskLevel", result.riskLevel());
        event.put("blockReason", result.blockReason());
        event.put("blockCode", result.blockCode());
        event.put("triggeredRuleCount", result.triggeredRules() != null ? result.triggeredRules().size() : 0);
        event.put("occurredAt", Instant.now().toString());

        String topic = isBlocked ? fraudDetectedTopic : fraudAlertTopic;
        publish(topic, result.customerId(), event);
    }

    public void notifyBlacklisted(String tenantId, String customerId,
                                  String blacklistType, String identifier, String reason) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "BLACKLIST_ADDED");
        event.put("tenantId", tenantId);
        event.put("customerId", customerId);
        event.put("blacklistType", blacklistType);
        event.put("identifier", identifier);
        event.put("reason", reason);
        event.put("occurredAt", Instant.now().toString());

        publish(blacklistAddedTopic, customerId, event);
    }

    private void publish(String topic, String key, Map<String, Object> event) {
        log.info("Publishing fraud event to topic={} key={} eventType={}",
                topic, key, event.get("eventType"));
        kafkaTemplate.send(topic, key, event)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish fraud event to {}: {}", topic, ex.getMessage(), ex);
                    } else {
                        log.debug("Fraud event published to {} partition={} offset={}",
                                topic, res.getRecordMetadata().partition(), res.getRecordMetadata().offset());
                    }
                });
    }
}
