package com.ksa.financing.product.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.product.domain.model.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(UUID tenantId, UUID id);
    Optional<Product> findById(UUID id);
    Optional<Product> findByProductCode(UUID tenantId, String productCode);
    List<Product> findAllByTenant(UUID tenantId);
    PageResponse<Product> findAllByTenant(UUID tenantId, PageQuery query);
    boolean existsByProductCode(UUID tenantId, String productCode);
}
