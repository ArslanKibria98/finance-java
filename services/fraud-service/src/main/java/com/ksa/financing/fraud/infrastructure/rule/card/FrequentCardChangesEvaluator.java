package com.ksa.financing.fraud.infrastructure.rule.card;

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
 * CARD_002 — Frequent Card Changes per Account.
 * Trigger: customer used 3+ distinct cards.
 */
@Component
@RequiredArgsConstructor
public class FrequentCardChangesEvaluator implements FraudRuleEvaluator {

    private final FraudEventRepository fraudEventRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.CARD_002;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.REPAYMENT || event.customerId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int max = rule.getParameterInt("max_distinct_cards", 3);
        long count = fraudEventRepository.countDistinctCardsByCustomer(
                event.tenantId(), event.customerId(),
                event.eventTimestamp().minusDays(180));
        if (count < max) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(), rule.blockCodeId(), rule.blockCode(),
                count + " distinct cards used",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
