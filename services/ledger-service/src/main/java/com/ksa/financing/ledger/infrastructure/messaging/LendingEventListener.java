package com.ksa.financing.ledger.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes loan lifecycle events from lending-service and posts
 * corresponding journal entries to the general ledger.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LendingEventListener {

    private final GlPostingService glPostingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.loan-disbursed:financing.loan.loan-disbursed}",
            groupId = "${spring.application.name}"
    )
    public void onLoanDisbursed(@Payload Map<String, Object> message) {
        try {
            Map<String, Object> event = toMap(message);
            var tenantId = uuid(event, "tenantId");
            var loanId = uuid(event, "loanId");
            var customerId = uuid(event, "customerId");
            var amount = bigDecimal(event, "amount");
            var disbursementDate = localDate(event, "disbursementDate");
            if (disbursementDate == null) {
                disbursementDate = LocalDate.now();
            }
            var idempotencyKey = "loan-disbursed:" + loanId;

            log.info("Received LoanDisbursed: tenantId={} loanId={} amount={} date={}",
                    tenantId, loanId, amount, disbursementDate);

            glPostingService.postDisbursement(tenantId, loanId, customerId, amount,
                    disbursementDate, idempotencyKey);
        } catch (Exception e) {
            log.error("Error processing LoanDisbursed event: {}", e.getMessage(), e);
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
                throw new IllegalArgumentException("Unable to parse lending event JSON", e);
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
        if (raw instanceof List<?> parts && parts.size() >= 3) {
            return LocalDate.of(
                    ((Number) parts.get(0)).intValue(),
                    ((Number) parts.get(1)).intValue(),
                    ((Number) parts.get(2)).intValue());
        }
        return LocalDate.parse(raw.toString());
    }
}
