package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.domain.model.PaymentAggregate;
import com.ksa.financing.collections.domain.model.PaymentId;
import com.ksa.financing.collections.domain.port.out.PaymentRepository;
import com.ksa.financing.collections.infrastructure.persistence.mapper.PaymentPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final JpaPaymentRepository jpaRepository;
    private final PaymentPersistenceMapper mapper;

    @Override
    public PaymentAggregate save(PaymentAggregate payment) {
        var entity = mapper.toJpa(payment);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PaymentAggregate> findById(UUID tenantId, PaymentId id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentAggregate> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return jpaRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentAggregate> findByInvoiceId(UUID tenantId, String invoiceId) {
        return jpaRepository.findByTenantIdAndInvoiceId(tenantId, invoiceId)
                .map(mapper::toDomain);
    }

    @Override
    public List<PaymentAggregate> findByLoanId(UUID tenantId, UUID loanId) {
        return jpaRepository.findByTenantIdAndLoanId(tenantId, loanId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
