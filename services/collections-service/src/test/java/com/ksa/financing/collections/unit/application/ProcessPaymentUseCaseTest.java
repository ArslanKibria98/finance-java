package com.ksa.financing.collections.unit.application;

import com.ksa.financing.collections.application.usecase.ProcessPaymentUseCaseImpl;
import com.ksa.financing.collections.domain.model.*;
import com.ksa.financing.collections.domain.port.in.ProcessPaymentUseCase;
import com.ksa.financing.collections.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessPaymentUseCaseImpl")
class ProcessPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @Mock
    private PaymentAllocationRepository allocationRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ProcessPaymentUseCaseImpl useCase;

    private UUID tenantId;
    private UUID loanId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        loanId = UUID.randomUUID();
    }

    @Test
    @DisplayName("initiatePayment returns existing payment on duplicate idempotency key")
    void returnsExistingOnDuplicateIdempotencyKey() {
        var idempotencyKey = "key-123";
        var existingPayment = buildPayment(tenantId, loanId, idempotencyKey);

        when(paymentRepository.findByIdempotencyKey(tenantId, idempotencyKey))
                .thenReturn(Optional.of(existingPayment));

        var customerId = UUID.randomUUID();
        var command = new ProcessPaymentUseCase.InitiatePaymentCommand(
                tenantId, loanId, UUID.randomUUID(), customerId,
                new BigDecimal("1000"), PaymentMethod.WALLET_MANUAL,
                "INV-001", LocalDate.now(), null, idempotencyKey);

        var result = useCase.initiatePayment(command);

        assertThat(result.getId()).isEqualTo(existingPayment.getId());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("initiatePayment creates new payment when no duplicate found")
    void createsNewPaymentWhenNoDuplicate() {
        var idempotencyKey = "unique-key";

        when(paymentRepository.findByIdempotencyKey(tenantId, idempotencyKey))
                .thenReturn(Optional.empty());

        var savedPayment = buildPayment(tenantId, loanId, idempotencyKey);
        when(paymentRepository.save(any())).thenReturn(savedPayment);

        var customerId = UUID.randomUUID();
        var command = new ProcessPaymentUseCase.InitiatePaymentCommand(
                tenantId, loanId, UUID.randomUUID(), customerId,
                new BigDecimal("1000"), PaymentMethod.WALLET_MANUAL,
                "INV-001", LocalDate.now(), null, idempotencyKey);

        var result = useCase.initiatePayment(command);

        assertThat(result).isNotNull();
        verify(paymentRepository).save(any());
        verify(eventPublisher).publishAll(any());
    }

    @Test
    @DisplayName("completePayment applies waterfall allocation and publishes events")
    void completePaymentAppliesWaterfall() {
        var paymentId = PaymentId.of(UUID.randomUUID());
        var idempotencyKey = "complete-key";

        var payment = buildPayment(tenantId, loanId, idempotencyKey);
        payment.markProcessing();

        var schedule = buildSchedule(tenantId, loanId);

        when(paymentRepository.findById(tenantId, paymentId)).thenReturn(Optional.of(payment));
        when(scheduleRepository.findActiveByLoanId(tenantId, loanId)).thenReturn(Optional.of(schedule));
        when(paymentRepository.save(any())).thenReturn(payment);
        when(scheduleRepository.save(any())).thenReturn(schedule);

        var command = new ProcessPaymentUseCase.CompletePaymentCommand(
                tenantId, paymentId.getValue(), "PROV-TXN-001");

        var result = useCase.completePayment(command);

        assertThat(result).isNotNull();
        assertThat(result.payment()).isNotNull();
        verify(allocationRepository).saveAll(eq(tenantId), any());
        verify(eventPublisher, atLeastOnce()).publishAll(any());
    }

    @Test
    @DisplayName("failPayment updates payment status to FAILED")
    void failsPaymentWithFailureDetails() {
        var paymentId = PaymentId.of(UUID.randomUUID());
        var payment = buildPayment(tenantId, loanId, "fail-key");
        payment.markProcessing();

        when(paymentRepository.findById(tenantId, paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new ProcessPaymentUseCase.FailPaymentCommand(
                tenantId, paymentId.getValue(), "INSUFFICIENT_FUNDS", "Not enough balance");

        var result = useCase.failPayment(command);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getFailureCode()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    // ==================== HELPERS ====================

    private PaymentAggregate buildPayment(UUID tenantId, UUID loanId, String idempotencyKey) {
        return PaymentAggregate.initiate(
                tenantId, "PAY-TEST-001", loanId, UUID.randomUUID(), UUID.randomUUID(),
                "INV-TEST-001", PaymentMethod.WALLET_MANUAL, new BigDecimal("1070"),
                LocalDate.now(), idempotencyKey);
    }

    private RepaymentScheduleAggregate buildSchedule(UUID tenantId, UUID loanId) {
        UUID scheduleId = UUID.randomUUID();
        var installments = List.of(
                Installment.create(tenantId, scheduleId, loanId, 1,
                        LocalDate.now().plusMonths(1),
                        new BigDecimal("1000"), new BigDecimal("50"), new BigDecimal("20"))
        );
        return RepaymentScheduleAggregate.create(
                tenantId, "SCH-TEST", loanId,
                new BigDecimal("1000"), new BigDecimal("50"),
                LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(1),
                installments, UUID.randomUUID());
    }
}
