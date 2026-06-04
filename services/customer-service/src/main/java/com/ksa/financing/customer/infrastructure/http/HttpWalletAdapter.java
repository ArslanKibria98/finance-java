package com.ksa.financing.customer.infrastructure.http;

import com.ksa.financing.customer.domain.port.out.WalletPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * HTTP adapter for communicating with the Wallet Service.
 * Fetches wallet IBAN for customer profile API.
 */
@Component
@Slf4j
public class HttpWalletAdapter implements WalletPort {

    private final RestTemplate restTemplate;

    @Value("${wallet.service.url:http://wallet-service:8092}")
    private String walletServiceBaseUrl;

    public HttpWalletAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Optional<String> getIbanByCustomerId(UUID tenantId, UUID customerId) {
        return getWalletInfoByCustomerId(tenantId, customerId).map(WalletInfo::iban);
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "getWalletInfoFallback")
    public Optional<WalletInfo> getWalletInfoByCustomerId(UUID tenantId, UUID customerId) {
        log.debug("Fetching wallet info for customer: {} tenant: {}", customerId, tenantId);

        String url = walletServiceBaseUrl + "/internal/wallets/by-customer/" + customerId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", tenantId.toString());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                // Unwrap ApiResponse wrapper if present: {"data":{...},"message":"success"}
                if (body.containsKey("data") && body.get("data") instanceof Map) {
                    body = (Map) body.get("data");
                }
                return Optional.of(new WalletInfo(
                        str(body.get("iban")),
                        str(body.get("accountNumber")),
                        str(body.get("walletNumber"))));
            }
            return Optional.empty();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.debug("No wallet found for customer: {}", customerId);
            return Optional.empty();
        }
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }

    @SuppressWarnings("unused")
    private Optional<WalletInfo> getWalletInfoFallback(UUID tenantId, UUID customerId, Throwable t) {
        log.warn("Wallet service unavailable for customer: {} — returning empty wallet info: {}", customerId, t.getMessage());
        return Optional.empty();
    }
}
