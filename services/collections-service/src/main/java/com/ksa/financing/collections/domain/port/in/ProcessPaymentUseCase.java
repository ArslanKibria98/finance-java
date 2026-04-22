package com.ksa.financing.collections.domain.port.in;

import com.ksa.financing.collections.domain.model.PaymentAggregate;
import com.ksa.financing.collections.domain.model.PaymentMethod;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface ProcessPaymentUseCase {

    record InitiatePaymentCommand(
            UUID tenantId,
            UUID loanId,
            UUID installmentId,
            UUID customerId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String invoiceId,
            LocalDate valueDate,
            UUID sourceWalletId,
            String idempotencyKey
    ) {}

    record CompletePaymentCommand(
            UUID tenantId,
            UUID paymentId,
            String providerTransactionId
    ) {}

    record FailPaymentCommand(
            UUID tenantId,
            UUID paymentId,
            String failureCode,
            String failureMessage
    ) {}

    record ReversePaymentCommand(
            UUID tenantId,
            UUID paymentId,
            String reason
    ) {}

    record PaymentResult(
            PaymentAggregate payment,
            RepaymentScheduleAggregate updatedSchedule
    ) {}

    PaymentAggregate initiatePayment(InitiatePaymentCommand command);

    PaymentResult completePayment(CompletePaymentCommand command);

    PaymentAggregate failPayment(FailPaymentCommand command);

    PaymentAggregate reversePayment(ReversePaymentCommand command);

    PaymentAggregate getPayment(UUID tenantId, UUID paymentId);

    PaymentAggregate getPaymentByIdempotencyKey(UUID tenantId, String idempotencyKey);
}
