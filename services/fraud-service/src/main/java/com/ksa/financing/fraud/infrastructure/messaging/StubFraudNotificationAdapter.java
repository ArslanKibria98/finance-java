package com.ksa.financing.fraud.infrastructure.messaging;

import com.ksa.financing.fraud.domain.model.fraud.FraudAlert;
import com.ksa.financing.fraud.domain.model.fraud.FraudEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.FraudNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stub notification adapter — logs alerts instead of publishing to Kafka.
 * Active only when fraud.notifications.adapter=stub (default: kafka adapter is primary).
 */
@Component
@ConditionalOnProperty(name = "fraud.notifications.adapter", havingValue = "stub")
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
