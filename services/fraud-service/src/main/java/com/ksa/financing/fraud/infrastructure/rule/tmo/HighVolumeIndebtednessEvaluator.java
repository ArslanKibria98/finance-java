package com.ksa.financing.fraud.infrastructure.rule.tmo;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.fraud.FraudEventType;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.FraudEventRepository;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * TMO_015 — High-Volume Proof of Indebtedness Requests.
 * Trigger: > 3 PROOF_OF_INDEBTEDNESS requests within 7 days.
 */
@Component
@RequiredArgsConstructor
public class HighVolumeIndebtednessEvaluator implements FraudRuleEvaluator {

    private final FraudEventRepository fraudEventRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_015;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.PROOF_OF_INDEBTEDNESS || event.customerId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int max = rule.getParameterInt("max_requests", 3);
        int days = rule.getParameterInt("time_window_days", 7);

        long count = fraudEventRepository.countByCustomerAndType(
                event.tenantId(), event.customerId(),
                FraudEventType.PROOF_OF_INDEBTEDNESS.name(),
                event.eventTimestamp().minusDays(days));
        if (count <= max) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                count + " indebtedness requests in " + days + " days",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
