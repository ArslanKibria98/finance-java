package com.ksa.financing.onboarding.shared.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST client for posting onboarding documents (ID / Passport) to the
 * document-service for encrypted MinIO storage. Best-effort: failure is logged
 * but never rolls back the onboarding flow (Facia already verified the doc and
 * the workflow can proceed without the blob copy being persisted).
 */
@Component
public class DocumentStorageClient {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageClient.class);

    private final RestTemplate restTemplate;
    private final String documentServiceUrl;

    public DocumentStorageClient(RestTemplate restTemplate,
                                 @Value("${app.services.document-service-url:http://document-service:8094}") String documentServiceUrl) {
        this.restTemplate = restTemplate;
        this.documentServiceUrl = documentServiceUrl == null ? null : documentServiceUrl.replaceAll("/+$", "");
    }

    public StoreResult store(StoreInput input) {
        if (documentServiceUrl == null || documentServiceUrl.isBlank()) {
            log.warn("document-service URL not configured — skipping store");
            return new StoreResult(false, null, "URL_NOT_CONFIGURED");
        }
        String url = documentServiceUrl + "/internal/documents";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenantId", input.tenantId());
        if (input.customerId() != null) body.put("customerId", input.customerId());
        if (input.workflowId() != null) body.put("workflowId", input.workflowId());
        body.put("kind", input.kind());
        body.put("sourceFlow", input.sourceFlow());
        if (input.documentNumber() != null) body.put("documentNumber", input.documentNumber());
        if (input.faciaReferenceId() != null) body.put("faciaReferenceId", input.faciaReferenceId());
        body.put("contentType", input.contentType() != null ? input.contentType() : "image/jpeg");
        body.put("base64Image", input.base64Image());
        if (input.idempotencyKey() != null) body.put("idempotencyKey", input.idempotencyKey());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
            Map<String, Object> envelope = resp.getBody();
            Map<String, Object> data = (envelope != null && envelope.get("data") instanceof Map<?, ?> m)
                    ? (Map<String, Object>) m : envelope;
            UUID documentId = data != null && data.get("documentId") != null
                    ? UUID.fromString(data.get("documentId").toString()) : null;
            log.info("document-service stored doc: id={} kind={} flow={} workflowId={}",
                    documentId, input.kind(), input.sourceFlow(), input.workflowId());
            return new StoreResult(true, documentId, "STORED");
        } catch (Exception e) {
            log.error("document-service store FAILED (non-fatal): workflowId={} kind={} err={}",
                    input.workflowId(), input.kind(), e.getMessage());
            return new StoreResult(false, null, e.getMessage());
        }
    }

    public record StoreInput(
            UUID tenantId,
            UUID customerId,
            String workflowId,
            String kind,
            String sourceFlow,
            String documentNumber,
            String faciaReferenceId,
            String contentType,
            String base64Image,
            String idempotencyKey
    ) {}

    public record StoreResult(boolean ok, UUID documentId, String reason) {}
}
