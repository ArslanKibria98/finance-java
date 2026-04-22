package com.ksa.financing.collections.infrastructure.persistence.mapper;

import com.ksa.financing.collections.domain.model.PaymentAggregate;
import com.ksa.financing.collections.domain.model.PaymentId;
import com.ksa.financing.collections.domain.model.PaymentMethod;
import com.ksa.financing.collections.domain.model.PaymentStatus;
import com.ksa.financing.collections.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentPersistenceMapper {

    public PaymentJpaEntity toJpa(PaymentAggregate aggregate) {
        var entity = new PaymentJpaEntity();
        entity.setId(aggregate.getId().getValue());
        entity.setTenantId(aggregate.getTenantId());
        entity.setPaymentNumber(aggregate.getPaymentNumber());
        entity.setLoanId(aggregate.getLoanId());
        entity.setInstallmentId(aggregate.getInstallmentId());
        entity.setCustomerId(aggregate.getCustomerId());
        entity.setInvoiceId(aggregate.getInvoiceId());
        entity.setPaymentMethod(aggregate.getPaymentMethod().name());
        entity.setAmount(aggregate.getAmount());
        entity.setCurrency(aggregate.getCurrency());
        entity.setStatus(aggregate.getStatus().name());
        entity.setValueDate(aggregate.getValueDate());
        entity.setSourceWalletId(aggregate.getSourceWalletId());
        entity.setSourceReference(aggregate.getSourceReference());
        entity.setProviderTransactionId(aggregate.getProviderTransactionId());
        entity.setIdempotencyKey(aggregate.getIdempotencyKey());
        entity.setLedgerSynced(aggregate.isLedgerSynced());
        entity.setLedgerEntryId(aggregate.getLedgerEntryId());
        entity.setFailureCode(aggregate.getFailureCode());
        entity.setFailureMessage(aggregate.getFailureMessage());
        entity.setCreatedAt(aggregate.getCreatedAt());
        entity.setUpdatedAt(aggregate.getUpdatedAt() != null ? aggregate.getUpdatedAt() : LocalDateTime.now());
        return entity;
    }

    public PaymentAggregate toDomain(PaymentJpaEntity entity) {
        return PaymentAggregate.reconstitute(
                PaymentId.of(entity.getId()),
                entity.getTenantId(),
                entity.getPaymentNumber(),
                entity.getLoanId(),
                entity.getInstallmentId(),
                entity.getCustomerId(),
                entity.getInvoiceId(),
                PaymentMethod.valueOf(entity.getPaymentMethod()),
                entity.getAmount(),
                entity.getCurrency(),
                PaymentStatus.valueOf(entity.getStatus()),
                entity.getValueDate(),
                entity.getSourceWalletId(),
                entity.getSourceReference(),
                entity.getProviderTransactionId(),
                entity.getIdempotencyKey(),
                entity.isLedgerSynced(),
                entity.getLedgerEntryId(),
                entity.getFailureCode(),
                entity.getFailureMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }
}
