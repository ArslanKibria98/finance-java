package com.ksa.financing.identity.infrastructure.keycloak;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Idempotent bootstrap that ensures every realm client publishes the user
 * attributes needed by the {@code BlacklistGuardFilter} as JWT claims.
 *
 * <p>For each configured client, registers two
 * {@code oidc-usermodel-attribute-mapper} protocol mappers if absent:
 * <ul>
 *   <li>{@code mobile_number} → claim {@code mobile_number}</li>
 *   <li>{@code national_id}   → claim {@code national_id}</li>
 * </ul>
 *
 * <p>Without these mappers, Keycloak does not include user attributes in the
 * access token, so the per-request blacklist filter has nothing to match
 * against beyond {@code sub}.</p>
 *
 * <p>Runs once on {@link ApplicationReadyEvent}. Safe to run multiple times —
 * existing mappers are detected by name and skipped.</p>
 */
@Component
@Slf4j
public class KeycloakProtocolMapperBootstrap {

    private final RestTemplate restTemplate;

    @Value("${keycloak.internal-url:${keycloak.auth-server-url:http://keycloak:8080}}")
    private String keycloakBaseUrl;

    @Value("${keycloak.realm:CompanyRealm}")
    private String realm;

    @Value("${keycloak.bootstrap.master-admin-username:admin}")
    private String masterAdminUsername;

    @Value("${keycloak.bootstrap.master-admin-password:admin}")
    private String masterAdminPassword;

    @Value("${keycloak.bootstrap.target-clients:admin-dashboard,partner-dashboard,customer-mobile-app,react-frontend}")
    private List<String> targetClients;

    @Value("${keycloak.bootstrap.enabled:true}")
    private boolean enabled;

    public KeycloakProtocolMapperBootstrap(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!enabled) {
            log.info("Keycloak protocol mapper bootstrap disabled — skipping");
            return;
        }
        try {
            String adminToken = obtainMasterAdminToken();
            for (String clientId : targetClients) {
                ensureMappersOnClient(adminToken, clientId);
            }
            log.info("Keycloak protocol mapper bootstrap complete for realm={}", realm);
        } catch (Exception e) {
            log.warn("Keycloak protocol mapper bootstrap failed (continuing — JWT may lack mobile/NID claims): {}",
                    e.getMessage());
        }
    }

    private void ensureMappersOnClient(String adminToken, String clientId) {
        String internalId = resolveClientInternalId(adminToken, clientId);
        if (internalId == null) {
            log.warn("Keycloak client '{}' not found in realm '{}', skipping", clientId, realm);
            return;
        }
        List<Map<String, Object>> mappers = listMappers(adminToken, internalId);

        ensureMapper(adminToken, internalId, mappers, "mobile_number-mapper", "mobile_number", "mobile_number");
        ensureMapper(adminToken, internalId, mappers, "national_id-mapper", "national_id", "national_id");
    }

    private void ensureMapper(String adminToken, String clientInternalId,
                              List<Map<String, Object>> existingMappers,
                              String mapperName, String userAttribute, String claimName) {
        boolean exists = existingMappers.stream()
                .anyMatch(m -> mapperName.equals(m.get("name")));
        if (exists) {
            log.debug("Mapper '{}' already exists on client {} — skipping", mapperName, clientInternalId);
            return;
        }

        String url = keycloakBaseUrl + "/admin/realms/" + realm
                + "/clients/" + clientInternalId + "/protocol-mappers/models";

        Map<String, Object> body = new HashMap<>();
        body.put("name", mapperName);
        body.put("protocol", "openid-connect");
        body.put("protocolMapper", "oidc-usermodel-attribute-mapper");
        Map<String, String> config = new HashMap<>();
        config.put("user.attribute", userAttribute);
        config.put("claim.name", claimName);
        config.put("jsonType.label", "String");
        config.put("id.token.claim", "true");
        config.put("access.token.claim", "true");
        config.put("userinfo.token.claim", "true");
        config.put("multivalued", "false");
        body.put("config", config);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
            log.info("Created protocol mapper '{}' on client {} (claim '{}')", mapperName, clientInternalId, claimName);
        } catch (HttpClientErrorException.Conflict ignored) {
            log.debug("Mapper '{}' already exists (409) — race-tolerated", mapperName);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listMappers(String adminToken, String clientInternalId) {
        String url = keycloakBaseUrl + "/admin/realms/" + realm
                + "/clients/" + clientInternalId + "/protocol-mappers/models";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        ResponseEntity<List> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), List.class);
        return response.getBody() != null ? response.getBody() : List.of();
    }

    @SuppressWarnings("unchecked")
    private String resolveClientInternalId(String adminToken, String clientId) {
        String url = keycloakBaseUrl + "/admin/realms/" + realm + "/clients?clientId=" + clientId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        ResponseEntity<List> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), List.class);

        List<Map<String, Object>> body = response.getBody();
        if (body == null || body.isEmpty()) return null;
        return (String) body.get(0).get("id");
    }

    private String obtainMasterAdminToken() {
        String tokenUrl = keycloakBaseUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", masterAdminUsername);
        form.add("password", masterAdminPassword);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenUrl, new HttpEntity<>(form, headers), Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("access_token")) {
            throw new IllegalStateException("Could not obtain master admin token from Keycloak");
        }
        return (String) body.get("access_token");
    }
}
