package com.ksa.financing.product.infrastructure.fineract;

import com.ksa.financing.lms.adapter.fineract.dto.FineractLoanProductRequest;
import com.ksa.financing.lms.adapter.fineract.dto.FineractLoanProductResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Lightweight Fineract REST client for product-service.
 * Only handles loan product CRUD operations — does not require
 * RedissonClient or IdempotencyStore unlike the full FineractClient.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductFineractClient {

    private final RestTemplate productFineractRestTemplate;

    @Value("${fineract.base-url:https://localhost:8443/fineract-provider/api/v1}")
    private String baseUrl;

    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractLoanProductResponse createLoanProduct(FineractLoanProductRequest request) {
        String url = baseUrl + "/loanproducts";
        log.debug("Creating loan product in Fineract: {}", request.getName());

        try {
            ResponseEntity<FineractLoanProductResponse> response =
                    productFineractRestTemplate.postForEntity(url, request, FineractLoanProductResponse.class);
            log.info("Loan product created successfully in Fineract: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to create loan product: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to create loan product in Fineract: " + e.getResponseBodyAsString(), e);
        }
    }
}
