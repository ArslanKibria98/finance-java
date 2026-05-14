package com.ksa.financing.wallet.infrastructure.persistence.mapper;

import com.ksa.financing.wallet.domain.iso.ChargeBearerType1Code;
import com.ksa.financing.wallet.domain.iso.ExternalPurpose1Code;
import com.ksa.financing.wallet.domain.iso.ServiceLevel;
import com.ksa.financing.wallet.domain.model.WalletWithdrawal;
import com.ksa.financing.wallet.domain.model.WithdrawalChannel;
import com.ksa.financing.wallet.domain.model.WithdrawalStatus;
import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletWithdrawalJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class WalletWithdrawalPersistenceMapper {

    public WalletWithdrawalJpaEntity toEntity(WalletWithdrawal d) {
        if (d == null) return null;
        WalletWithdrawalJpaEntity e = new WalletWithdrawalJpaEntity();
        e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setWithdrawalNumber(d.getWithdrawalNumber());
        e.setSourceWalletId(d.getSourceWalletId());
        e.setSourceCustomerId(d.getSourceCustomerId());
        e.setChannel(d.getChannel() != null ? d.getChannel().name() : null);
        e.setDestinationIban(d.getDestinationIban());
        e.setDestinationBankCode(d.getDestinationBankCode());
        e.setDestinationBankName(d.getDestinationBankName());
        e.setBeneficiaryName(d.getBeneficiaryName());
        e.setBeneficiaryId(d.getBeneficiaryId());
        e.setAmount(d.getAmount());
        e.setFeeAmount(d.getFeeAmount());
        e.setTotalDebit(d.getTotalDebit());
        e.setCurrency(d.getCurrency());
        e.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        e.setPurposeNote(d.getPurposeNote());
        e.setPurposeCode(d.getPurposeCode() != null ? d.getPurposeCode().name() : null);
        e.setChargeBearer(d.getChargeBearer() != null ? d.getChargeBearer().name() : "DEBT");
        e.setServiceLevel(d.getServiceLevel() != null ? d.getServiceLevel().name() : "NURG");
        e.setEndToEndId(d.getEndToEndId());
        e.setUetr(d.getUetr());
        e.setInstructionId(d.getInstructionId());
        e.setDestinationCountry(d.getDestinationCountry());
        e.setScreeningRef(d.getScreeningRef());
        e.setScreeningDecision(d.getScreeningDecision());
        e.setScreeningScore(d.getScreeningScore());
        e.setScreeningMatches(d.getScreeningMatchesJson());
        e.setScreenedAt(toOffset(d.getScreenedAt()));
        e.setEddRequired(d.isEddRequired());
        e.setReleasedBy(d.getReleasedBy());
        e.setReleasedAt(toOffset(d.getReleasedAt()));
        e.setReleaseReason(d.getReleaseReason());
        e.setDebitMovementId(d.getDebitMovementId());
        e.setFeeMovementId(d.getFeeMovementId());
        e.setRefundMovementId(d.getRefundMovementId());
        e.setFineractDebitTxnId(d.getFineractDebitTxnId());
        e.setFineractRefundTxnId(d.getFineractRefundTxnId());
        e.setBankReference(d.getBankReference());
        e.setSarieReference(d.getSarieReference());
        e.setWorkflowId(d.getWorkflowId());
        e.setIdempotencyKey(d.getIdempotencyKey());
        e.setInitiatorUserId(d.getInitiatorUserId());
        e.setInitiatorIp(d.getInitiatorIp());
        e.setInitiatorDeviceId(d.getInitiatorDeviceId());
        e.setErrorCode(d.getErrorCode());
        e.setErrorMessage(d.getErrorMessage());
        e.setInitiatedAt(toOffset(d.getInitiatedAt()));
        e.setDebitedAt(toOffset(d.getDebitedAt()));
        e.setBankSubmittedAt(toOffset(d.getBankSubmittedAt()));
        e.setCompletedAt(toOffset(d.getCompletedAt()));
        e.setFailedAt(toOffset(d.getFailedAt()));
        e.setCompensatedAt(toOffset(d.getCompensatedAt()));
        e.setCreatedAt(toOffset(d.getCreatedAt()));
        e.setUpdatedAt(toOffset(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    public WalletWithdrawal toDomain(WalletWithdrawalJpaEntity e) {
        if (e == null) return null;
        WalletWithdrawal d = new WalletWithdrawal();
        d.setId(e.getId());
        d.setTenantId(e.getTenantId());
        d.setWithdrawalNumber(e.getWithdrawalNumber());
        d.setSourceWalletId(e.getSourceWalletId());
        d.setSourceCustomerId(e.getSourceCustomerId());
        d.setChannel(e.getChannel() != null ? WithdrawalChannel.valueOf(e.getChannel()) : null);
        d.setDestinationIban(e.getDestinationIban());
        d.setDestinationBankCode(e.getDestinationBankCode());
        d.setDestinationBankName(e.getDestinationBankName());
        d.setBeneficiaryName(e.getBeneficiaryName());
        d.setBeneficiaryId(e.getBeneficiaryId());
        d.setAmount(e.getAmount());
        d.setFeeAmount(e.getFeeAmount());
        d.setTotalDebit(e.getTotalDebit());
        d.setCurrency(e.getCurrency());
        d.setStatus(e.getStatus() != null ? WithdrawalStatus.valueOf(e.getStatus()) : null);
        d.setPurposeNote(e.getPurposeNote());
        d.setPurposeCode(ExternalPurpose1Code.parse(e.getPurposeCode()));
        d.setChargeBearer(ChargeBearerType1Code.parseOrDefault(e.getChargeBearer()));
        d.setServiceLevel(ServiceLevel.parseOrDefault(e.getServiceLevel()));
        d.setEndToEndId(e.getEndToEndId());
        d.setUetr(e.getUetr());
        d.setInstructionId(e.getInstructionId());
        d.setDestinationCountry(e.getDestinationCountry());
        d.setScreeningRef(e.getScreeningRef());
        d.setScreeningDecision(e.getScreeningDecision());
        d.setScreeningScore(e.getScreeningScore());
        d.setScreeningMatchesJson(e.getScreeningMatches());
        d.setScreenedAt(toInstant(e.getScreenedAt()));
        d.setEddRequired(e.isEddRequired());
        d.setReleasedBy(e.getReleasedBy());
        d.setReleasedAt(toInstant(e.getReleasedAt()));
        d.setReleaseReason(e.getReleaseReason());
        d.setDebitMovementId(e.getDebitMovementId());
        d.setFeeMovementId(e.getFeeMovementId());
        d.setRefundMovementId(e.getRefundMovementId());
        d.setFineractDebitTxnId(e.getFineractDebitTxnId());
        d.setFineractRefundTxnId(e.getFineractRefundTxnId());
        d.setBankReference(e.getBankReference());
        d.setSarieReference(e.getSarieReference());
        d.setWorkflowId(e.getWorkflowId());
        d.setIdempotencyKey(e.getIdempotencyKey());
        d.setInitiatorUserId(e.getInitiatorUserId());
        d.setInitiatorIp(e.getInitiatorIp());
        d.setInitiatorDeviceId(e.getInitiatorDeviceId());
        d.setErrorCode(e.getErrorCode());
        d.setErrorMessage(e.getErrorMessage());
        d.setInitiatedAt(toInstant(e.getInitiatedAt()));
        d.setDebitedAt(toInstant(e.getDebitedAt()));
        d.setBankSubmittedAt(toInstant(e.getBankSubmittedAt()));
        d.setCompletedAt(toInstant(e.getCompletedAt()));
        d.setFailedAt(toInstant(e.getFailedAt()));
        d.setCompensatedAt(toInstant(e.getCompensatedAt()));
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
