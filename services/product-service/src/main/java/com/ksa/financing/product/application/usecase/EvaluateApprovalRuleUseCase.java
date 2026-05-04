package com.ksa.financing.product.application.usecase;

import com.ksa.financing.product.infrastructure.persistence.entity.ApprovalConditionJpaEntity;
import com.ksa.financing.product.infrastructure.persistence.entity.ApprovalWorkflowJpaEntity;
import com.ksa.financing.product.infrastructure.persistence.repository.JpaApprovalConditionRepository;
import com.ksa.financing.product.infrastructure.persistence.repository.JpaApprovalWorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Evaluates product_approval_workflows rules against application context (loan_amount,
 * credit_score, dbr_percentage, etc.) and returns the resulting decision:
 * AUTO_APPROVAL | MANUAL_APPROVAL | REJECTION_SCENARIO.
 *
 * <p>Rules are evaluated in priority order; the first workflow whose ALL conditions
 * match wins. If nothing matches, defaults to MANUAL_APPROVAL (safer than auto-approve).</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EvaluateApprovalRuleUseCase {

    private final JpaApprovalWorkflowRepository workflowRepo;
    private final JpaApprovalConditionRepository conditionRepo;

    public Decision evaluate(UUID tenantId, UUID productId, Map<String, Object> context) {
        var workflows = workflowRepo.findByProductIdAndTenantIdOrderByPriority(productId, tenantId);
        log.info("Evaluating {} approval workflows for product={} tenant={}",
                workflows.size(), productId, tenantId);

        for (var wf : workflows) {
            if (!wf.isActive()) continue;

            var conditions = conditionRepo.findByWorkflowIdOrderBySortOrder(wf.getId());
            if (conditions.isEmpty()) continue;

            boolean allMatch = true;
            for (var cond : conditions) {
                if (!evaluateCondition(cond, context)) {
                    allMatch = false;
                    break;
                }
            }

            if (allMatch) {
                log.info("Workflow matched: type={} name={} id={}",
                        wf.getWorkflowType(), wf.getNameEn(), wf.getId());
                return new Decision(
                        wf.getWorkflowType(),
                        wf.getId().toString(),
                        wf.getNameEn()
                );
            }
        }

        log.info("No workflow matched for product={}, defaulting to MANUAL_APPROVAL", productId);
        return new Decision("MANUAL_APPROVAL", null, "Default Manual Review");
    }

    private boolean evaluateCondition(ApprovalConditionJpaEntity cond, Map<String, Object> context) {
        var field = cond.getField();
        var operator = cond.getOperator();
        var rawValue = cond.getValue();
        var actual = context.get(field);

        if (actual == null) {
            log.debug("Condition field '{}' not present in context, skipping", field);
            return false;
        }

        try {
            BigDecimal actualNum = toBigDecimal(actual);
            BigDecimal expectedNum = toBigDecimal(stripJson(rawValue));

            return switch (operator.toUpperCase()) {
                case "EQ", "=", "EQUAL_TO", "EQUALS"
                        -> actualNum.compareTo(expectedNum) == 0;
                case "NE", "!=", "NOT_EQUAL", "NOT_EQUAL_TO"
                        -> actualNum.compareTo(expectedNum) != 0;
                case "LT", "<", "LESS_THAN"
                        -> actualNum.compareTo(expectedNum) < 0;
                case "LTE", "LE", "<=", "LESS_THAN_OR_EQUAL", "LESS_THAN_OR_EQUAL_TO"
                        -> actualNum.compareTo(expectedNum) <= 0;
                case "GT", ">", "GREATER_THAN"
                        -> actualNum.compareTo(expectedNum) > 0;
                case "GTE", "GE", ">=", "GREATER_THAN_OR_EQUAL", "GREATER_THAN_OR_EQUAL_TO"
                        -> actualNum.compareTo(expectedNum) >= 0;
                default -> {
                    log.warn("Unsupported operator '{}' for field '{}', treating as no match", operator, field);
                    yield false;
                }
            };
        } catch (NumberFormatException e) {
            log.warn("Non-numeric comparison for field {} not yet supported, skipping", field);
            return false;
        }
    }

    private String stripJson(String raw) {
        if (raw == null) return "";
        var s = raw.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(v.toString());
    }

    public record Decision(
            String decision,
            String workflowId,
            String workflowName
    ) {
        public boolean isAuto() { return "AUTO_APPROVAL".equals(decision); }
        public boolean isManual() { return "MANUAL_APPROVAL".equals(decision); }
        public boolean isReject() { return "REJECTION_SCENARIO".equals(decision); }
    }
}
