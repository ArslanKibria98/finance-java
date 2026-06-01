package com.ksa.financing.collections.infrastructure.messaging;

import com.ksa.financing.collections.domain.model.PaymentAggregate;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.model.SettlementAggregate;
import com.ksa.financing.collections.domain.port.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaCollectionsEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-completed:financing.payment.completed}")
    private String paymentCompletedTopic;

    @Value("${kafka.topics.payment-failed:financing.payment.failed}")
    private String paymentFailedTopic;

    @Value("${kafka.topics.schedule-fully-paid:financing.repayment.schedule.fully-paid}")
    private String scheduleFullyPaidTopic;

    @Value("${kafka.topics.payment-applied:financing.repayment.payment-applied}")
    private String paymentAppliedTopic;

    @Value("${kafka.topics.settlement-initiated:financing.settlement.initiated}")
    private String settlementInitiatedTopic;

    @Value("${kafka.topics.settlement-confirmed:financing.settlement.confirmed}")
    private String settlementConfirmedTopic;

    @Value("${kafka.topics.loan-overdue:financing.loan.overdue}")
    private String loanOverdueTopic;

    @Value("${kafka.topics.installment-due-soon:financing.installment.due-soon}")
    private String installmentDueSoonTopic;

    @Override
    public void publishAll(List<Object> events) {
        for (Object event : events) {
            try {
                publishEvent(event);
            } catch (Exception e) {
                log.error("Failed to publish event: {} error: {}", event.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    private void publishEvent(Object event) {
        switch (event) {
            case PaymentAggregate.PaymentCompleted e -> {
                log.info("Publishing PaymentCompleted: paymentId={} loanId={}", e.paymentId(), e.loanId());
                kafkaTemplate.send(paymentCompletedTopic, e.loanId().toString(), e);
            }
            case PaymentAggregate.PaymentFailed e -> {
                log.warn("Publishing PaymentFailed: paymentId={} loanId={}", e.paymentId(), e.loanId());
                kafkaTemplate.send(paymentFailedTopic, e.loanId().toString(), e);
            }
            case RepaymentScheduleAggregate.PaymentApplied e -> {
                log.info("Publishing PaymentApplied: scheduleId={} loanId={}", e.scheduleId(), e.loanId());
                kafkaTemplate.send(paymentAppliedTopic, e.loanId().toString(), e);
            }
            case RepaymentScheduleAggregate.ScheduleFullyPaid e -> {
                log.info("Publishing ScheduleFullyPaid: scheduleId={} loanId={}", e.scheduleId(), e.loanId());
                kafkaTemplate.send(scheduleFullyPaidTopic, e.loanId().toString(), e);
            }
            case SettlementAggregate.SettlementInitiated e -> {
                log.info("Publishing SettlementInitiated: settlementId={} loanId={}", e.settlementId(), e.loanId());
                kafkaTemplate.send(settlementInitiatedTopic, e.loanId().toString(), e);
            }
            case SettlementAggregate.SettlementConfirmed e -> {
                log.info("Publishing SettlementConfirmed: settlementId={} loanId={}", e.settlementId(), e.loanId());
                kafkaTemplate.send(settlementConfirmedTopic, e.loanId().toString(), e);
            }
            case RepaymentScheduleAggregate.LoanOverdue e -> {
                log.info("Publishing LoanOverdue: loanId={} maxDpd={} overdueAmount={}",
                        e.loanId(), e.maxDpd(), e.overdueAmount());
                kafkaTemplate.send(loanOverdueTopic, e.loanId().toString(), e);
            }
            case RepaymentScheduleAggregate.InstallmentDueSoon e -> {
                log.info("Publishing InstallmentDueSoon: loanId={} installmentNumber={} dueDate={} daysUntilDue={}",
                        e.loanId(), e.installmentNumber(), e.dueDate(), e.daysUntilDue());
                kafkaTemplate.send(installmentDueSoonTopic, e.loanId().toString(), e);
            }
            default ->
                log.debug("Unhandled event type: {}", event.getClass().getSimpleName());
        }
    }
}
