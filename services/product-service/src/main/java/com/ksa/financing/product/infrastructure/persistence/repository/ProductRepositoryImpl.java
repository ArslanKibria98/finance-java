package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.domain.model.AdminFeeSlab;
import com.ksa.financing.product.domain.model.Product;
import com.ksa.financing.product.domain.port.out.ProductRepository;
import com.ksa.financing.product.infrastructure.persistence.mapper.ProductPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Infrastructure implementation of the {@link ProductRepository} output port.
 * Delegates to {@link JpaProductRepository} and uses {@link ProductPersistenceMapper}
 * for domain/JPA entity translation.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductRepositoryImpl implements ProductRepository {

    private final JpaProductRepository jpaProductRepository;
    private final JpaAdminFeeSlabRepository jpaAdminFeeSlabRepository;

    @Override
    public Product save(Product product) {
        log.debug("Persisting product: code={}, tenantId={}", product.getProductCode(), product.getTenantId());

        var jpaEntity = ProductPersistenceMapper.toEntity(product);
        var saved = jpaProductRepository.save(jpaEntity);

        log.debug("Product persisted successfully: id={}", saved.getId());
        return ProductPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Product> findById(UUID tenantId, UUID id) {
        log.debug("Finding product by id={}, tenantId={}", id, tenantId);

        return jpaProductRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .map(entity -> {
                    var product = ProductPersistenceMapper.toDomain(entity);
                    var slabs = jpaAdminFeeSlabRepository
                            .findByProductIdAndTenantIdOrderBySortOrder(id, tenantId)
                            .stream()
                            .map(s -> new AdminFeeSlab(
                                    s.getId(), s.getMinAmount(), s.getMaxAmount(),
                                    s.getProfitPercentage(), s.getProcessingFee(),
                                    s.getAdminFee(), s.getPartnerScope(), s.getStatus(),
                                    s.getSortOrder(), s.getMinTenure(), s.getMaxTenure()))
                            .toList();
                    product.setAdminFeeSlabs(slabs);
                    return product;
                });
    }

    @Override
    public Optional<Product> findByProductCode(UUID tenantId, String productCode) {
        log.debug("Finding product by code={}, tenantId={}", productCode, tenantId);

        return jpaProductRepository.findByProductCodeAndTenantIdAndDeletedAtIsNull(productCode, tenantId)
                .map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public List<Product> findAllByTenant(UUID tenantId) {
        log.debug("Listing all products for tenantId={}", tenantId);

        return jpaProductRepository.findAllByTenantIdAndDeletedAtIsNull(tenantId)
                .stream()
                .map(ProductPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByProductCode(UUID tenantId, String productCode) {
        return jpaProductRepository.existsByProductCodeAndTenantIdAndDeletedAtIsNull(productCode, tenantId);
    }
}
