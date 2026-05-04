package com.ksa.financing.fraud.infrastructure.rule.tmo;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.FraudEventRepository;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * TMO_014 — Round-Number Transaction Pattern.
 * Trigger: 3+ consecutive round-number transactions (multiples of 500).
 */
@Component
@RequiredArgsConstructor
public class RoundNumberPatternEvaluator implements FraudRuleEvaluator {

    private final FraudEventRepository fraudEventRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_014;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.customerId() == null || event.transactionAmount() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int round = rule.getParameterInt("round_denomination", 500);
        int min = rule.getParameterInt("min_consecutive", 3);

        BigDecimal denom = BigDecimal.valueOf(round);
        if (event.transactionAmount().remainder(denom).signum() != 0) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        var last = fraudEventRepository.findLastNByCustomer(event.tenantId(), event.customerId(), min - 1);
        long roundCount = 1; // current event
        for (var e : last) {
            if (e.transactionAmount() == null
                    || e.transactionAmount().remainder(denom).signum() != 0) {
                break;
            }
            roundCount++;
        }
        if (roundCount < min) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                roundCount + " consecutive round-number transactions",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
