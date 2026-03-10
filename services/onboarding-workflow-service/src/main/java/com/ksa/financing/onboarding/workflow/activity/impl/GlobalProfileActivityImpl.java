package com.ksa.financing.onboarding.workflow.activity.impl;

import com.ksa.financing.onboarding.workflow.activity.GlobalProfileActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Local activity implementation that calls Global Profile Service via HTTP.
 * <p>
 * This is NOT a Spring bean — it is instantiated manually and registered
 * with the Temporal Worker. Dependencies are provided via constructor injection.
 * <p>
 * GPS security is permitAll(), so no JWT is required for service-to-service calls.
 */
public class GlobalProfileActivityImpl implements GlobalProfileActivity {

    private static final Logger log = LoggerFactory.getLogger(GlobalProfileActivityImpl.class);

    private final RestTemplate restTemplate;
    private final String globalProfileUrl;

    public GlobalProfileActivityImpl(RestTemplate restTemplate, String globalProfileUrl) {
        this.restTemplate = restTemplate;
        this.globalProfileUrl = globalProfileUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GlobalProfileResult createGlobalProfile(GlobalProfileInput input) {
        log.info("Creating global profile via GPS for mobile: ****{}",
                input.mobileNumber() != null && input.mobileNumber().length() >= 4
                        ? input.mobileNumber().substring(input.mobileNumber().length() - 4)
                        : "****");

        try {
            String url = globalProfileUrl + "/api/v1/profiles";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            if (input.email() != null && !input.email().isBlank()) {
                requestBody.put("email", input.email());
            }
            requestBody.put("mobile", input.mobileNumber() != null ? input.mobileNumber() : "");
            requestBody.put("primaryCountryCode", input.countryCode() != null ? input.countryCode() : "SAU");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null) {
                // GPS wraps response in {"data": {...}} or returns flat
                Map<String, Object> data = body.containsKey("data")
                        ? (Map<String, Object>) body.get("data") : body;
                if (data != null && data.containsKey("globalUid")) {
                    String globalUid = data.get("globalUid").toString();
                    log.info("Global profile created with globalUid: {}", globalUid);
                    return new GlobalProfileResult(globalUid, true);
                }
            }

            log.warn("GPS returned response without globalUid: {}", body);
            return new GlobalProfileResult(null, false);

        } catch (Exception e) {
            log.error("Failed to create global profile via GPS: {}", e.getMessage());
            return new GlobalProfileResult(null, false);
        }
    }
}
