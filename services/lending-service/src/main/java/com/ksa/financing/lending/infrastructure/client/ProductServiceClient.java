package com.ksa.financing.lending.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.lending.domain.port.out.ProductConfigPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * REST client for product-service + Fineract loan product validation.
 * Merges product-service config with Fineract loan product limits
 * to ensure amounts/tenures are valid in both systems before processing.
 */
@Slf4j
@Component
public class ProductServiceClient implements ProductConfigPort {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String productServiceUrl;
    private final String fineractBaseUrl;
    private final String fineractUsername;
    private final String fineractPassword;
    private final String fineractTenantId;

    public ProductServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.product-service-url:http://localhost:8091}") String productServiceUrl,
            @Value("${app.services.fineract-base-url:https://localhost:8443/fineract-provider/api/v1}") String fineractBaseUrl,
            @Value("${fineract.username:mifos}") String fineractUsername,
            @Value("${fineract.password:password}") String fineractPassword,
            @Value("${fineract.tenant-id:default}") String fineractTenantId
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.productServiceUrl = productServiceUrl;
        this.fineractBaseUrl = fineractBaseUrl;
        this.fineractUsername = fineractUsername;
        this.fineractPassword = fineractPassword;
        this.fineractTenantId = fineractTenantId;
    }

    @Override
    public ProductConfig fetchProductConfig(UUID tenantId, String productId, BigDecimal amount, int tenureMonths) {
        if (productId == null || productId.isBlank()) {
            log.info("No productId provided, returning default product config");
            return defaultConfig();
        }

        ProductConfig config;
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId.toString());

            var url = productServiceUrl + "/api/v1/products/" + productId;
            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            var rawRoot = objectMapper.readTree(response.getBody());
            // Unwrap {"data": {...}} wrapper if present
            var root = rawRoot.has("data") && rawRoot.get("data").isObject() ? rawRoot.get("data") : rawRoot;
            config = parseProductConfig(root, amount, tenureMonths);

            // Extract fineractProductId from product-service response
            var fineractProdId = textOrNull(root, "fineractProductId");
            if (fineractProdId == null) {
                // Also check nested data wrapper
                var dataNode = root.path("data");
                if (!dataNode.isMissingNode()) {
                    fineractProdId = textOrNull(dataNode, "fineractProductId");
                }
            }

            // Merge with Fineract limits using direct product ID
            return mergeWithFineractLimits(config, fineractProdId);

        } catch (Exception e) {
            log.warn("Product-service unavailable ({}), using default config", e.getMessage());
            config = defaultConfig();
        }

        return mergeWithFineractLimits(config, null);
    }

    /**
     * Fetches Fineract loan product limits and merges with product-service config.
     * Uses fineractProductId for direct lookup if available, otherwise falls back to sharia structure matching.
     */
    private ProductConfig mergeWithFineractLimits(ProductConfig config, String fineractProductId) {
        try {
            // Only match by fineractProductId — no guessing/fallback
            if (fineractProductId == null || fineractProductId.isBlank()) {
                log.warn("Product '{}' has no fineractProductId — not linked to Fineract", config.productName());
                return new ProductConfig(
                        config.productId(), config.productName(), config.productCode(), config.shariaStructure(),
                        config.profitRate(), config.costOfTermPercent(),
                        config.processingFeePercent(), config.processingFeeAmount(),
                        config.adminFeeAmount(), config.vatPercent(),
                        config.minAmount(), config.maxAmount(),
                        config.minTenureMonths(), config.maxTenureMonths(),
                        config.allowedTenures(), config.defaultTenureMonths(),
                        config.minSalary(), config.minAge(), config.maxAge(), config.minEmploymentMonths(),
                        config.maxDbrPercent(), false
                );
            }

            JsonNode matched = fetchFineractLoanProductById(fineractProductId);
            if (matched != null) {
                log.info("Fineract product matched by ID: {} → {}", fineractProductId, matched.path("name").asText());
            }

            if (matched == null) {
                log.warn("No matching Fineract loan product found for product: {} (sharia: {}, fineractId: {})",
                        config.productName(), config.shariaStructure(), fineractProductId);
                return new ProductConfig(
                        config.productId(), config.productName(), config.productCode(), config.shariaStructure(),
                        config.profitRate(), config.costOfTermPercent(),
                        config.processingFeePercent(), config.processingFeeAmount(),
                        config.adminFeeAmount(), config.vatPercent(),
                        config.minAmount(), config.maxAmount(),
                        config.minTenureMonths(), config.maxTenureMonths(),
                        config.allowedTenures(), config.defaultTenureMonths(),
                        config.minSalary(), config.minAge(), config.maxAge(), config.minEmploymentMonths(),
                        config.maxDbrPercent(), false  // NOT linked to Fineract
                );
            }

            var fMinPrincipal = decimalOrNull(matched, "minPrincipal");
            var fMaxPrincipal = decimalOrNull(matched, "maxPrincipal");
            var fMinTenure = intOrNull(matched, "minNumberOfRepayments");
            var fMaxTenure = intOrNull(matched, "maxNumberOfRepayments");

            log.info("Fineract limits for {}: principal [{} - {}], tenure [{} - {}]",
                    matched.path("name").asText(), fMinPrincipal, fMaxPrincipal, fMinTenure, fMaxTenure);

            // Merge: take stricter limits
            var mergedMinAmount = stricterMin(config.minAmount(), fMinPrincipal);
            var mergedMaxAmount = stricterMax(config.maxAmount(), fMaxPrincipal);
            var mergedMinTenure = Math.max(config.minTenureMonths(), fMinTenure != null ? fMinTenure : 0);
            int mergedMaxTenure;
            if (config.maxTenureMonths() > 0 && fMaxTenure != null) {
                mergedMaxTenure = Math.min(config.maxTenureMonths(), fMaxTenure);
            } else if (fMaxTenure != null) {
                mergedMaxTenure = fMaxTenure;
            } else {
                mergedMaxTenure = config.maxTenureMonths();
            }

            return new ProductConfig(
                    config.productId(), config.productName(), config.productCode(), config.shariaStructure(),
                    config.profitRate(), config.costOfTermPercent(),
                    config.processingFeePercent(), config.processingFeeAmount(),
                    config.adminFeeAmount(), config.vatPercent(),
                    mergedMinAmount, mergedMaxAmount,
                    mergedMinTenure, mergedMaxTenure,
                    config.allowedTenures(), config.defaultTenureMonths(),
                    config.minSalary(), config.minAge(), config.maxAge(), config.minEmploymentMonths(),
                    config.maxDbrPercent()
            );
        } catch (Exception e) {
            log.error("Fineract unavailable for limit merge: {} ({})", e.getMessage(), e.getClass().getSimpleName());
            // Fineract is down — cannot validate, mark as not linked
            return new ProductConfig(
                    config.productId(), config.productName(), config.productCode(), config.shariaStructure(),
                    config.profitRate(), config.costOfTermPercent(),
                    config.processingFeePercent(), config.processingFeeAmount(),
                    config.adminFeeAmount(), config.vatPercent(),
                    config.minAmount(), config.maxAmount(),
                    config.minTenureMonths(), config.maxTenureMonths(),
                    config.allowedTenures(), config.defaultTenureMonths(),
                    config.minSalary(), config.minAge(), config.maxAge(), config.minEmploymentMonths(),
                    config.maxDbrPercent(), false
            );
        }
    }

    private JsonNode fetchFineractLoanProductById(String fineractProductId) {
        try {
            var headers = new HttpHeaders();
            headers.set("Fineract-Platform-TenantId", fineractTenantId);
            headers.set("Authorization", "Basic " +
                    Base64.getEncoder().encodeToString((fineractUsername + ":" + fineractPassword).getBytes()));

            var response = restTemplate.exchange(
                    fineractBaseUrl + "/loanproducts/" + fineractProductId,
                    HttpMethod.GET, new HttpEntity<>(headers), String.class);

            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to fetch Fineract loan product {}: {}", fineractProductId, e.getMessage());
            return null;
        }
    }

    private BigDecimal stricterMin(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) > 0 ? a : b; // higher min = stricter
    }

    private BigDecimal stricterMax(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) < 0 ? a : b; // lower max = stricter
    }

    private ProductConfig parseProductConfig(JsonNode root, BigDecimal amount, int tenureMonths) {
        return new ProductConfig(
                textOrNull(root, "id"),
                textOrNull(root, "nameEn"),
                textOrNull(root, "code"),
                textOrNull(root, "shariaStructure"),
                decimalOrDefault(root, "baseProfitRate", new BigDecimal("0.0385")),
                decimalOrDefault(root, "costOfTermPercent", null),
                decimalOrDefault(root, "processingFeePercent", BigDecimal.ZERO),
                decimalOrDefault(root, "processingFeeAmount", BigDecimal.ZERO),
                decimalOrDefault(root, "adminFeeAmount", BigDecimal.ZERO),
                decimalOrDefault(root, "vatPercent", new BigDecimal("15")),
                decimalOrDefault(root, "minAmount", null),
                decimalOrDefault(root, "maxAmount", null),
                intOrDefault(root, "minTenureMonths", 0),
                intOrDefault(root, "maxTenureMonths", 0),
                List.of(),
                0,
                decimalOrDefault(root, "minSalary", new BigDecimal("4000")),
                intOrDefault(root, "minAge", 18),
                intOrDefault(root, "maxAge", 60),
                intOrDefault(root, "minEmploymentMonths", 6),
                decimalOrDefault(root, "maxDbrPercent", new BigDecimal("65"))
        );
    }

    private ProductConfig defaultConfig() {
        return new ProductConfig(
                null, "Micro Finance", "FAZZA", "MURABAHA",
                new BigDecimal("0.0385"),
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("15"),
                null,  // min/max from Fineract only
                null,
                0, 0,
                List.of(),
                0,
                new BigDecimal("4000"),
                18, 60, 6,
                new BigDecimal("65")
        );
    }

    private String textOrNull(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText() : null;
    }

    private BigDecimal decimalOrDefault(JsonNode node, String field, BigDecimal defaultVal) {
        if (node != null && node.has(field) && !node.get(field).isNull()) {
            return new BigDecimal(node.get(field).asText());
        }
        return defaultVal;
    }

    private BigDecimal decimalOrNull(JsonNode node, String field) {
        if (node != null && node.has(field) && !node.get(field).isNull()) {
            return new BigDecimal(node.get(field).asText());
        }
        return null;
    }

    private Integer intOrNull(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull()
                ? node.get(field).asInt() : null;
    }

    private int intOrDefault(JsonNode node, String field, int defaultVal) {
        return node != null && node.has(field) && !node.get(field).isNull()
                ? node.get(field).asInt() : defaultVal;
    }
}
