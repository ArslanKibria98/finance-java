package com.ksa.financing.fraud.infrastructure.rule.tmo;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;
import com.ksa.financing.fraud.domain.model.fraud.FraudEventType;
import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.out.LendingPort;
import com.ksa.financing.fraud.domain.service.FraudRuleEvaluator;
import com.ksa.financing.fraud.infrastructure.rule.EvaluatorScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * TMO_011 — Installment Payment Amount Discrepancy.
 * Trigger: payment amount differs from scheduled installment by > 5% tolerance.
 */
@Component
@RequiredArgsConstructor
public class InstallmentDiscrepancyEvaluator implements FraudRuleEvaluator {

    private final LendingPort lendingPort;

    @Override
    public FraudRuleId getRuleId() {
        return FraudRuleId.TMO_011;
    }

    @Override
    public RuleEvaluationResult evaluate(FraudEvent event, FraudRule rule) {
        if (event.eventType() != FraudEventType.REPAYMENT
                || event.transactionAmount() == null
                || event.loanApplicationId() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        BigDecimal tolerancePercent = new BigDecimal(rule.getParameterValue("tolerance_percent", "5"));
        var scheduled = lendingPort.fetchScheduledInstallment(event.tenantId(), event.loanApplicationId(), 0);
        if (scheduled.isEmpty() || scheduled.get().scheduledAmount() == null) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        BigDecimal expected = scheduled.get().scheduledAmount();
        BigDecimal diff = event.transactionAmount().subtract(expected).abs();
        BigDecimal threshold = expected.multiply(tolerancePercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        if (diff.compareTo(threshold) <= 0) {
            return RuleEvaluationResult.notTriggered(rule.ruleId());
        }
        return RuleEvaluationResult.triggered(rule.ruleId(), rule.defaultAction(), rule.blockType(), rule.blockCodeId(), rule.blockCode(),
                "Paid " + event.transactionAmount() + " vs scheduled " + expected,
                EvaluatorScores.forDecision(rule.defaultAction()));
    }
}
