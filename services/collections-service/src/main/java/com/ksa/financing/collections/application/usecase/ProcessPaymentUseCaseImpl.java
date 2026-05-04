package com.ksa.financing.collections.application.usecase;

import com.ksa.financing.collections.application.service.DelinquencyEngine;
import com.ksa.financing.collections.domain.model.PaymentAggregate;
import com.ksa.financing.collections.domain.model.PaymentId;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.port.in.ProcessPaymentUseCase;
import com.ksa.financing.collections.domain.port.out.EventPublisher;
import com.ksa.financing.collections.domain.port.out.PaymentAllocationRepository;
import com.ksa.financing.collections.domain.port.out.PaymentRepository;
import com.ksa.financing.collections.domain.port.out.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessPaymentUseCaseImpl implements ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final EventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final DelinquencyEngine delinquencyEngine;

    @Override
    @Transactional
    public PaymentAggregate initiatePayment(InitiatePaymentCommand command) {
        log.info("Initiating payment for loan: {} tenant: {} amount: {} method: {}",
                command.loanId(), command.tenantId(), command.amount(), command.paymentMethod());

        // Idempotency check by key
        var existing = paymentRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Returning existing payment for idempotency key: {}", command.idempotencyKey());
            return existing.get();
        }

        // Idempotency check by invoice_id: DB has UNIQUE(tenant_id, invoice_id), so re-posting
        // the same invoice with a new key would explode on insert. Return existing payment instead.
        if (command.invoiceId() != null && !command.invoiceId().isBlank()) {
            var existingByInvoice = paymentRepository.findByInvoiceId(command.tenantId(), command.invoiceId());
            if (existingByInvoice.isPresent()) {
                log.info("Returning existing payment for invoice: {} (status={})",
                        command.invoiceId(), existingByInvoice.get().getStatus());
                return existingByInvoice.get();
            }
        }

        String paymentNumber = generatePaymentNumber(command.tenantId());

        var payment = PaymentAggregate.initiate(
                command.tenantId(),
                paymentNumber,
                command.loanId(),
                command.installmentId(),
                command.customerId(),
                command.invoiceId(),
                command.paymentMethod(),
                command.amount(),
                command.valueDate(),
                command.idempotencyKey());

        if (command.sourceWalletId() != null) {
            payment.setSourceWalletId(command.sourceWalletId());
        }

        var saved = paymentRepository.save(payment);
        eventPublisher.publishAll(saved.getUncommittedEvents());
        saved.markEventsAsCommitted();

        return saved;
    }

    @Override
    @Transactional
    public PaymentResult completePayment(CompletePaymentCommand command) {
        log.info("Completing payment: {} tenant: {}", command.paymentId(), command.tenantId());

        var payment = paymentRepository.findById(command.tenantId(), PaymentId.of(command.paymentId()))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + command.paymentId()));

        payment.complete(command.providerTransactionId());

        var scheduleOpt = scheduleRepository.findActiveByLoanId(command.tenantId(), payment.getLoanId());
        RepaymentScheduleAggregate schedule = null;
        int allocationCount = 0;

        if (scheduleOpt.isPresent()) {
            schedule = scheduleOpt.get();

            // Fix: Run delinquency assessment BEFORE payment application.
            // This ensures latest penalties (Type 2/3) are reflected in totalAmount
            // before the waterfall tries to settle them.
            var assessment = delinquencyEngine.tickAndAssess(
                    command.tenantId(), null /* productCode hook */,
                    schedule, LocalDate.now());
            
            eventPublisher.publishAll(List.<Object>of(assessment));
            log.info("Delinquency assessed loan={} dpd={} stage={} simah={} policy={}",
                    schedule.getLoanId(), assessment.loanDpd(), assessment.loanStage(),
                    assessment.reportToSimah(), assessment.policySource());

            List<RepaymentScheduleAggregate.PaymentAllocation> allocations;
            if (payment.getInstallmentId() != null) {
                allocations = schedule.applyPaymentToInstallment(
                        payment.getId().getValue(), payment.getInstallmentId(), payment.getAmount());
            } else {
                allocations = schedule.applyPayment(payment.getId().getValue(), payment.getAmount());
            }
            allocationRepository.saveAll(command.tenantId(), allocations);
            allocationCount = allocations.size();

            scheduleRepository.save(schedule);
        } else {
            log.warn("No active repayment schedule for loan {} — payment {} completed without allocation",
                    payment.getLoanId(), command.paymentId());
        }

        var savedPayment = paymentRepository.save(payment);
        entityManager.flush();

        eventPublisher.publishAll(savedPayment.getUncommittedEvents());
        savedPayment.markEventsAsCommitted();

        log.info("Payment completed: {} applied {} allocations", command.paymentId(), allocationCount);
        return new PaymentResult(savedPayment, schedule);
    }

    @Override
    @Transactional
    public PaymentAggregate failPayment(FailPaymentCommand command) {
        log.warn("Failing payment: {} reason: {}", command.paymentId(), command.failureMessage());

        var payment = paymentRepository.findById(command.tenantId(), PaymentId.of(command.paymentId()))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + command.paymentId()));

        payment.fail(command.failureCode(), command.failureMessage());

        var saved = paymentRepository.save(payment);
        eventPublisher.publishAll(saved.getUncommittedEvents());
        saved.markEventsAsCommitted();

        return saved;
    }

    @Override
    @Transactional
    public PaymentAggregate reversePayment(ReversePaymentCommand command) {
        log.warn("Reversing payment: {} reason: {}", command.paymentId(), command.reason());

        var payment = paymentRepository.findById(command.tenantId(), PaymentId.of(command.paymentId()))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + command.paymentId()));

        payment.reverse(command.reason());

        var saved = paymentRepository.save(payment);
        eventPublisher.publishAll(saved.getUncommittedEvents());
        saved.markEventsAsCommitted();

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentAggregate getPayment(UUID tenantId, UUID paymentId) {
        return paymentRepository.findById(tenantId, PaymentId.of(paymentId))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentAggregate getPaymentByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(tenantId, idempotencyKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found for idempotency key: " + idempotencyKey));
    }

    private String generatePaymentNumber(UUID tenantId) {
        return "PAY-" + System.currentTimeMillis();
    }
}
