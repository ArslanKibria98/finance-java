package com.ksa.financing.wallet.infrastructure.persistence.mapper;

import com.ksa.financing.wallet.domain.model.IbftStatus;
import com.ksa.financing.wallet.domain.model.IbftTransaction;
import com.ksa.financing.wallet.infrastructure.persistence.entity.IbftTransactionJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class IbftTransactionPersistenceMapper {

    public IbftTransactionJpaEntity toEntity(IbftTransaction d) {
        if (d == null) return null;
        var e = new IbftTransactionJpaEntity();
        e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setIbftNumber(d.getIbftNumber());
        e.setCustomerId(d.getCustomerId());
        e.setWalletId(d.getWalletId());
        e.setBeneficiaryId(d.getBeneficiaryId());
        e.setDebtorCorporateAccount(d.getDebtorCorporateAccount());
        e.setCreditorAccount(d.getCreditorAccount());
        e.setCreditorName(d.getCreditorName());
        e.setAmount(d.getAmount());
        e.setFeeAmount(d.getFeeAmount());
        e.setCurrency(d.getCurrency());
        e.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        e.setPurposeNote(d.getPurposeNote());
        e.setEndToEndId(d.getEndToEndId());
        e.setFineractHoldTxnId(d.getFineractHoldTxnId());
        e.setHoldMovementId(d.getHoldMovementId());
        e.setDebitMovementId(d.getDebitMovementId());
        e.setReleaseMovementId(d.getReleaseMovementId());
        e.setLedgerEntryId(d.getLedgerEntryId());
        e.setScotiaSubmissionId(d.getScotiaSubmissionId());
        e.setScotiaPaymentId(d.getScotiaPaymentId());
        e.setScotiaStatus(d.getScotiaStatus());
        e.setIdempotencyKey(d.getIdempotencyKey());
        e.setInitiatorUserId(d.getInitiatorUserId());
        e.setInitiatorIp(d.getInitiatorIp());
        e.setInitiatorDeviceId(d.getInitiatorDeviceId());
        e.setInquiryAttempts(d.getInquiryAttempts());
        e.setLastInquiredAt(toOffset(d.getLastInquiredAt()));
        e.setErrorCode(d.getErrorCode());
        e.setErrorMessage(d.getErrorMessage());
        e.setInitiatedAt(toOffset(d.getInitiatedAt()));
        e.setSubmittedAt(toOffset(d.getSubmittedAt()));
        e.setSettledAt(toOffset(d.getSettledAt()));
        e.setFailedAt(toOffset(d.getFailedAt()));
        e.setCreatedAt(toOffset(d.getCreatedAt()));
        e.setUpdatedAt(toOffset(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    public IbftTransaction toDomain(IbftTransactionJpaEntity e) {
        if (e == null) return null;
        var d = new IbftTransaction();
        d.setId(e.getId());
        d.setTenantId(e.getTenantId());
        d.setIbftNumber(e.getIbftNumber());
        d.setCustomerId(e.getCustomerId());
        d.setWalletId(e.getWalletId());
        d.setBeneficiaryId(e.getBeneficiaryId());
        d.setDebtorCorporateAccount(e.getDebtorCorporateAccount());
        d.setCreditorAccount(e.getCreditorAccount());
        d.setCreditorName(e.getCreditorName());
        d.setAmount(e.getAmount());
        d.setFeeAmount(e.getFeeAmount());
        d.setCurrency(e.getCurrency());
        d.setStatus(e.getStatus() != null ? IbftStatus.valueOf(e.getStatus()) : null);
        d.setPurposeNote(e.getPurposeNote());
        d.setEndToEndId(e.getEndToEndId());
        d.setFineractHoldTxnId(e.getFineractHoldTxnId());
        d.setHoldMovementId(e.getHoldMovementId());
        d.setDebitMovementId(e.getDebitMovementId());
        d.setReleaseMovementId(e.getReleaseMovementId());
        d.setLedgerEntryId(e.getLedgerEntryId());
        d.setScotiaSubmissionId(e.getScotiaSubmissionId());
        d.setScotiaPaymentId(e.getScotiaPaymentId());
        d.setScotiaStatus(e.getScotiaStatus());
        d.setIdempotencyKey(e.getIdempotencyKey());
        d.setInitiatorUserId(e.getInitiatorUserId());
        d.setInitiatorIp(e.getInitiatorIp());
        d.setInitiatorDeviceId(e.getInitiatorDeviceId());
        d.setInquiryAttempts(e.getInquiryAttempts());
        d.setLastInquiredAt(toInstant(e.getLastInquiredAt()));
        d.setErrorCode(e.getErrorCode());
        d.setErrorMessage(e.getErrorMessage());
        d.setInitiatedAt(toInstant(e.getInitiatedAt()));
        d.setSubmittedAt(toInstant(e.getSubmittedAt()));
        d.setSettledAt(toInstant(e.getSettledAt()));
        d.setFailedAt(toInstant(e.getFailedAt()));
        d.setCreatedAt(toInstant(e.getCreatedAt()));
        d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }

    private OffsetDateTime toOffset(Instant i) { return i != null ? i.atOffset(ZoneOffset.UTC) : null; }
    private Instant toInstant(OffsetDateTime o) { return o != null ? o.toInstant() : null; }
}
