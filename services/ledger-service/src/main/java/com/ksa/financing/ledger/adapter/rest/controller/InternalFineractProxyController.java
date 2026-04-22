package com.ksa.financing.ledger.adapter.rest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

/**
 * Internal Fineract proxy — no JWT required.
 * Used by other services (product-service Temporal activities) to route
 * Fineract calls through ledger-service as the single Fineract bridge.
 *
 * All paths under /internal/** are permit-all in SecurityConfig.
 */
@Slf4j
@RestController
@RequestMapping("/internal/fineract-proxy")
@Tag(name = "Internal Fineract Proxy", description = "Service-to-service Fineract operations — no JWT required")
public class InternalFineractProxyController {

    private final RestTemplate fineractRestTemplate;
    private final ObjectMapper objectMapper;
    private final String fineractBaseUrl;
    private final String fineractTenantId;
    private final String fineractUsername;
    private final String fineractPassword;

    public InternalFineractProxyController(
            RestTemplate fineractRestTemplate,
            ObjectMapper objectMapper,
            @Value("${ksa.fineract.base-url:https://localhost:8443/fineract-provider/api/v1}") String fineractBaseUrl,
            @Value("${ksa.fineract.tenant-id:default}") String fineractTenantId,
            @Value("${ksa.fineract.username:#{null}}") String fineractUsername,
            @Value("${ksa.fineract.password:#{null}}") String fineractPassword) {
        this.fineractRestTemplate = fineractRestTemplate;
        this.objectMapper = objectMapper;
        this.fineractBaseUrl = fineractBaseUrl;
        this.fineractTenantId = fineractTenantId;
        this.fineractUsername = fineractUsername;
        this.fineractPassword = fineractPassword;
    }

    /**
     * Creates a loan product in Fineract.
     * Called by product-service Temporal activities instead of calling Fineract directly.
     *
     * Request body: Fineract loanproducts payload (same structure as Fineract API).
     * Response: { "resourceId": <fineractProductId> }
     */
    @PostMapping("/loan-products")
    @Operation(summary = "Create loan product in Fineract (internal, no auth)")
    public ResponseEntity<JsonNode> createLoanProduct(@RequestBody Map<String, Object> requestBody) {
        log.info("Internal Fineract proxy: creating loan product name={}", requestBody.get("name"));

        try {
            HttpHeaders headers = buildFineractHeaders();
            var response = fineractRestTemplate.exchange(
                    fineractBaseUrl + "/loanproducts",
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers),
                    String.class
            );

            JsonNode result = objectMapper.readTree(response.getBody());
            log.info("Fineract loan product created: resourceId={}", result.has("resourceId") ? result.get("resourceId") : "N/A");
            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (HttpStatusCodeException e) {
            log.error("Fineract rejected loan product creation: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(objectMapper.createObjectNode()
                            .put("error", e.getResponseBodyAsString())
                            .put("fineractStatus", e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Internal Fineract proxy: loan product creation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(objectMapper.createObjectNode().put("error", e.getMessage()));
        }
    }

    private HttpHeaders buildFineractHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Fineract-Platform-TenantId", fineractTenantId);
        if (fineractUsername != null && fineractPassword != null) {
            String encoded = Base64.getEncoder()
                    .encodeToString((fineractUsername + ":" + fineractPassword).getBytes());
            headers.set("Authorization", "Basic " + encoded);
        }
        return headers;
    }
}
