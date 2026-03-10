package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.domain.model.Partner;
import com.ksa.financing.product.domain.port.out.PartnerRepository;
import com.ksa.financing.product.infrastructure.persistence.entity.PartnerJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PartnerRepositoryImpl implements PartnerRepository {

    private final JpaPartnerRepository jpaPartnerRepository;

    @Override
    public Partner save(Partner partner) {
        var entity = toJpaEntity(partner);
        var saved = jpaPartnerRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Partner> findById(UUID tenantId, UUID partnerId) {
        return jpaPartnerRepository.findByTenantIdAndId(tenantId, partnerId)
                .map(this::toDomain);
    }

    @Override
    public List<Partner> findAllByTenant(UUID tenantId) {
        return jpaPartnerRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(UUID tenantId, String partnerCode) {
        return jpaPartnerRepository.existsByTenantIdAndPartnerCode(tenantId, partnerCode);
    }

    // === Mapping ===

    private PartnerJpaEntity toJpaEntity(Partner p) {
        var e = new PartnerJpaEntity();
        e.setId(p.getId());
        e.setTenantId(p.getTenantId());
        e.setPartnerCode(p.getPartnerCode());
        e.setNameEn(p.getNameEn());
        e.setNameAr(p.getNameAr());
        e.setEmail(p.getEmail());
        e.setPhone(p.getPhone());
        e.setContactPerson(p.getContactPerson());
        e.setLogoUrl(p.getLogoUrl());
        e.setStatus(p.getStatus());
        e.setCreatedBy(p.getCreatedBy());
        e.setUpdatedBy(p.getUpdatedBy());
        e.setVersion(p.getVersion());

        var now = OffsetDateTime.now(ZoneOffset.UTC);
        e.setCreatedAt(p.getCreatedAt() != null ? p.getCreatedAt().atOffset(ZoneOffset.UTC) : now);
        e.setUpdatedAt(now);
        return e;
    }

    private Partner toDomain(PartnerJpaEntity e) {
        var p = new Partner();
        p.setId(e.getId());
        p.setTenantId(e.getTenantId());
        p.setPartnerCode(e.getPartnerCode());
        p.setNameEn(e.getNameEn());
        p.setNameAr(e.getNameAr());
        p.setEmail(e.getEmail());
        p.setPhone(e.getPhone());
        p.setContactPerson(e.getContactPerson());
        p.setLogoUrl(e.getLogoUrl());
        p.setStatus(e.getStatus());
        p.setCreatedBy(e.getCreatedBy());
        p.setUpdatedBy(e.getUpdatedBy());
        p.setVersion(e.getVersion());
        p.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null);
        p.setUpdatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant() : null);
        return p;
    }
}
