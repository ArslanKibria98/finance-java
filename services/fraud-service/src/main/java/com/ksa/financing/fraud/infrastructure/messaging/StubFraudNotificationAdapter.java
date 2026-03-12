package com.ksa.financing.fraud.infrastructure.messaging;

import com.ksa.financing.fraud.domain.model.fraud.FraudAlert;
import com.ksa.financing.fraud.domain.model.fraud.FraudEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.FraudNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub notification adapter — logs alerts instead of publishing to Kafka.
 * Will be replaced with real KafkaFraudAlertPublisher in Sprint 9.
 */
@Component
@Slf4j
public class StubFraudNotificationAdapter implements FraudNotificationPort {

    @Override
    public void notifyAlert(FraudAlert alert) {
        log.info("[STUB] Fraud alert generated: alertId={} ruleId={} priority={} customer={}",
                alert.id(), alert.triggeringRuleId(), alert.priority(), alert.customerId());
    }

    @Override
    public void notifyBlock(FraudEvaluationResult result) {
        log.info("[STUB] Fraud block notification: eventId={} decision={} score={} customer={}",
                result.eventId(), result.decision(), result.compositeRiskScore(), result.customerId());
    }
}
