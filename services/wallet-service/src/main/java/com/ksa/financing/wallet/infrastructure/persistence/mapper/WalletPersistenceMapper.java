package com.ksa.financing.wallet.infrastructure.persistence.mapper;

import com.ksa.financing.wallet.domain.model.*;
import com.ksa.financing.wallet.infrastructure.persistence.entity.TopUpTransactionJpaEntity;
import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletJpaEntity;
import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletMovementJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class WalletPersistenceMapper {

    // ========== Wallet Mapping ==========

    public WalletJpaEntity toEntity(Wallet domain) {
        if (domain == null) return null;

        WalletJpaEntity entity = new WalletJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setWalletNumber(domain.getWalletNumber());
        entity.setCustomerId(domain.getCustomerId());
        entity.setAccountNumber(domain.getAccountNumber());
        entity.setAvailableBalance(domain.getAvailableBalance());
        entity.setReservedBalance(domain.getReservedBalance());
        entity.setCurrency(domain.getCurrency());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        entity.setDailyTopUpLimit(domain.getDailyTopUpLimit());
        entity.setMonthlyTopUpLimit(domain.getMonthlyTopUpLimit());
        entity.setSingleTopUpLimit(domain.getSingleTopUpLimit());
        entity.setTodayTopUpAmount(domain.getTodayTopUpAmount());
        entity.setMonthTopUpAmount(domain.getMonthTopUpAmount());
        entity.setSingleTransactionLimit(domain.getSingleTransactionLimit());
        entity.setDailyTransactionLimit(domain.getDailyTransactionLimit());
        entity.setMonthlyTransactionLimit(domain.getMonthlyTransactionLimit());
        entity.setYearlyTransactionLimit(domain.getYearlyTransactionLimit());
        entity.setFineractSavingsAccountId(domain.getFineractSavingsAccountId());
        entity.setLedgerSynced(domain.isLedgerSynced());
        entity.setLastLedgerSyncAt(toOffsetDateTime(domain.getLastLedgerSyncAt()));
        entity.setIban(domain.getIban());
        entity.setMaskedName(domain.getMaskedName());
        entity.setEnglishFirstName(domain.getEnglishFirstName());
        entity.setEnglishThirdName(domain.getEnglishThirdName());
        entity.setAutoDebitEnabled(domain.isAutoDebitEnabled());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public Wallet toDomain(WalletJpaEntity entity) {
        if (entity == null) return null;

        Wallet domain = new Wallet();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setWalletNumber(entity.getWalletNumber());
        domain.setCustomerId(entity.getCustomerId());
        domain.setAccountNumber(entity.getAccountNumber());
        domain.setAvailableBalance(entity.getAvailableBalance());
        domain.setReservedBalance(entity.getReservedBalance());
        domain.setTotalBalance(entity.getTotalBalance());
        domain.setCurrency(entity.getCurrency());
        domain.setStatus(entity.getStatus() != null ? WalletStatus.valueOf(entity.getStatus()) : null);
        domain.setDailyTopUpLimit(entity.getDailyTopUpLimit());
        domain.setMonthlyTopUpLimit(entity.getMonthlyTopUpLimit());
        domain.setSingleTopUpLimit(entity.getSingleTopUpLimit());
        domain.setTodayTopUpAmount(entity.getTodayTopUpAmount());
        domain.setMonthTopUpAmount(entity.getMonthTopUpAmount());
        domain.setSingleTransactionLimit(entity.getSingleTransactionLimit());
        domain.setDailyTransactionLimit(entity.getDailyTransactionLimit());
        domain.setMonthlyTransactionLimit(entity.getMonthlyTransactionLimit());
        domain.setYearlyTransactionLimit(entity.getYearlyTransactionLimit());
        domain.setFineractSavingsAccountId(entity.getFineractSavingsAccountId());
        domain.setLedgerSynced(entity.isLedgerSynced());
        domain.setLastLedgerSyncAt(toInstant(entity.getLastLedgerSyncAt()));
        domain.setIban(entity.getIban());
        domain.setMaskedName(entity.getMaskedName());
        domain.setEnglishFirstName(entity.getEnglishFirstName());
        domain.setEnglishThirdName(entity.getEnglishThirdName());
        domain.setAutoDebitEnabled(entity.isAutoDebitEnabled());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // ========== WalletMovement Mapping ==========

    public WalletMovementJpaEntity toEntity(WalletMovement domain) {
        if (domain == null) return null;

        WalletMovementJpaEntity entity = new WalletMovementJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setWalletId(domain.getWalletId());
        entity.setMovementNumber(domain.getMovementNumber());
        entity.setMovementType(domain.getMovementType() != null ? domain.getMovementType().name() : null);
        entity.setPurpose(domain.getPurpose() != null ? domain.getPurpose().name() : null);
        entity.setAmount(domain.getAmount());
        entity.setBalanceBefore(domain.getBalanceBefore());
        entity.setBalanceAfter(domain.getBalanceAfter());
        entity.setReferenceType(domain.getReferenceType());
        entity.setReferenceId(domain.getReferenceId());
        entity.setDescription(domain.getDescription());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        return entity;
    }

    public WalletMovement toDomain(WalletMovementJpaEntity entity) {
        if (entity == null) return null;

        WalletMovement domain = new WalletMovement();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setWalletId(entity.getWalletId());
        domain.setMovementNumber(entity.getMovementNumber());
        domain.setMovementType(entity.getMovementType() != null ? MovementType.valueOf(entity.getMovementType()) : null);
        domain.setPurpose(entity.getPurpose() != null ? TransactionPurpose.valueOf(entity.getPurpose()) : null);
        domain.setAmount(entity.getAmount());
        domain.setBalanceBefore(entity.getBalanceBefore());
        domain.setBalanceAfter(entity.getBalanceAfter());
        domain.setReferenceType(entity.getReferenceType());
        domain.setReferenceId(entity.getReferenceId());
        domain.setDescription(entity.getDescription());
        domain.setIdempotencyKey(entity.getIdempotencyKey());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        return domain;
    }

    // ========== TopUpTransaction Mapping ==========

    public TopUpTransactionJpaEntity toEntity(TopUpTransaction domain) {
        if (domain == null) return null;

        TopUpTransactionJpaEntity entity = new TopUpTransactionJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setWalletId(domain.getWalletId());
        entity.setTransactionNumber(domain.getTransactionNumber());
        entity.setMethod(domain.getMethod() != null ? domain.getMethod().name() : null);
        entity.setAmount(domain.getAmount());
        entity.setFeeAmount(domain.getFeeAmount());
        entity.setNetAmount(domain.getNetAmount());
        entity.setSourceCardLastFour(domain.getSourceCardLastFour());
        entity.setSourceCardBrand(domain.getSourceCardBrand());
        entity.setSourceIban(domain.getSourceIban());
        entity.setProviderTransactionId(domain.getProviderTransactionId());
        entity.setProviderReference(domain.getProviderReference());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        entity.setMovementId(domain.getMovementId());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setErrorCode(domain.getErrorCode());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setInitiatedAt(toOffsetDateTime(domain.getInitiatedAt()));
        entity.setCompletedAt(toOffsetDateTime(domain.getCompletedAt()));
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public TopUpTransaction toDomain(TopUpTransactionJpaEntity entity) {
        if (entity == null) return null;

        TopUpTransaction domain = new TopUpTransaction();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setWalletId(entity.getWalletId());
        domain.setTransactionNumber(entity.getTransactionNumber());
        domain.setMethod(entity.getMethod() != null ? TopUpMethod.valueOf(entity.getMethod()) : null);
        domain.setAmount(entity.getAmount());
        domain.setFeeAmount(entity.getFeeAmount());
        domain.setNetAmount(entity.getNetAmount());
        domain.setSourceCardLastFour(entity.getSourceCardLastFour());
        domain.setSourceCardBrand(entity.getSourceCardBrand());
        domain.setSourceIban(entity.getSourceIban());
        domain.setProviderTransactionId(entity.getProviderTransactionId());
        domain.setProviderReference(entity.getProviderReference());
        domain.setStatus(entity.getStatus() != null ? TopUpStatus.valueOf(entity.getStatus()) : null);
        domain.setMovementId(entity.getMovementId());
        domain.setIdempotencyKey(entity.getIdempotencyKey());
        domain.setErrorCode(entity.getErrorCode());
        domain.setErrorMessage(entity.getErrorMessage());
        domain.setInitiatedAt(toInstant(entity.getInitiatedAt()));
        domain.setCompletedAt(toInstant(entity.getCompletedAt()));
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // ========== Timestamp Conversion Helpers ==========

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

    private Instant toInstant(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toInstant() : null;
    }
}
