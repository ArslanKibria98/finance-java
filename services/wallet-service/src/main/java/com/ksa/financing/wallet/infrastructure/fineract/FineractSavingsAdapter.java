package com.ksa.financing.wallet.infrastructure.fineract;

import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fineract adapter that creates and activates savings accounts
 * to back each wallet with a core banking ledger entry.
 *
 * Implements FineractSavingsPort with step-wise methods
 * for Temporal activity retry and saga compensation.
 */
@Component
public class FineractSavingsAdapter implements FineractSavingsPort {

    private static final Logger log = LoggerFactory.getLogger(FineractSavingsAdapter.class);
    private static final DateTimeFormatter FINERACT_DATE = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    private final RestTemplate restTemplate;
    private final FineractWalletConfig config;

    public FineractSavingsAdapter(
            @Qualifier("fineractWalletRestTemplate") RestTemplate restTemplate,
            FineractWalletConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    @Override
    public Long lookupClientByExternalId(String externalId) {
        String url = config.getBaseUrl() + "/clients?externalId=" + externalId;
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            Map<String, Object> body = response.getBody();
            if (body == null) return null;

            Object pageItems = body.get("pageItems");
            if (pageItems instanceof List<?> list && !list.isEmpty()) {
                Object first = list.getFirst();
                if (first instanceof Map<?, ?> clientMap) {
                    Object id = clientMap.get("id");
                    return id instanceof Number num ? num.longValue() : null;
                }
            }
            return null;
        } catch (RestClientException e) {
            log.warn("Failed to lookup Fineract client externalId={}: {}", externalId, e.getMessage());
            throw e;
        }
    }

    @Override
    public Long createClient(String externalId, String displayName) {
        String url = config.getBaseUrl() + "/clients";
        String today = LocalDate.now().format(FINERACT_DATE);

        // Split displayName into first/last or use defaults
        String firstName = "Customer";
        String lastName = externalId.length() > 8
                ? externalId.substring(externalId.length() - 8)
                : externalId;
        if (displayName != null && !displayName.isBlank()) {
            String[] parts = displayName.trim().split("\\s+", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : externalId.substring(externalId.length() - 8);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("officeId", 1);
        request.put("firstname", firstName);
        request.put("lastname", lastName);
        request.put("externalId", externalId);
        request.put("active", true);
        request.put("activationDate", today);
        request.put("locale", "en");
        request.put("dateFormat", "dd MMMM yyyy");
        request.put("legalFormId", 1);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        Map<String, Object> body = response.getBody();
        if (body != null) {
            Object clientId = body.get("clientId");
            if (clientId == null) clientId = body.get("resourceId");
            if (clientId instanceof Number num) {
                log.info("Fineract client created: clientId={} externalId={}", num.longValue(), externalId);
                return num.longValue();
            }
        }
        throw new RestClientException("Unexpected response creating Fineract client for externalId=" + externalId);
    }

    @Override
    public Long createSavings(Long fineractClientId, String walletNumber) {
        String url = config.getBaseUrl() + "/savingsaccounts";
        String today = LocalDate.now().format(FINERACT_DATE);

        Map<String, Object> request = new HashMap<>();
        request.put("clientId", fineractClientId);
        request.put("productId", config.getSavingsProductId());
        request.put("locale", "en");
        request.put("dateFormat", "dd MMMM yyyy");
        request.put("submittedOnDate", today);
        request.put("externalId", walletNumber);
        request.put("nominalAnnualInterestRate", 0);
        request.put("interestCompoundingPeriodType", 1);
        request.put("interestPostingPeriodType", 4);
        request.put("interestCalculationType", 1);
        request.put("interestCalculationDaysInYearType", 365);
        request.put("withdrawalFeeForTransfers", false);
        request.put("allowOverdraft", false);
        request.put("enforceMinRequiredBalance", false);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        Map<String, Object> body = response.getBody();
        if (body != null) {
            Object savingsId = body.get("savingsId");
            if (savingsId == null) savingsId = body.get("resourceId");
            if (savingsId instanceof Number num) {
                log.info("Fineract savings created: savingsId={} wallet={}", num.longValue(), walletNumber);
                return num.longValue();
            }
        }
        throw new RestClientException("Unexpected response creating savings for wallet=" + walletNumber);
    }

    @Override
    public void approveSavings(Long savingsId) {
        String url = config.getBaseUrl() + "/savingsaccounts/" + savingsId + "?command=approve";
        String today = LocalDate.now().format(FINERACT_DATE);

        Map<String, Object> request = new HashMap<>();
        request.put("approvedOnDate", today);
        request.put("locale", "en");
        request.put("dateFormat", "dd MMMM yyyy");

        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(request),
                new ParameterizedTypeReference<Map<String, Object>>() {});
        log.debug("Fineract savings approved: savingsId={}", savingsId);
    }

    @Override
    public void activateSavings(Long savingsId) {
        String url = config.getBaseUrl() + "/savingsaccounts/" + savingsId + "?command=activate";
        String today = LocalDate.now().format(FINERACT_DATE);

        Map<String, Object> request = new HashMap<>();
        request.put("activatedOnDate", today);
        request.put("locale", "en");
        request.put("dateFormat", "dd MMMM yyyy");

        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(request),
                new ParameterizedTypeReference<Map<String, Object>>() {});
        log.debug("Fineract savings activated: savingsId={}", savingsId);
    }

    @Override
    public void deleteSavings(Long savingsId) {
        String url = config.getBaseUrl() + "/savingsaccounts/" + savingsId;
        try {
            restTemplate.delete(url);
            log.info("Fineract savings deleted (compensation): savingsId={}", savingsId);
        } catch (RestClientException e) {
            log.warn("Failed to delete Fineract savings {} during compensation: {}", savingsId, e.getMessage());
        }
    }
}
