package com.ksa.financing.customer.infrastructure.http;

import com.ksa.financing.customer.domain.port.out.IdentityLinkPort;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class HttpIdentityLinkAdapter implements IdentityLinkPort {

    private final RestTemplate restTemplate;

    @Value("${identity.service.base-url:http://identity-service:8083}")
    private String identityBaseUrl;

    public HttpIdentityLinkAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Retry(name = "internal-rest")
    public void linkInternalCustomer(UUID keycloakUserId, UUID internalCustomerId) {
        if (keycloakUserId == null || internalCustomerId == null) {
            log.warn("Skipping identity link: keycloakUserId={} customerId={}", keycloakUserId, internalCustomerId);
            return;
        }
        String url = identityBaseUrl + "/internal/users/link-customer";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of(
                "keycloakUserId", keycloakUserId.toString(),
                "internalCustomerId", internalCustomerId.toString()
        );
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        if (resp.getStatusCode().isError() || resp.getBody() == null) {
            throw new IllegalStateException("Identity link failed: status=" + resp.getStatusCode());
        }
        Object data = resp.getBody().get("data");
        boolean linked = data instanceof Map && Boolean.TRUE.equals(((Map<?, ?>) data).get("linked"));
        if (!linked) {
            String reason = data instanceof Map ? String.valueOf(((Map<?, ?>) data).get("reason")) : "UNKNOWN";
            throw new IllegalStateException("Identity link rejected: reason=" + reason);
        }
        log.info("Identity link succeeded for keycloakUserId={} → customerId={}", keycloakUserId, internalCustomerId);
    }
}
