package com.ksa.financing.wallet.infrastructure.persistence.mapper;

import com.ksa.financing.wallet.domain.model.IbanBeneficiary;
import com.ksa.financing.wallet.infrastructure.persistence.entity.IbanBeneficiaryJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class IbanBeneficiaryPersistenceMapper {

    public IbanBeneficiaryJpaEntity toEntity(IbanBeneficiary d) {
        if (d == null) return null;
        IbanBeneficiaryJpaEntity e = new IbanBeneficiaryJpaEntity();
        e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setCustomerId(d.getCustomerId());
        e.setWalletId(d.getWalletId());
        e.setNickname(d.getNickname());
        e.setBeneficiaryName(d.getBeneficiaryName());
        e.setIban(d.getIban());
        e.setBankCode(d.getBankCode());
        e.setBankName(d.getBankName());
        e.setVerified(d.isVerified());
        e.setVerifiedAt(toOffset(d.getVerifiedAt()));
        e.setActive(d.isActive());
        e.setCreatedAt(toOffset(d.getCreatedAt()));
        e.setUpdatedAt(toOffset(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    public IbanBeneficiary toDomain(IbanBeneficiaryJpaEntity e) {
        if (e == null) return null;
        IbanBeneficiary d = new IbanBeneficiary();
        d.setId(e.getId());
        d.setTenantId(e.getTenantId());
        d.setCustomerId(e.getCustomerId());
        d.setWalletId(e.getWalletId());
        d.setNickname(e.getNickname());
        d.setBeneficiaryName(e.getBeneficiaryName());
        d.setIban(e.getIban());
        d.setBankCode(e.getBankCode());
        d.setBankName(e.getBankName());
        d.setVerified(e.isVerified());
        d.setVerifiedAt(toInstant(e.getVerifiedAt()));
        d.setActive(e.isActive());
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
