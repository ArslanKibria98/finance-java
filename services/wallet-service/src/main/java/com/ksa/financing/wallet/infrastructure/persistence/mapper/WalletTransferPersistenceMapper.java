package com.ksa.financing.wallet.infrastructure.persistence.mapper;

import com.ksa.financing.wallet.domain.model.TransferChannel;
import com.ksa.financing.wallet.domain.model.TransferStatus;
import com.ksa.financing.wallet.domain.model.WalletTransfer;
import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletTransferJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class WalletTransferPersistenceMapper {

    public WalletTransferJpaEntity toEntity(WalletTransfer domain) {
        if (domain == null) return null;

        WalletTransferJpaEntity entity = new WalletTransferJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setTransferNumber(domain.getTransferNumber());
        entity.setSourceWalletId(domain.getSourceWalletId());
        entity.setDestinationWalletId(domain.getDestinationWalletId());
        entity.setSourceCustomerId(domain.getSourceCustomerId());
        entity.setDestinationCustomerId(domain.getDestinationCustomerId());
        entity.setChannel(domain.getChannel() != null ? domain.getChannel().name() : null);
        entity.setAmount(domain.getAmount());
        entity.setFeeAmount(domain.getFeeAmount());
        entity.setTotalDebit(domain.getTotalDebit());
        entity.setCurrency(domain.getCurrency());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        entity.setPurposeNote(domain.getPurposeNote());
        entity.setDebitMovementId(domain.getDebitMovementId());
        entity.setCreditMovementId(domain.getCreditMovementId());
        entity.setFeeMovementId(domain.getFeeMovementId());
        entity.setLedgerEntryId(domain.getLedgerEntryId());
        entity.setFineractTransferId(domain.getFineractTransferId());
        entity.setWorkflowId(domain.getWorkflowId());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setInitiatorUserId(domain.getInitiatorUserId());
        entity.setInitiatorIp(domain.getInitiatorIp());
        entity.setInitiatorDeviceId(domain.getInitiatorDeviceId());
        entity.setErrorCode(domain.getErrorCode());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setInitiatedAt(toOffset(domain.getInitiatedAt()));
        entity.setCompletedAt(toOffset(domain.getCompletedAt()));
        entity.setReversedAt(toOffset(domain.getReversedAt()));
        entity.setReversalOfTransferId(domain.getReversalOfTransferId());
        entity.setCreatedAt(toOffset(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffset(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public WalletTransfer toDomain(WalletTransferJpaEntity entity) {
        if (entity == null) return null;

        WalletTransfer domain = new WalletTransfer();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setTransferNumber(entity.getTransferNumber());
        domain.setSourceWalletId(entity.getSourceWalletId());
        domain.setDestinationWalletId(entity.getDestinationWalletId());
        domain.setSourceCustomerId(entity.getSourceCustomerId());
        domain.setDestinationCustomerId(entity.getDestinationCustomerId());
        domain.setChannel(entity.getChannel() != null ? TransferChannel.valueOf(entity.getChannel()) : null);
        domain.setAmount(entity.getAmount());
        domain.setFeeAmount(entity.getFeeAmount());
        domain.setTotalDebit(entity.getTotalDebit());
        domain.setCurrency(entity.getCurrency());
        domain.setStatus(entity.getStatus() != null ? TransferStatus.valueOf(entity.getStatus()) : null);
        domain.setPurposeNote(entity.getPurposeNote());
        domain.setDebitMovementId(entity.getDebitMovementId());
        domain.setCreditMovementId(entity.getCreditMovementId());
        domain.setFeeMovementId(entity.getFeeMovementId());
        domain.setLedgerEntryId(entity.getLedgerEntryId());
        domain.setFineractTransferId(entity.getFineractTransferId());
        domain.setWorkflowId(entity.getWorkflowId());
        domain.setIdempotencyKey(entity.getIdempotencyKey());
        domain.setInitiatorUserId(entity.getInitiatorUserId());
        domain.setInitiatorIp(entity.getInitiatorIp());
        domain.setInitiatorDeviceId(entity.getInitiatorDeviceId());
        domain.setErrorCode(entity.getErrorCode());
        domain.setErrorMessage(entity.getErrorMessage());
        domain.setInitiatedAt(toInstant(entity.getInitiatedAt()));
        domain.setCompletedAt(toInstant(entity.getCompletedAt()));
        domain.setReversedAt(toInstant(entity.getReversedAt()));
        domain.setReversalOfTransferId(entity.getReversalOfTransferId());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    private OffsetDateTime toOffset(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

    private Instant toInstant(OffsetDateTime odt) {
        return odt != null ? odt.toInstant() : null;
    }
}
