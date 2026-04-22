package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate.PaymentAllocation;
import com.ksa.financing.collections.domain.port.out.PaymentAllocationRepository;
import com.ksa.financing.collections.infrastructure.persistence.entity.PaymentAllocationJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentAllocationRepositoryImpl implements PaymentAllocationRepository {

    private final JpaPaymentAllocationRepository jpaRepository;

    @Override
    public void saveAll(UUID tenantId, List<PaymentAllocation> allocations) {
        var entities = allocations.stream()
                .map(alloc -> {
                    var entity = new PaymentAllocationJpaEntity();
                    entity.setTenantId(tenantId);
                    entity.setPaymentId(alloc.paymentId());
                    entity.setInstallmentId(alloc.installmentId());
                    entity.setAllocationOrder(alloc.allocationOrder());
                    entity.setPrincipalAllocated(alloc.principalAllocated());
                    entity.setProfitAllocated(alloc.profitAllocated());
                    entity.setFeeAllocated(alloc.feeAllocated());
                    entity.setTotalAllocated(alloc.totalAllocated());
                    entity.setCreatedAt(LocalDateTime.now());
                    return entity;
                })
                .toList();
        jpaRepository.saveAll(entities);
    }

    @Override
    public List<PaymentAllocation> findByPaymentId(UUID tenantId, UUID paymentId) {
        return jpaRepository.findByTenantIdAndPaymentId(tenantId, paymentId).stream()
                .map(e -> new PaymentAllocation(
                        e.getPaymentId(),
                        e.getInstallmentId(),
                        e.getAllocationOrder(),
                        e.getPrincipalAllocated(),
                        e.getProfitAllocated(),
                        e.getFeeAllocated(),
                        e.getTotalAllocated()))
                .toList();
    }
}
