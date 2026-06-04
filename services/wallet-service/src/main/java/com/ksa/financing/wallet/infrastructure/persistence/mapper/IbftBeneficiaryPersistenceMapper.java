package com.ksa.financing.wallet.infrastructure.persistence.mapper;

import com.ksa.financing.wallet.domain.model.IbftBeneficiary;
import com.ksa.financing.wallet.infrastructure.persistence.entity.IbftBeneficiaryJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class IbftBeneficiaryPersistenceMapper {

    public IbftBeneficiaryJpaEntity toEntity(IbftBeneficiary d) {
        if (d == null) return null;
        var e = new IbftBeneficiaryJpaEntity();
        e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setCustomerId(d.getCustomerId());
        e.setWalletId(d.getWalletId());
        e.setNickname(d.getNickname());
        e.setBeneficiaryName(d.getBeneficiaryName());
        e.setInstitutionNumber(d.getInstitutionNumber());
        e.setTransit(d.getTransit());
        e.setAccountNumber(d.getAccountNumber());
        e.setBankName(d.getBankName());
        e.setCurrency(d.getCurrency());
        e.setValidated(d.isValidated());
        e.setValidationRef(d.getValidationRef());
        e.setValidationResult(d.getValidationResult());
        e.setActive(d.isActive());
        e.setCreatedAt(toOffset(d.getCreatedAt()));
        e.setUpdatedAt(toOffset(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    public IbftBeneficiary toDomain(IbftBeneficiaryJpaEntity e) {
        if (e == null) return null;
        var d = new IbftBeneficiary();
        d.setId(e.getId());
        d.setTenantId(e.getTenantId());
        d.setCustomerId(e.getCustomerId());
        d.setWalletId(e.getWalletId());
        d.setNickname(e.getNickname());
        d.setBeneficiaryName(e.getBeneficiaryName());
        d.setInstitutionNumber(e.getInstitutionNumber());
        d.setTransit(e.getTransit());
        d.setAccountNumber(e.getAccountNumber());
        d.setBankName(e.getBankName());
        d.setCurrency(e.getCurrency());
        d.setValidated(e.isValidated());
        d.setValidationRef(e.getValidationRef());
        d.setValidationResult(e.getValidationResult());
        d.setActive(e.isActive());
        d.setCreatedAt(toInstant(e.getCreatedAt()));
        d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }

    private OffsetDateTime toOffset(Instant i) { return i != null ? i.atOffset(ZoneOffset.UTC) : null; }
    private Instant toInstant(OffsetDateTime o) { return o != null ? o.toInstant() : null; }
}
