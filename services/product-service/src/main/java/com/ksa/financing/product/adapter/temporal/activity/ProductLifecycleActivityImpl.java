package com.ksa.financing.product.adapter.temporal.activity;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.product.domain.port.out.EventPublisherPort;
import com.ksa.financing.product.domain.port.out.ProductRepository;
import com.ksa.islamic.orchestration.activity.product.ProductLifecycleActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductLifecycleActivityImpl implements ProductLifecycleActivity {

    private final ProductRepository productRepository;
    private final EventPublisherPort eventPublisher;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void updateStatusToPendingActivation(UpdateStatusInput input) {
        log.info("Updating product status to PENDING_ACTIVATION: productId={}", input.productId());
        var tenantId = UUID.fromString(input.tenantId());
        var productId = UUID.fromString(input.productId());

        var product = productRepository.findById(tenantId, productId)
                .orElseThrow(() -> NotFoundException.forEntity("Product", input.productId()));

        product.requestActivation();
        productRepository.save(product);
        log.info("Product status updated to PENDING_ACTIVATION: {}", productId);
    }

    @Override
    @Transactional
    public void activateProduct(ActivateProductInput input) {
        log.info("Activating product: productId={} fineractProductId={}", input.productId(), input.fineractProductId());
        var tenantId = UUID.fromString(input.tenantId());
        var productId = UUID.fromString(input.productId());

        var product = productRepository.findById(tenantId, productId)
                .orElseThrow(() -> NotFoundException.forEntity("Product", input.productId()));

        product.activate(input.fineractProductId());
        productRepository.save(product);
        eventPublisher.publishProductActivated(product);
        log.info("Product activated successfully: {}", productId);
    }

    @Override
    @Transactional
    public void markActivationFailed(MarkFailedInput input) {
        log.info("Marking product activation as failed: productId={} reason={}", input.productId(), input.reason());
        var tenantId = UUID.fromString(input.tenantId());
        var productId = UUID.fromString(input.productId());

        var product = productRepository.findById(tenantId, productId)
                .orElseThrow(() -> NotFoundException.forEntity("Product", input.productId()));

        product.markActivationFailed();
        productRepository.save(product);
        log.warn("Product activation failed: {} reason={}", productId, input.reason());
    }

    @Override
    @Transactional
    public void revertToDraft(RevertToDraftInput input) {
        log.info("Reverting product to DRAFT: productId={}", input.productId());
        var tenantId = UUID.fromString(input.tenantId());
        var productId = UUID.fromString(input.productId());

        var product = productRepository.findById(tenantId, productId)
                .orElseThrow(() -> NotFoundException.forEntity("Product", input.productId()));

        product.revertToDraft();
        productRepository.save(product);
        log.info("Product reverted to DRAFT: {}", productId);
    }

    @Override
    @Transactional
    public void storeFineractMapping(StoreMappingInput input) {
        log.info("Storing Fineract mapping: productId={} fineractProductId={}", input.productId(), input.fineractProductId());

        var now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                INSERT INTO product_fineract_mappings
                    (id, tenant_id, product_id, product_code, fineract_product_id, synced_at, created_at, updated_at)
                VALUES (?, ?::uuid, ?::uuid, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, product_id) DO UPDATE
                SET fineract_product_id = EXCLUDED.fineract_product_id,
                    product_code = EXCLUDED.product_code,
                    synced_at = EXCLUDED.synced_at,
                    updated_at = EXCLUDED.updated_at
                """,
                UUID.randomUUID(),
                input.tenantId(),
                input.productId(),
                input.productCode(),
                input.fineractProductId(),
                now, now, now
        );
        log.info("Fineract mapping stored successfully for product: {}", input.productId());
    }
}
