package com.ksa.financing.product.adapter.temporal.activity;

import com.ksa.financing.product.domain.port.out.ProductRepository;
import com.ksa.islamic.orchestration.activity.product.ProductValidationActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductValidationActivityImpl implements ProductValidationActivity {

    private final ProductRepository productRepository;

    @Override
    public ValidationResult validateForActivation(ValidateProductInput input) {
        log.info("Validating product for activation: productId={} tenant={}", input.productId(), input.tenantId());

        var tenantId = UUID.fromString(input.tenantId());
        var productId = UUID.fromString(input.productId());

        var productOpt = productRepository.findById(tenantId, productId);
        if (productOpt.isEmpty()) {
            return new ValidationResult(false, List.of("Product not found: " + input.productId()),
                    null, null, null, null, null, null, null, null, null, 0, 0, null, null, null, 0, false, null);
        }

        var product = productOpt.get();
        var errors = new ArrayList<String>();

        if (product.getProductCode() == null || product.getProductCode().isBlank()) {
            errors.add("Product code is required");
        }
        if (product.getNameEn() == null || product.getNameEn().isBlank()) {
            errors.add("Product name (English) is required");
        }
        if (product.getProductType() == null) {
            errors.add("Product type is required");
        }
        if (product.getMinAmount() == null) {
            errors.add("Minimum amount is required");
        }
        if (product.getMaxAmount() == null) {
            errors.add("Maximum amount is required");
        }
        if (product.getBaseProfitRate() == null) {
            errors.add("Base profit rate is required");
        }
        if (product.getMinTenureMonths() <= 0) {
            errors.add("Minimum tenure must be positive");
        }
        if (product.getMaxTenureMonths() <= 0) {
            errors.add("Maximum tenure must be positive");
        }

        boolean valid = errors.isEmpty();
        log.info("Product validation result: valid={} errors={}", valid, errors.size());

        return new ValidationResult(
                valid,
                errors,
                product.getProductCode(),
                product.getNameEn(),
                product.getNameAr(),
                product.getProductType() != null ? product.getProductType().name() : null,
                product.getShariaStructure(),
                product.getTargetSegment(),
                product.getCurrency(),
                product.getMinAmount(),
                product.getMaxAmount(),
                product.getMinTenureMonths(),
                product.getMaxTenureMonths(),
                product.getBaseProfitRate(),
                product.getRateType(),
                product.getRepaymentFrequency(),
                product.getGracePeriodDays(),
                product.isEarlySettlementAllowed(),
                product.getAllowedTenures()
        );
    }
}
