package com.ksa.financing.lending.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Non-blocking fraud event publisher that sends events to fraud-service's
 * POST /api/v1/fraud/evaluate endpoint.
 *
 * All calls are fire-and-forget (@Async) with full exception swallowing.
 * If fraud-service is down, the lending flow continues unaffected.
 */
@Slf4j
@Component
public class FraudEventNotifier {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String fraudServiceUrl;

    public FraudEventNotifier(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.fraud-service-url:}") String fraudServiceUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.fraudServiceUrl = fraudServiceUrl;
    }

    /**
     * Send LOAN_APPLICATION fraud event when a new loan application is created.
     */
    @Async
    public void notifyLoanApplicationCreated(String tenantId, String customerId,
                                              String nationalIdHash, String applicationId,
                                              String productType, BigDecimal amount,
                                              String ipAddress, String deviceId) {
        var event = new HashMap<String, Object>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "LOAN_APPLICATION");
        event.put("customerId", customerId);
        event.put("nationalIdHash", safe(nationalIdHash));
        event.put("loanApplicationId", applicationId);
        event.put("loanProductType", safe(productType));
        event.put("transactionType", "LOAN_APPLICATION");
        event.put("transactionAmount", amount != null ? amount : BigDecimal.ZERO);
        event.put("currency", "SAR");
        event.put("ipAddress", safe(ipAddress));
        event.put("deviceId", safe(deviceId));
        event.put("eventTimestamp", LocalDateTime.now().toString());
        event.put("correlationId", applicationId);
        sendFraudEvent(tenantId, event);
    }

    /**
     * Send DISBURSEMENT fraud event when funds are about to be disbursed.
     */
    @Async
    public void notifyDisbursement(String tenantId, String customerId,
                                    String nationalIdHash, String applicationId,
                                    String loanId, BigDecimal amount,
                                    String disbursementIban) {
        var event = new HashMap<String, Object>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "DISBURSEMENT");
        event.put("customerId", safe(customerId));
        event.put("nationalIdHash", safe(nationalIdHash));
        event.put("loanApplicationId", safe(applicationId));
        event.put("transactionType", "DISBURSEMENT");
        event.put("transactionAmount", amount != null ? amount : BigDecimal.ZERO);
        event.put("currency", "SAR");
        event.put("disbursementIban", safe(disbursementIban));
        event.put("eventTimestamp", LocalDateTime.now().toString());
        event.put("correlationId", loanId != null ? loanId : applicationId);
        sendFraudEvent(tenantId, event);
    }

    private void sendFraudEvent(String tenantId, Map<String, Object> eventData) {
        if (fraudServiceUrl == null || fraudServiceUrl.isBlank()) {
            log.debug("Fraud service URL not configured, skipping fraud event notification");
            return;
        }

        try {
            String url = fraudServiceUrl + "/api/v1/fraud/evaluate";
            String body = objectMapper.writeValueAsString(eventData);

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId);
            headers.set("X-Caller-Service", "lending-service");

            var response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Fraud event sent successfully: type={}, appId={}",
                        eventData.get("eventType"), eventData.get("loanApplicationId"));
            } else {
                log.warn("Fraud event returned non-2xx: status={}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Fraud event notification failed (non-blocking): type={}, error={}",
                    eventData.get("eventType"), e.getMessage());
        }
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
