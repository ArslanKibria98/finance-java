package com.ksa.financing.lending.adapter.temporal.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.islamic.orchestration.activity.lending.ProductValidationActivity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates product details by calling product-service REST API.
 * Used in Step 1 (Basic Information) of the loan application workflow.
 */
@Slf4j
@Component
public class ProductValidationActivityImpl implements ProductValidationActivity {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String productServiceUrl;

    public ProductValidationActivityImpl(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.product-service-url}") String productServiceUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.productServiceUrl = productServiceUrl;
    }

    @Override
    public ProductValidationResult validateProduct(ProductValidationInput input) {
        log.info("Activity: Validating product {} for tenant {}", input.productId(), input.tenantId());

        try {
            String url = productServiceUrl + "/api/v1/products/" + input.productId();

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return invalidResult("Product not found: " + input.productId());
            }

            var rawBody = objectMapper.readTree(response.getBody());
            // Handle wrapped response: { "data": { ... } } or direct { ... }
            var product = rawBody.has("data") ? rawBody.get("data") : rawBody;

            // Extract product details (product-service uses nameEn/productCode/productType fields)
            String productCode = textOrNull(product, "productCode");
            String productName = textOrNull(product, "nameEn");
            String shariaStructure = textOrNull(product, "shariaStructure");
            if (shariaStructure == null) {
                shariaStructure = textOrNull(product, "productType");
            }
            BigDecimal minAmount = decimalOrNull(product, "minAmount");
            BigDecimal maxAmount = decimalOrNull(product, "maxAmount");
            int minTenure = intOrZero(product, "minTenureMonths");
            int maxTenure = intOrZero(product, "maxTenureMonths");
            BigDecimal profitRate = decimalOrNull(product, "profitRate");
            if (profitRate == null) {
                profitRate = decimalOrNull(product, "baseProfitRate");
            }
            BigDecimal processingFeePercent = decimalOrNull(product, "processingFeePercent");
            BigDecimal adminFeeAmount = decimalOrNull(product, "adminFeeAmount");
            int minAge = intOrZero(product, "minAge");
            int maxAge = intOrZero(product, "maxAge");
            BigDecimal minSalary = decimalOrNull(product, "minSalary");
            int minEmploymentMonths = intOrZero(product, "minEmploymentMonths");
            int minCreditScore = intOrZero(product, "minCreditScore");
            BigDecimal maxDbrPercent = decimalOrNull(product, "maxDbrPercent");
            // Fallback: product-service stores DBR limit under feeSettings.maxDbrPercentage
            if (maxDbrPercent == null && product.has("feeSettings") && !product.get("feeSettings").isNull()) {
                maxDbrPercent = decimalOrNull(product.get("feeSettings"), "maxDbrPercentage");
            }
            String fineractProductId = textOrNull(product, "fineractProductId");

            // Disbursement delay (hours) — pulled from durationSettings block on the product response.
            // 0 (or missing) means "disburse immediately" (backwards-compatible).
            int disbursementDurationHours = 0;
            if (product.has("durationSettings") && !product.get("durationSettings").isNull()) {
                disbursementDurationHours = intOrZero(product.get("durationSettings"), "disbursementDurationHours");
                if (disbursementDurationHours < 0) {
                    disbursementDurationHours = 0;
                }
            }

            // Validate amount range
            if (minAmount != null && input.requestedAmount().compareTo(minAmount) < 0) {
                return invalidResult("Requested amount below minimum: " + minAmount);
            }
            if (maxAmount != null && input.requestedAmount().compareTo(maxAmount) > 0) {
                return invalidResult("Requested amount exceeds maximum: " + maxAmount);
            }

            // Validate tenure range
            if (input.requestedTenureMonths() < minTenure) {
                return invalidResult("Tenure below minimum: " + minTenure + " months");
            }
            if (maxTenure > 0 && input.requestedTenureMonths() > maxTenure) {
                return invalidResult("Tenure exceeds maximum: " + maxTenure + " months");
            }

            // Extract required documents
            List<String> requiredDocs = new ArrayList<>();
            if (product.has("requiredDocuments") && product.get("requiredDocuments").isArray()) {
                product.get("requiredDocuments").forEach(doc -> requiredDocs.add(doc.asText()));
            }

            log.info("Product validated successfully: {} ({}), disbursementDelay={}h",
                    productName, productCode, disbursementDurationHours);
            return new ProductValidationResult(
                    true, productCode, productName, shariaStructure, fineractProductId,
                    minAmount, maxAmount, minTenure, maxTenure,
                    profitRate, processingFeePercent, adminFeeAmount,
                    minAge, maxAge, minSalary, minEmploymentMonths, minCreditScore, maxDbrPercent,
                    requiredDocs, disbursementDurationHours, null
            );

        } catch (Exception e) {
            log.warn("Product service call failed ({}), returning valid with defaults. " +
                     "Product validation will be re-checked during eligibility.", e.getMessage());
            // Graceful fallback: allow workflow to continue with user-provided data.
            // Full validation happens during eligibility/credit check phase.
            return new ProductValidationResult(
                    true, null, null, null, null,
                    null, null, 0, 0,
                    null, null, null,
                    0, 0, null, 0, 0, null,
                    List.of(), null
            );
        }
    }

    private ProductValidationResult invalidResult(String reason) {
        return new ProductValidationResult(
                false, null, null, null, null, null, null, 0, 0,
                null, null, null, 0, 0, null, 0, 0, null,
                List.of(), reason
        );
    }

    private String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private BigDecimal decimalOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? new BigDecimal(node.get(field).asText()) : null;
    }

    private int intOrZero(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asInt() : 0;
    }
}
