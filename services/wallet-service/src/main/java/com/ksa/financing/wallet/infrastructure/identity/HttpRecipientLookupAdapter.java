package com.ksa.financing.wallet.infrastructure.identity;

import com.ksa.financing.infra.audit.ApiAuditRestTemplateInterceptor;
import com.ksa.financing.wallet.domain.port.out.RecipientLookupPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class HttpRecipientLookupAdapter implements RecipientLookupPort {

    private final RestTemplate restTemplate;
    private final String identityBaseUrl;

    public HttpRecipientLookupAdapter(
            @Value("${identity.service.base-url:http://identity-service:8083}") String identityBaseUrl,
            @Value("${identity.service.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${identity.service.read-timeout-ms:5000}") int readTimeoutMs,
            ObjectProvider<ApiAuditRestTemplateInterceptor> auditInterceptor) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(factory);
        // Attach SDK audit interceptor so every outbound identity-service call is
        // captured into the api-audit Elasticsearch index with full request/response.
        ApiAuditRestTemplateInterceptor interceptor = auditInterceptor.getIfAvailable();
        if (interceptor != null) {
            this.restTemplate.getInterceptors().add(interceptor);
        }
        this.identityBaseUrl = identityBaseUrl;
    }

    @Override
    public Optional<UserLookup> lookupByMobile(String mobileNumber) {
        return lookup(Map.of("mobile", mobileNumber));
    }

    @Override
    public Optional<UserLookup> lookupByKeycloakUserId(UUID keycloakUserId) {
        return lookup(Map.of("keycloakId", keycloakUserId.toString()));
    }

    @Override
    public Optional<UserLookup> lookupByNationalId(String nationalId) {
        return lookup(Map.of("nid", nationalId));
    }

    @Override
    public Optional<UserLookup> lookupByCustomerId(UUID customerId) {
        return lookup(Map.of("customerId", customerId.toString()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Optional<UserLookup> lookup(Map<String, String> params) {
        UriComponentsBuilder b = UriComponentsBuilder
                .fromHttpUrl(identityBaseUrl)
                .path("/internal/users/lookup");
        
        params.forEach(b::queryParam);
        
        java.net.URI uri = b.build().encode().toUri();

        try {
            log.debug("Calling identity lookup: {}", uri);
            ResponseEntity<Map> resp = restTemplate.getForEntity(uri, Map.class);
            Map<String, Object> envelope = (Map<String, Object>) resp.getBody();
            if (envelope == null) return Optional.empty();
            // Identity-service wraps responses as { data: {...}, message, timestamp }
            Object inner = envelope.containsKey("data") ? envelope.get("data") : envelope;
            if (!(inner instanceof Map)) return Optional.empty();
            Map<String, Object> body = (Map<String, Object>) inner;
            Boolean found = (Boolean) body.get("found");
            if (!Boolean.TRUE.equals(found)) return Optional.empty();

            return Optional.of(new UserLookup(
                    parseUuid(body.get("keycloakUserId")),
                    parseUuid(body.get("customerId")),
                    parseUuid(body.get("tenantId")),
                    (String) body.get("name"),
                    (String) body.get("firstName"),
                    (String) body.get("lastName"),
                    (String) body.get("maskedMobile"),
                    (String) body.get("status"),
                    Boolean.TRUE.equals(body.get("enabled"))));
        } catch (Exception ex) {
            log.warn("Identity lookup failed url={} err={}", uri, ex.getMessage());
            return Optional.empty();
        }
    }

    private UUID parseUuid(Object v) {
        if (v == null) return null;
        try { return UUID.fromString(v.toString()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
