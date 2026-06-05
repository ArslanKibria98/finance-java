package com.ksa.financing.wallet.infrastructure.persistence.mapper;

import com.ksa.financing.wallet.domain.model.LimitRequestStatus;
import com.ksa.financing.wallet.domain.model.WalletLimitBounds;
import com.ksa.financing.wallet.domain.model.WalletLimitChangeRequest;
import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletLimitBoundsJpaEntity;
import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletLimitChangeRequestJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class WalletLimitPersistenceMapper {

    // ========== WalletLimitBounds ==========

    public WalletLimitBoundsJpaEntity toEntity(WalletLimitBounds domain) {
        if (domain == null) return null;
        WalletLimitBoundsJpaEntity e = new WalletLimitBoundsJpaEntity();
        e.setId(domain.getId());
        e.setTenantId(domain.getTenantId());
        e.setMinSingleLimit(domain.getMinSingleLimit());
        e.setMaxSingleLimit(domain.getMaxSingleLimit());
        e.setDefaultSingleLimit(domain.getDefaultSingleLimit());
        e.setMinDailyLimit(domain.getMinDailyLimit());
        e.setMaxDailyLimit(domain.getMaxDailyLimit());
        e.setMinMonthlyLimit(domain.getMinMonthlyLimit());
        e.setMaxMonthlyLimit(domain.getMaxMonthlyLimit());
        e.setMinYearlyLimit(domain.getMinYearlyLimit());
        e.setMaxYearlyLimit(domain.getMaxYearlyLimit());
        e.setDefaultDailyLimit(domain.getDefaultDailyLimit());
        e.setDefaultMonthlyLimit(domain.getDefaultMonthlyLimit());
        e.setDefaultYearlyLimit(domain.getDefaultYearlyLimit());
        e.setCreatedAt(toOdt(domain.getCreatedAt()));
        e.setUpdatedAt(toOdt(domain.getUpdatedAt()));
        e.setUpdatedBy(domain.getUpdatedBy());
        e.setVersion(domain.getVersion());
        return e;
    }

    public WalletLimitBounds toDomain(WalletLimitBoundsJpaEntity e) {
        if (e == null) return null;
        WalletLimitBounds d = new WalletLimitBounds();
        d.setId(e.getId());
        d.setTenantId(e.getTenantId());
        d.setMinSingleLimit(e.getMinSingleLimit());
        d.setMaxSingleLimit(e.getMaxSingleLimit());
        d.setDefaultSingleLimit(e.getDefaultSingleLimit());
        d.setMinDailyLimit(e.getMinDailyLimit());
        d.setMaxDailyLimit(e.getMaxDailyLimit());
        d.setMinMonthlyLimit(e.getMinMonthlyLimit());
        d.setMaxMonthlyLimit(e.getMaxMonthlyLimit());
        d.setMinYearlyLimit(e.getMinYearlyLimit());
        d.setMaxYearlyLimit(e.getMaxYearlyLimit());
        d.setDefaultDailyLimit(e.getDefaultDailyLimit());
        d.setDefaultMonthlyLimit(e.getDefaultMonthlyLimit());
        d.setDefaultYearlyLimit(e.getDefaultYearlyLimit());
        d.setCreatedAt(toInstant(e.getCreatedAt()));
        d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setUpdatedBy(e.getUpdatedBy());
        d.setVersion(e.getVersion());
        return d;
    }

    // ========== WalletLimitChangeRequest ==========

    public WalletLimitChangeRequestJpaEntity toEntity(WalletLimitChangeRequest domain) {
        if (domain == null) return null;
        WalletLimitChangeRequestJpaEntity e = new WalletLimitChangeRequestJpaEntity();
        e.setId(domain.getId());
        e.setTenantId(domain.getTenantId());
        e.setWalletId(domain.getWalletId());
        e.setCustomerId(domain.getCustomerId());
        e.setRequestedSingleLimit(domain.getRequestedSingleLimit());
        e.setRequestedDailyLimit(domain.getRequestedDailyLimit());
        e.setRequestedMonthlyLimit(domain.getRequestedMonthlyLimit());
        e.setRequestedYearlyLimit(domain.getRequestedYearlyLimit());
        e.setCurrentSingleLimit(domain.getCurrentSingleLimit());
        e.setCurrentDailyLimit(domain.getCurrentDailyLimit());
        e.setCurrentMonthlyLimit(domain.getCurrentMonthlyLimit());
        e.setCurrentYearlyLimit(domain.getCurrentYearlyLimit());
        e.setReason(domain.getReason());
        e.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        e.setRequestedBy(domain.getRequestedBy());
        e.setRequestedAt(toOdt(domain.getRequestedAt()));
        e.setDecisionBy(domain.getDecisionBy());
        e.setDecisionAt(toOdt(domain.getDecisionAt()));
        e.setDecisionNotes(domain.getDecisionNotes());
        e.setRejectionReason(domain.getRejectionReason());
        e.setCreatedAt(toOdt(domain.getCreatedAt()));
        e.setUpdatedAt(toOdt(domain.getUpdatedAt()));
        e.setVersion(domain.getVersion());
        return e;
    }

    public WalletLimitChangeRequest toDomain(WalletLimitChangeRequestJpaEntity e) {
        if (e == null) return null;
        WalletLimitChangeRequest d = new WalletLimitChangeRequest();
        d.setId(e.getId());
        d.setTenantId(e.getTenantId());
        d.setWalletId(e.getWalletId());
        d.setCustomerId(e.getCustomerId());
        d.setRequestedSingleLimit(e.getRequestedSingleLimit());
        d.setRequestedDailyLimit(e.getRequestedDailyLimit());
        d.setRequestedMonthlyLimit(e.getRequestedMonthlyLimit());
        d.setRequestedYearlyLimit(e.getRequestedYearlyLimit());
        d.setCurrentSingleLimit(e.getCurrentSingleLimit());
        d.setCurrentDailyLimit(e.getCurrentDailyLimit());
        d.setCurrentMonthlyLimit(e.getCurrentMonthlyLimit());
        d.setCurrentYearlyLimit(e.getCurrentYearlyLimit());
        d.setReason(e.getReason());
        d.setStatus(e.getStatus() != null ? LimitRequestStatus.valueOf(e.getStatus()) : null);
        d.setRequestedBy(e.getRequestedBy());
        d.setRequestedAt(toInstant(e.getRequestedAt()));
        d.setDecisionBy(e.getDecisionBy());
        d.setDecisionAt(toInstant(e.getDecisionAt()));
        d.setDecisionNotes(e.getDecisionNotes());
        d.setRejectionReason(e.getRejectionReason());
        d.setCreatedAt(toInstant(e.getCreatedAt()));
        d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }

    private OffsetDateTime toOdt(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

    private Instant toInstant(OffsetDateTime odt) {
        return odt != null ? odt.toInstant() : null;
    }
}
