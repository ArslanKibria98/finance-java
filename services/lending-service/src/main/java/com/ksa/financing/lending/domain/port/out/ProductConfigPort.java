package com.ksa.financing.lending.domain.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port to fetch product configuration from product-service.
 * Used by finance calculator and eligibility checks.
 */
public interface ProductConfigPort {

    /**
     * Fetch product financial configuration for a given amount and tenure.
     *
     * @param tenantId     Tenant ID
     * @param productId    Product UUID (nullable — uses default product if null)
     * @param amount       Requested financing amount
     * @param tenureMonths Requested tenure in months
     * @return Product configuration with applicable fees and rates
     */
    ProductConfig fetchProductConfig(UUID tenantId, String productId, BigDecimal amount, int tenureMonths);

    /**
     * List all ACTIVE products visible to customers for the given tenant.
     * Used by the financial-first product suggestion flow.
     *
     * @param tenantId  Tenant ID
     * @param authToken JWT bearer token to forward for product-service auth
     * @return List of full product configs, empty if none found or on error
     */
    List<ProductConfig> listActiveProducts(UUID tenantId, String authToken);

    /**
     * Fetch the lightweight product summary (id + bilingual names) for response enrichment.
     * Returns empty when the product is unknown or product-service is unavailable.
     */
    Optional<ProductSummary> fetchProductSummary(UUID tenantId, String productId);

    record ProductSummary(String id, String nameEn, String nameAr) {}

    record ProductConfig(
            String productId,
            String productName,
            String productCode,
            String shariaStructure,
            BigDecimal profitRate,
            BigDecimal costOfTermPercent,
            BigDecimal processingFeePercent,
            BigDecimal processingFeeAmount,
            BigDecimal adminFeeAmount,
            BigDecimal vatPercent,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int minTenureMonths,
            int maxTenureMonths,
            List<Integer> allowedTenures,
            int defaultTenureMonths,
            BigDecimal minSalary,
            int minAge,
            int maxAge,
            int minEmploymentMonths,
            BigDecimal maxDbrPercent,
            boolean fineractLinked,
            boolean isDisbursementInclusive
    ) {
        /** Backward-compatible constructor (defaults fineractLinked=true, isDisbursementInclusive=true) */
        public ProductConfig(
                String productId, String productName, String productCode, String shariaStructure,
                BigDecimal profitRate, BigDecimal costOfTermPercent,
                BigDecimal processingFeePercent, BigDecimal processingFeeAmount,
                BigDecimal adminFeeAmount, BigDecimal vatPercent,
                BigDecimal minAmount, BigDecimal maxAmount,
                int minTenureMonths, int maxTenureMonths,
                List<Integer> allowedTenures, int defaultTenureMonths,
                BigDecimal minSalary, int minAge, int maxAge, int minEmploymentMonths,
                BigDecimal maxDbrPercent) {
            this(productId, productName, productCode, shariaStructure,
                    profitRate, costOfTermPercent, processingFeePercent, processingFeeAmount,
                    adminFeeAmount, vatPercent, minAmount, maxAmount,
                    minTenureMonths, maxTenureMonths, allowedTenures, defaultTenureMonths,
                    minSalary, minAge, maxAge, minEmploymentMonths, maxDbrPercent, true, true);
        }

        /** Backward-compatible constructor (defaults isDisbursementInclusive=true) */
        public ProductConfig(
                String productId, String productName, String productCode, String shariaStructure,
                BigDecimal profitRate, BigDecimal costOfTermPercent,
                BigDecimal processingFeePercent, BigDecimal processingFeeAmount,
                BigDecimal adminFeeAmount, BigDecimal vatPercent,
                BigDecimal minAmount, BigDecimal maxAmount,
                int minTenureMonths, int maxTenureMonths,
                List<Integer> allowedTenures, int defaultTenureMonths,
                BigDecimal minSalary, int minAge, int maxAge, int minEmploymentMonths,
                BigDecimal maxDbrPercent, boolean fineractLinked) {
            this(productId, productName, productCode, shariaStructure,
                    profitRate, costOfTermPercent, processingFeePercent, processingFeeAmount,
                    adminFeeAmount, vatPercent, minAmount, maxAmount,
                    minTenureMonths, maxTenureMonths, allowedTenures, defaultTenureMonths,
                    minSalary, minAge, maxAge, minEmploymentMonths, maxDbrPercent, fineractLinked, true);
        }
    }
}
