package com.ksa.financing.onboarding.adapter.rest.controller;

import com.ksa.financing.onboarding.application.dto.ReferenceDataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Proxies EDD reference data from customer-service for onboarding forms.
 * Upstream customer-service wraps responses in ApiResponse — this controller
 * extracts the inner data, then ApiResponseAdvice re-wraps in the standard format.
 */
@RestController
@RequestMapping("/api/v1/onboarding/reference-data")
@Tag(name = "EDD Reference Data", description = "Dropdown options for Enhanced Due Diligence forms during onboarding")
public class EddReferenceDataController {

    private static final Logger log = LoggerFactory.getLogger(EddReferenceDataController.class);

    private final RestTemplate restTemplate;

    @Value("${app.services.customer-service-url}")
    private String customerServiceUrl;

    @Value("${app.default-tenant-id:00000000-0000-0000-0000-000000000001}")
    private String defaultTenantId;

    public EddReferenceDataController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/source-of-wealth")
    @Operation(summary = "List active Source of Wealth options",
               description = "Returns active Source of Wealth dropdown options sorted by display order. "
                           + "Used to populate the EDD form during onboarding.")
    public ResponseEntity<List<ReferenceDataResponse>> listSourceOfWealth() {
        log.debug("Fetching active Source of Wealth options from customer-service");
        var url = customerServiceUrl + "/api/v1/reference-data/source-of-wealth/active";
        return fetchReferenceData(url, "Source of Wealth");
    }

    @GetMapping("/source-of-funds")
    @Operation(summary = "List active Source of Funds options",
               description = "Returns active Source of Funds dropdown options sorted by display order. "
                           + "Used to populate the EDD form during onboarding.")
    public ResponseEntity<List<ReferenceDataResponse>> listSourceOfFunds() {
        log.debug("Fetching active Source of Funds options from customer-service");
        var url = customerServiceUrl + "/api/v1/reference-data/source-of-funds/active";
        return fetchReferenceData(url, "Source of Funds");
    }

    @GetMapping("/net-worth-ranges")
    @Operation(summary = "List active Net Worth Range options",
               description = "Returns active Net Worth Range dropdown options sorted by display order. "
                           + "Used to populate the EDD form during onboarding.")
    public ResponseEntity<List<ReferenceDataResponse>> listNetWorthRanges() {
        log.debug("Fetching active Net Worth Range options from customer-service");
        var url = customerServiceUrl + "/api/v1/reference-data/net-worth-ranges/active";
        return fetchReferenceData(url, "Net Worth Ranges");
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<List<ReferenceDataResponse>> fetchReferenceData(String url, String dataType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", defaultTenantId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Customer-service wraps responses in ApiResponse{data, message, timestamp}
            // via ApiResponseAdvice. Deserialize as Map and extract the "data" field.
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            var body = response.getBody();
            if (body == null || !body.containsKey("data")) {
                log.warn("Unexpected response structure from customer-service for {}", dataType);
                return ResponseEntity.ok(Collections.emptyList());
            }

            var rawData = (List<Map<String, Object>>) body.get("data");
            List<ReferenceDataResponse> data = rawData.stream()
                    .map(this::toReferenceDataResponse)
                    .toList();

            log.debug("Received {} {} options from customer-service", data.size(), dataType);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Failed to fetch {} options from customer-service: {}", dataType, e.getMessage(), e);
            return ResponseEntity.status(503).body(Collections.emptyList());
        }
    }

    private ReferenceDataResponse toReferenceDataResponse(Map<String, Object> map) {
        return new ReferenceDataResponse(
                map.get("id") != null ? UUID.fromString(map.get("id").toString()) : null,
                (String) map.get("code"),
                (String) map.get("nameEn"),
                (String) map.get("nameAr"),
                (String) map.get("descriptionEn"),
                (String) map.get("descriptionAr"),
                Boolean.TRUE.equals(map.get("isActive")),
                map.get("displayOrder") != null ? ((Number) map.get("displayOrder")).intValue() : 0,
                map.get("createdAt") != null ? Instant.parse(map.get("createdAt").toString()) : null,
                map.get("updatedAt") != null ? Instant.parse(map.get("updatedAt").toString()) : null
        );
    }
}
