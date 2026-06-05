package com.ksa.financing.collections.domain.port.out;

import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate.PaymentAllocation;

import java.util.List;
import java.util.UUID;

public interface PaymentAllocationRepository {

    void saveAll(UUID tenantId, List<PaymentAllocation> allocations);

    List<PaymentAllocation> findByPaymentId(UUID tenantId, UUID paymentId);
}
