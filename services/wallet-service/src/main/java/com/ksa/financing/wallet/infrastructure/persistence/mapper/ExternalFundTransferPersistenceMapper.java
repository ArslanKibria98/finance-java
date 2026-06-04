package com.ksa.financing.wallet.infrastructure.persistence.mapper;

import com.ksa.financing.wallet.domain.model.ExternalFundTransfer;
import com.ksa.financing.wallet.domain.model.ExternalTransferDirection;
import com.ksa.financing.wallet.domain.model.ExternalTransferStatus;
import com.ksa.financing.wallet.infrastructure.persistence.entity.ExternalFundTransferJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class ExternalFundTransferPersistenceMapper {

    public ExternalFundTransferJpaEntity toEntity(ExternalFundTransfer d) {
        if (d == null) return null;
        var e = new ExternalFundTransferJpaEntity();
        e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setTransferNumber(d.getTransferNumber());
        e.setDirection(d.getDirection() != null ? d.getDirection().name() : null);
        e.setWalletId(d.getWalletId());
        e.setCustomerId(d.getCustomerId());
        e.setAccountNumber(d.getAccountNumber());
        e.setCounterpartyName(d.getCounterpartyName());
        e.setCounterpartyAccount(d.getCounterpartyAccount());
        e.setCounterpartyEmail(d.getCounterpartyEmail());
        e.setCounterpartyBankCode(d.getCounterpartyBankCode());
        e.setAmount(d.getAmount());
        e.setFeeAmount(d.getFeeAmount());
        e.setCurrency(d.getCurrency());
        e.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        e.setPurposeNote(d.getPurposeNote());
        e.setMovementId(d.getMovementId());
        e.setCounterpartyWalletId(d.getCounterpartyWalletId());
        e.setCounterpartyMovementId(d.getCounterpartyMovementId());
        e.setCounterpartyInternal(d.isCounterpartyInternal());
        e.setLedgerEntryId(d.getLedgerEntryId());
        e.setScotiaPaymentId(d.getScotiaPaymentId());
        e.setScotiaClearingRef(d.getScotiaClearingRef());
        e.setScotiaStatus(d.getScotiaStatus());
        e.setIdempotencyKey(d.getIdempotencyKey());
        e.setInitiatorUserId(d.getInitiatorUserId());
        e.setInitiatorIp(d.getInitiatorIp());
        e.setInitiatorDeviceId(d.getInitiatorDeviceId());
        e.setErrorCode(d.getErrorCode());
        e.setErrorMessage(d.getErrorMessage());
        e.setInitiatedAt(toOffset(d.getInitiatedAt()));
        e.setCompletedAt(toOffset(d.getCompletedAt()));
        e.setCreatedAt(toOffset(d.getCreatedAt()));
        e.setUpdatedAt(toOffset(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    public ExternalFundTransfer toDomain(ExternalFundTransferJpaEntity e) {
        if (e == null) return null;
        var d = new ExternalFundTransfer();
        d.setId(e.getId());
        d.setTenantId(e.getTenantId());
        d.setTransferNumber(e.getTransferNumber());
        d.setDirection(e.getDirection() != null ? ExternalTransferDirection.valueOf(e.getDirection()) : null);
        d.setWalletId(e.getWalletId());
        d.setCustomerId(e.getCustomerId());
        d.setAccountNumber(e.getAccountNumber());
        d.setCounterpartyName(e.getCounterpartyName());
        d.setCounterpartyAccount(e.getCounterpartyAccount());
        d.setCounterpartyEmail(e.getCounterpartyEmail());
        d.setCounterpartyBankCode(e.getCounterpartyBankCode());
        d.setAmount(e.getAmount());
        d.setFeeAmount(e.getFeeAmount());
        d.setCurrency(e.getCurrency());
        d.setStatus(e.getStatus() != null ? ExternalTransferStatus.valueOf(e.getStatus()) : null);
        d.setPurposeNote(e.getPurposeNote());
        d.setMovementId(e.getMovementId());
        d.setCounterpartyWalletId(e.getCounterpartyWalletId());
        d.setCounterpartyMovementId(e.getCounterpartyMovementId());
        d.setCounterpartyInternal(e.isCounterpartyInternal());
        d.setLedgerEntryId(e.getLedgerEntryId());
        d.setScotiaPaymentId(e.getScotiaPaymentId());
        d.setScotiaClearingRef(e.getScotiaClearingRef());
        d.setScotiaStatus(e.getScotiaStatus());
        d.setIdempotencyKey(e.getIdempotencyKey());
        d.setInitiatorUserId(e.getInitiatorUserId());
        d.setInitiatorIp(e.getInitiatorIp());
        d.setInitiatorDeviceId(e.getInitiatorDeviceId());
        d.setErrorCode(e.getErrorCode());
        d.setErrorMessage(e.getErrorMessage());
        d.setInitiatedAt(toInstant(e.getInitiatedAt()));
        d.setCompletedAt(toInstant(e.getCompletedAt()));
        d.setCreatedAt(toInstant(e.getCreatedAt()));
        d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }

    private OffsetDateTime toOffset(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

    private Instant toInstant(OffsetDateTime odt) {
        return odt != null ? odt.toInstant() : null;
    }
}
