package com.ksa.financing.product.infrastructure.ledger;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP client for ledger-service.
 * Routes Fineract loan product operations through ledger-service
 * instead of calling Fineract directly from product-service.
 *
 * All calls use the internal endpoint (no JWT required).
 */
@Slf4j
@Component
public class LedgerServiceClient {

    private final RestTemplate restTemplate;
    private final String ledgerServiceUrl;

    public LedgerServiceClient(
            RestTemplate ledgerServiceRestTemplate,
            @Value("${ledger.service.url:${LEDGER_SERVICE_URL:http://localhost:8095}}") String ledgerServiceUrl) {
        this.restTemplate = ledgerServiceRestTemplate;
        this.ledgerServiceUrl = ledgerServiceUrl;
    }

    /**
     * Creates a loan product in Fineract via ledger-service.
     *
     * @param loanProductPayload Fineract loanproducts request body
     * @return Fineract resourceId of created product
     */
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest")
    public Long createLoanProduct(Map<String, Object> loanProductPayload) {
        String url = ledgerServiceUrl + "/internal/fineract-proxy/loan-products";
        log.info("Routing loan product creation through ledger-service: name={}", loanProductPayload.get("name"));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(loanProductPayload, headers),
                    JsonNode.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode body = response.getBody();

                // Unwrap SDK response envelope {"data": {...}, "message": ...} if present
                JsonNode payload = body.has("data") ? body.get("data") : body;

                if (payload.has("resourceId")) {
                    Long resourceId = payload.get("resourceId").asLong();
                    log.info("Loan product created via ledger-service: fineractResourceId={}", resourceId);
                    return resourceId;
                }
                if (payload.has("error")) {
                    throw new RuntimeException("Ledger-service Fineract error: " + payload.get("error").asText());
                }
            }

            throw new RuntimeException("Unexpected response from ledger-service: " + response.getStatusCode());

        } catch (HttpStatusCodeException e) {
            log.error("Ledger-service rejected loan product creation: status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Ledger-service error creating loan product: " + e.getResponseBodyAsString(), e);
        }
    }
}
