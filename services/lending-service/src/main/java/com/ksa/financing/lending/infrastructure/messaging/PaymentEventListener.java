package com.ksa.financing.lending.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.lending.domain.model.LoanId;
import com.ksa.financing.lending.domain.port.out.LoanRepository;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaAmortizationScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final LoanRepository loanRepository;
    private final ObjectMapper objectMapper;
    private final JpaAmortizationScheduleRepository amortizationScheduleRepository;

    /**
     * Listens to financing.payment.completed event from collections-service.
     * Updates the loan's outstanding balance when a payment is completed.
     */
    @KafkaListener(
            topics = "${kafka.topics.payment-completed:financing.payment.completed}",
            groupId = "${spring.application.name}"
    )
    @Transactional
    public void onPaymentCompleted(Object message) {
        try {
            log.info("📥 RAW EVENT MESSAGE: {}", message);
            Map<String, Object> event = toEventMap(message);

            UUID tenantId = UUID.fromString((String) event.get("tenantId"));
            UUID loanId = UUID.fromString((String) event.get("loanId"));
            BigDecimal amount = new BigDecimal(event.get("amount").toString());

            log.info("📥 PARSED: PaymentCompleted event: tenantId={} loanId={} amount={}", tenantId, loanId, amount);

            // Load the loan from lending-service
            var loan = loanRepository.findById(tenantId, LoanId.of(loanId))
                    .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

            // Update the outstanding balance
            BigDecimal currentOutstanding = loan.getTotalOutstanding() != null
                    ? loan.getTotalOutstanding()
                    : BigDecimal.ZERO;
            BigDecimal newOutstanding = currentOutstanding.subtract(amount);
            if (newOutstanding.compareTo(BigDecimal.ZERO) < 0) {
                newOutstanding = BigDecimal.ZERO;
            }

            // Update loan with new outstanding balance
            loan.updateOutstandingBalance(newOutstanding);
            loanRepository.save(loan);
            applyPaymentToAmortizationSchedules(loanId, amount);

            log.info("✅ Updated loan outstanding: loanId={} outstanding={}", loanId, newOutstanding);

        } catch (Exception e) {
            log.error("❌ Error processing PaymentCompleted event: {}", e.getMessage(), e);
        }
    }

    /**
     * Alternative: Listen to financing.repayment.payment-applied event.
     * This is more granular and includes allocations.
     */
    @KafkaListener(
            topics = "${kafka.topics.payment-applied:financing.repayment.payment-applied}",
            groupId = "${spring.application.name}"
    )
    public void onPaymentApplied(Object message) {
        try {
            Map<String, Object> event = toEventMap(message);

            UUID tenantId = UUID.fromString((String) event.get("tenantId"));
            UUID loanId = UUID.fromString((String) event.get("loanId"));
            BigDecimal totalApplied = new BigDecimal(event.get("totalApplied").toString());

            log.info("📥 Received PaymentApplied event: loanId={} totalApplied={}", loanId, totalApplied);

            // Update the loan's outstanding balance
            var loan = loanRepository.findById(tenantId, LoanId.of(loanId))
                    .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

            BigDecimal currentOutstanding = loan.getTotalOutstanding() != null
                    ? loan.getTotalOutstanding()
                    : BigDecimal.ZERO;
            BigDecimal newOutstanding = currentOutstanding.subtract(totalApplied);
            if (newOutstanding.compareTo(BigDecimal.ZERO) < 0) {
                newOutstanding = BigDecimal.ZERO;
            }

            loan.updateOutstandingBalance(newOutstanding);
            loanRepository.save(loan);

            log.info("✅ Updated loan outstanding after payment applied: loanId={} outstanding={}",
                    loanId, newOutstanding);

        } catch (Exception e) {
            log.error("❌ Error processing PaymentApplied event: {}", e.getMessage(), e);
        }
    }

    private void applyPaymentToAmortizationSchedules(UUID loanId, BigDecimal paymentAmount) {
        List<String> payableStatuses = List.of("PENDING", "OVERDUE", "PARTIALLY_PAID");
        var schedules = amortizationScheduleRepository
                .findByLoanIdAndActiveOrderByInstallmentNumberAsc(loanId, true);
        BigDecimal remaining = paymentAmount;

        for (var schedule : schedules) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            var status = schedule.getPaymentStatus() != null ? schedule.getPaymentStatus() : "PENDING";
            if (!payableStatuses.contains(status)) {
                continue;
            }

            var installmentTotal = schedule.getTotalInstallment() != null
                    ? schedule.getTotalInstallment()
                    : BigDecimal.ZERO;
            if (installmentTotal.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            if (remaining.compareTo(installmentTotal) >= 0) {
                schedule.setPaymentStatus("PAID");
                remaining = remaining.subtract(installmentTotal);
            } else {
                schedule.setPaymentStatus("PARTIALLY_PAID");
                remaining = BigDecimal.ZERO;
            }
        }
    }

    private Map<String, Object> toEventMap(Object message) {
        if (message instanceof Map<?, ?> mapMessage) {
            return objectMapper.convertValue(mapMessage, Map.class);
        }
        if (message instanceof String stringMessage) {
            try {
                return objectMapper.readValue(stringMessage, Map.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to parse payment event JSON string", e);
            }
        }
        return objectMapper.convertValue(message, Map.class);
    }
}
