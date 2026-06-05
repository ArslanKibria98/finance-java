package com.ksa.financing.collections.domain.port.out;

import com.ksa.financing.collections.domain.model.PaymentAggregate;
import com.ksa.financing.collections.domain.model.PaymentId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    PaymentAggregate save(PaymentAggregate payment);

    Optional<PaymentAggregate> findById(UUID tenantId, PaymentId id);

    Optional<PaymentAggregate> findByIdempotencyKey(UUID tenantId, String idempotencyKey);

    Optional<PaymentAggregate> findByInvoiceId(UUID tenantId, String invoiceId);

    List<PaymentAggregate> findByLoanId(UUID tenantId, UUID loanId);
}
