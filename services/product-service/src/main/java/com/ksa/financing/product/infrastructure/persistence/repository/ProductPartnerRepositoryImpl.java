package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.domain.port.out.ProductPartnerRepository;
import com.ksa.financing.product.infrastructure.persistence.entity.ProductPartnerAffiliationJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Infrastructure implementation of the {@link ProductPartnerRepository} output port.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductPartnerRepositoryImpl implements ProductPartnerRepository {

    private final JpaProductPartnerAffiliationRepository jpaAffiliationRepository;

    @Override
    public void saveAffiliation(UUID tenantId, UUID productId, UUID partnerId,
                                String affiliationType, BigDecimal commissionPercentage) {
        log.debug("Saving partner affiliation: productId={}, partnerId={}, type={}",
                productId, partnerId, affiliationType);

        var now = OffsetDateTime.now(ZoneOffset.UTC);

        var entity = new ProductPartnerAffiliationJpaEntity();
        entity.setTenantId(tenantId);
        entity.setProductId(productId);
        entity.setPartnerId(partnerId);
        entity.setAffiliationType(affiliationType);
        entity.setCommissionPercentage(commissionPercentage);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        jpaAffiliationRepository.save(entity);
        log.debug("Partner affiliation saved: productId={}, partnerId={}", productId, partnerId);
    }

    @Override
    @Transactional
    public void deleteAffiliation(UUID productId, UUID partnerId) {
        log.debug("Deleting partner affiliation: productId={}, partnerId={}", productId, partnerId);

        jpaAffiliationRepository.deleteByProductIdAndPartnerId(productId, partnerId);
        log.debug("Partner affiliation deleted: productId={}, partnerId={}", productId, partnerId);
    }

    @Override
    public boolean existsAffiliation(UUID productId, UUID partnerId) {
        return jpaAffiliationRepository.existsByProductIdAndPartnerId(productId, partnerId);
    }
}
