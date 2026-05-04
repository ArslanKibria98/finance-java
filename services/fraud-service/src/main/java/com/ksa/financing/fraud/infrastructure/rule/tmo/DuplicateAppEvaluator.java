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
 * TMO_003 — Duplicate Loan Application Submission.
 * Trigger: same customer + same product + same amount within 1h.
 */
@Component
@RequiredArgsConstructor
public class DuplicateAppEvaluator implements FraudRuleEvaluator {

    private final FraudEventRepository fraudEventRepository;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_003;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.LOAN_APPLICATION
                || event.customerId() == null
                || event.loanProductType() == null
                || event.transactionAmount() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        int windowHours = rule.getParameterInt("time_window_hours", 1);
        var dupes = fraudEventRepository.findDuplicateApplications(
                event.tenantId(), event.customerId(),
                event.loanProductType(), event.transactionAmount(),
                event.eventTimestamp().minusHours(windowHours));
        long otherDupes = dupes.stream()
                .filter(e -> e.eventId() != null && !e.eventId().equals(event.eventId()))
                .count();
        if (otherDupes == 0) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(),
                "Duplicate application within " + windowHours + "h",
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
