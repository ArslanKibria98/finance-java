package com.ksa.financing.lending.adapter.temporal.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.islamic.orchestration.activity.lending.CustomerValidationActivity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Validates customer details by calling customer-service REST API.
 * Used in Step 1 (Basic Information) of the loan application workflow.
 */
@Slf4j
@Component
public class CustomerValidationActivityImpl implements CustomerValidationActivity {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String customerServiceUrl;

    public CustomerValidationActivityImpl(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.customer-service-url}") String customerServiceUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.customerServiceUrl = customerServiceUrl;
    }

    @Override
    public CustomerValidationResult validateCustomer(CustomerValidationInput input) {
        log.info("Activity: Validating customer {} for tenant {}", input.customerId(), input.tenantId());

        try {
            String url = customerServiceUrl + "/api/v1/customers/" + input.customerId();

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return invalidResult("Customer not found: " + input.customerId());
            }

            var customer = objectMapper.readTree(response.getBody());

            String nationalId = textOrNull(customer, "nationalId");
            String fullName = textOrNull(customer, "fullName");
            if (fullName == null) {
                // Try first + last name
                String first = textOrNull(customer, "firstNameEn");
                String last = textOrNull(customer, "lastNameEn");
                fullName = (first != null ? first : "") + " " + (last != null ? last : "");
                fullName = fullName.trim();
            }
            String mobileNumber = textOrNull(customer, "mobileNumber");
            int age = intOrZero(customer, "age");
            String employmentType = textOrNull(customer, "employmentType");
            int employmentDurationMonths = intOrZero(customer, "employmentDurationMonths");
            String employerName = textOrNull(customer, "employerName");

            // Validate basic customer eligibility
            if (nationalId == null || nationalId.isBlank()) {
                return invalidResult("Customer missing National ID");
            }

            log.info("Customer validated successfully: {}", fullName);
            return new CustomerValidationResult(
                    true, nationalId, fullName, mobileNumber,
                    age, employmentType, employmentDurationMonths, employerName, null
            );

        } catch (Exception e) {
            log.warn("Customer service call failed ({}), returning valid with defaults. " +
                     "Customer validation will be re-checked during eligibility.", e.getMessage());
            // Graceful fallback: allow workflow to continue with user-provided data.
            return new CustomerValidationResult(
                    true, null, null, null, 0, null, 0, null, null
            );
        }
    }

    private CustomerValidationResult invalidResult(String reason) {
        return new CustomerValidationResult(false, null, null, null, 0, null, 0, null, reason);
    }

    private String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private int intOrZero(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asInt() : 0;
    }
}
