package com.ksa.financing.ledger.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes payment and settlement events from collections-service and posts
 * corresponding journal entries to the general ledger.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionsEventListener {

    private final GlPostingService glPostingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.payment-completed:financing.payment.completed}",
            groupId = "${spring.application.name}"
    )
    public void onPaymentCompleted(Object message) {
        try {
            Map<String, Object> event = toMap(message);
            var tenantId = uuid(event, "tenantId");
            var loanId = uuid(event, "loanId");
            var customerId = uuid(event, "customerId");
            var paymentId = uuid(event, "paymentId");
            var amount = bigDecimal(event, "amount");
            var idempotencyKey = "payment-completed:" + paymentId;

            log.info("Received PaymentCompleted: tenantId={} loanId={} paymentId={} amount={}",
                    tenantId, loanId, paymentId, amount);

            glPostingService.postRepayment(tenantId, loanId, customerId, paymentId, amount,
                    LocalDate.now(), idempotencyKey);
        } catch (Exception e) {
            log.error("Error processing PaymentCompleted event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.settlement-confirmed:financing.settlement.confirmed}",
            groupId = "${spring.application.name}"
    )
    public void onSettlementConfirmed(Object message) {
        try {
            Map<String, Object> event = toMap(message);
            var tenantId = uuid(event, "tenantId");
            var loanId = uuid(event, "loanId");
            var customerId = uuid(event, "customerId");
            var settlementId = uuid(event, "settlementId");
            var amount = bigDecimal(event, "amount");
            var ibraAmount = bigDecimal(event, "ibraAmount");
            var idempotencyKey = "settlement-confirmed:" + settlementId;

            log.info("Received SettlementConfirmed: tenantId={} loanId={} settlementId={} amount={} ibra={}",
                    tenantId, loanId, settlementId, amount, ibraAmount);

            glPostingService.postSettlement(tenantId, loanId, customerId, settlementId,
                    amount, ibraAmount, LocalDate.now(), idempotencyKey);
        } catch (Exception e) {
            log.error("Error processing SettlementConfirmed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.loan-overdue:financing.loan.overdue}",
            groupId = "${spring.application.name}"
    )
    public void onLoanOverdue(Object message) {
        try {
            Map<String, Object> event = toMap(message);
            var tenantId = uuid(event, "tenantId");
            var loanId = uuid(event, "loanId");
            var customerId = uuid(event, "customerId");
            var overdueAmount = bigDecimal(event, "overdueAmount");
            var reportedOn = localDate(event, "overdueDate");
            if (reportedOn == null) {
                reportedOn = LocalDate.now();
            }
            var idempotencyKey = "loan-overdue:" + loanId + ":" + reportedOn;

            log.info("Received LoanOverdue: tenantId={} loanId={} overdueAmount={} date={}",
                    tenantId, loanId, overdueAmount, reportedOn);

            if (overdueAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("Skipping provision posting — overdueAmount is zero for loanId={}", loanId);
                return;
            }
            glPostingService.postOverdueProvision(tenantId, loanId, customerId, overdueAmount,
                    reportedOn, idempotencyKey);
        } catch (Exception e) {
            log.error("Error processing LoanOverdue event: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object message) {
        if (message instanceof Map<?, ?> m) {
            return objectMapper.convertValue(m, Map.class);
        }
        if (message instanceof String s) {
            try {
                return objectMapper.readValue(s, Map.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to parse collections event JSON", e);
            }
        }
        return objectMapper.convertValue(message, Map.class);
    }

    private UUID uuid(Map<String, Object> event, String key) {
        var raw = event.get(key);
        if (raw == null) return null;
        return UUID.fromString(raw.toString());
    }

    private BigDecimal bigDecimal(Map<String, Object> event, String key) {
        var raw = event.get(key);
        if (raw == null) return BigDecimal.ZERO;
        return new BigDecimal(raw.toString());
    }

    private LocalDate localDate(Map<String, Object> event, String key) {
        var raw = event.get(key);
        if (raw == null) return null;
        return LocalDate.parse(raw.toString());
    }
}
