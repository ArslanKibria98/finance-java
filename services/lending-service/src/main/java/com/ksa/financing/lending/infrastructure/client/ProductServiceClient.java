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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST client for product-service.
 * Product-service is the single source of truth for all product limits
 * (min/max amount, min/max tenure). These are derived from admin fee slabs
 * configured per product. Fineract is NOT consulted for limit validation.
 */
@Slf4j
@Component
public class ProductServiceClient implements ProductConfigPort {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String productServiceUrl;

    public ProductServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.product-service-url:http://localhost:8091}") String productServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.productServiceUrl = productServiceUrl;
    }

    @Override
    public List<ProductConfig> listActiveProducts(UUID tenantId, String authToken) {
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId.toString());
            if (authToken != null && !authToken.isBlank()) {
                headers.set("Authorization", "Bearer " + authToken);
            }

            var url = productServiceUrl + "/api/v1/products";
            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            var rawRoot = objectMapper.readTree(response.getBody());
            var root = rawRoot.has("data") && rawRoot.get("data").isArray() ? rawRoot.get("data") : rawRoot;

            if (!root.isArray()) {
                log.warn("Product-service list response is not an array");
                return List.of();
            }

            var results = new ArrayList<ProductConfig>();
            for (var item : root) {
                var status = textOrNull(item, "status");
                var visibleToCustomers = item.has("visibleToCustomers") && item.get("visibleToCustomers").asBoolean(false);
                var fineractProductId = textOrNull(item, "fineractProductId");

                if (!"ACTIVE".equalsIgnoreCase(status)) continue;
                if (!visibleToCustomers) continue;
                if (fineractProductId == null || fineractProductId.isBlank()) continue;

            results.add(parseProductConfig(item, null, null));
            }

            log.info("Listed {} active customer-visible products for tenant {}", results.size(), tenantId);
            return results;

        } catch (Exception e) {
            log.warn("Product-service list unavailable ({}), returning empty list", e.getMessage());
            return List.of();
        }
    }

    @Override
    public ProductConfig fetchProductConfig(UUID tenantId, String productId, BigDecimal amount, int tenureMonths) {
        if (productId == null || productId.isBlank()) {
            log.info("No productId provided, returning default product config");
            return defaultConfig();
        }

        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId.toString());

            var url = productServiceUrl + "/api/v1/products/" + productId;
            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            var rawRoot = objectMapper.readTree(response.getBody());
            // Unwrap {"data": {...}} wrapper if present
            var root = rawRoot.has("data") && rawRoot.get("data").isObject() ? rawRoot.get("data") : rawRoot;

            return parseProductConfig(root, amount, tenureMonths);

        } catch (Exception e) {
            log.warn("Product-service unavailable ({}), using default config", e.getMessage());
            return defaultConfig();
        }
    }

    @Override
    public Optional<ProductSummary> fetchProductSummary(UUID tenantId, String productId) {
        if (productId == null || productId.isBlank()) return Optional.empty();
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId.toString());

            var url = productServiceUrl + "/api/v1/products/" + productId;
            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            var rawRoot = objectMapper.readTree(response.getBody());
            var root = rawRoot.has("data") && rawRoot.get("data").isObject() ? rawRoot.get("data") : rawRoot;

            return Optional.of(new ProductSummary(
                    textOrNull(root, "id") != null ? textOrNull(root, "id") : productId,
                    textOrNull(root, "nameEn"),
                    textOrNull(root, "nameAr")
            ));
        } catch (Exception e) {
            log.warn("Product-service summary unavailable for {} ({})", productId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parses product-service response into ProductConfig.
     * min/max amount and tenure come from slab-derived fields in the response —
     * product-service already aggregates all admin fee slabs into these top-level values.
     * If slabs are present in the response, we re-derive from them for accuracy.
     */
    private ProductConfig parseProductConfig(JsonNode root, BigDecimal requestedAmount, Integer requestedTenure) {
        // Try to derive limits from slabs if present (most accurate)
        var slabsNode = root.path("adminFeeSlabs");
        BigDecimal minAmount = null;
        BigDecimal maxAmount = null;
        int minTenure = 0;
        int maxTenure = 0;
        List<Integer> allowedTenures = List.of();

        // Slab matching variables
        BigDecimal slabProfitRate = null;
        BigDecimal slabProcFee = null;
        BigDecimal slabAdminFee = null;

        if (slabsNode.isArray() && slabsNode.size() > 0) {
            for (var slab : slabsNode) {
                var slabMin = decimalOrNull(slab, "minAmount");
                var slabMax = decimalOrNull(slab, "maxAmount");
                var slabMinT = intOrNull(slab, "minTenure");
                var slabMaxT = intOrNull(slab, "maxTenure");

                // Update global limits
                if (slabMin != null) minAmount = minAmount == null ? slabMin : minAmount.min(slabMin);
                if (slabMax != null) maxAmount = maxAmount == null ? slabMax : maxAmount.max(slabMax);
                if (slabMinT != null) minTenure = minTenure == 0 ? slabMinT : Math.min(minTenure, slabMinT);
                if (slabMaxT != null) maxTenure = Math.max(maxTenure, slabMaxT);

                // Check for match if request context is provided
                if (requestedAmount != null && slabProfitRate == null) {
                    boolean amountMatch = (slabMin == null || requestedAmount.compareTo(slabMin) >= 0) &&
                                         (slabMax == null || requestedAmount.compareTo(slabMax) <= 0);
                    
                    boolean tenureMatch = true;
                    if (requestedTenure != null) {
                        tenureMatch = (slabMinT == null || requestedTenure >= slabMinT) &&
                                      (slabMaxT == null || requestedTenure <= slabMaxT);
                    }

                    if (amountMatch && tenureMatch) {
                        slabProfitRate = decimalOrNull(slab, "profitPercentage");
                        slabProcFee = decimalOrNull(slab, "processingFee");
                        slabAdminFee = decimalOrNull(slab, "adminFee");
                        log.info("Matching slab found for amount={} tenure={}: profit={}, adminFee={}, procFee={}",
                                requestedAmount, requestedTenure, slabProfitRate, slabAdminFee, slabProcFee);
                    }
                }
            }

            if (minTenure > 0 && maxTenure > 0) {
                var tenureList = new ArrayList<Integer>();
                for (int i = minTenure; i <= maxTenure; i++) tenureList.add(i);
                allowedTenures = List.copyOf(tenureList);
            }

            log.info("Product limits derived from {} slabs: amount [{} - {}], tenure [{} - {}]",
                    slabsNode.size(), minAmount, maxAmount, minTenure, maxTenure);
        }

        // Fallback to top-level fields if slabs not present or incomplete
        if (minAmount == null) minAmount = decimalOrDefault(root, "minAmount", null);
        if (maxAmount == null) maxAmount = decimalOrDefault(root, "maxAmount", null);
        if (minTenure == 0) minTenure = intOrDefault(root, "minTenureMonths", 0);
        if (maxTenure == 0) maxTenure = intOrDefault(root, "maxTenureMonths", 0);
        if (allowedTenures.isEmpty()) {
            var at = root.path("allowedTenures");
            if (at.isArray() && at.size() > 0) {
                var list = new ArrayList<Integer>();
                for (var t : at) list.add(t.asInt());
                allowedTenures = List.copyOf(list);
            }
        }

        // Final values: Priority to Slab, then Root, then Default
        BigDecimal profitRate = slabProfitRate != null ? slabProfitRate : decimalOrDefault(root, "baseProfitRate", new BigDecimal("0.0385"));
        // Normalize to decimal (e.g. 2.5 -> 0.025)
        if (profitRate != null && profitRate.compareTo(new BigDecimal("0.5")) > 0) {
            profitRate = profitRate.divide(new BigDecimal("100"), 6, java.math.RoundingMode.HALF_UP);
        }
        BigDecimal processingFeeAmount = slabProcFee != null ? slabProcFee : decimalOrDefault(root, "processingFeeAmount", BigDecimal.ZERO);
        BigDecimal adminFeeAmount = slabAdminFee != null ? slabAdminFee : decimalOrDefault(root, "adminFeeAmount", BigDecimal.ZERO);

        return new ProductConfig(
                textOrNull(root, "id"),
                textOrNull(root, "nameEn"),
                textOrNull(root, "productCode"),
                textOrNull(root, "shariaStructure"),
                profitRate,
                decimalOrDefault(root, "costOfTermPercent", null),
                decimalOrDefault(root, "processingFeePercent", BigDecimal.ZERO),
                processingFeeAmount,
                adminFeeAmount,
                decimalOrDefault(root, "vatPercent", new BigDecimal("15")),
                minAmount,
                maxAmount,
                minTenure,
                maxTenure,
                allowedTenures,
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
                null, null,
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
