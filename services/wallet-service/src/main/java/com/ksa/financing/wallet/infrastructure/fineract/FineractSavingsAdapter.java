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

import java.math.BigDecimal;
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

    @Override
    public SavingsAccountInfo getAccountInfo(Long savingsId) {
        String url = config.getBaseUrl() + "/savingsaccounts/" + savingsId;
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});
        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new RestClientException("Fineract savings " + savingsId + " not found");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) body.get("summary");
        @SuppressWarnings("unchecked")
        Map<String, Object> status = (Map<String, Object>) body.get("status");
        @SuppressWarnings("unchecked")
        Map<String, Object> currency = (Map<String, Object>) body.get("currency");

        return new SavingsAccountInfo(
                savingsId,
                toLong(body.get("clientId")),
                (String) body.get("externalId"),
                status != null ? (String) status.get("value") : null,
                toBigDecimal(summary != null ? summary.get("accountBalance") : null),
                toBigDecimal(summary != null ? summary.get("availableBalance") : null),
                currency != null ? (String) currency.get("code") : null);
    }

    @Override
    public Long deposit(Long savingsId, BigDecimal amount, String externalReference) {
        String url = config.getBaseUrl() + "/savingsaccounts/" + savingsId + "/transactions?command=deposit";
        return postSavingsTransaction(url, amount, externalReference, "deposit savingsId=" + savingsId);
    }

    @Override
    public Long withdraw(Long savingsId, BigDecimal amount, String externalReference) {
        String url = config.getBaseUrl() + "/savingsaccounts/" + savingsId + "/transactions?command=withdrawal";
        return postSavingsTransaction(url, amount, externalReference, "withdraw savingsId=" + savingsId);
    }

    @Override
    public Long transferBetweenSavings(
            Long fromClientId, Long fromSavingsId,
            Long toClientId,   Long toSavingsId,
            BigDecimal amount, String description) {

        String url = config.getBaseUrl() + "/accounttransfers";
        String today = LocalDate.now().format(FINERACT_DATE);

        Map<String, Object> request = new HashMap<>();
        request.put("fromOfficeId", 1);
        request.put("fromClientId", fromClientId);
        request.put("fromAccountType", 2);          // 2 = savings
        request.put("fromAccountId", fromSavingsId);
        request.put("toOfficeId", 1);
        request.put("toClientId", toClientId);
        request.put("toAccountType", 2);
        request.put("toAccountId", toSavingsId);
        request.put("transferAmount", amount);
        request.put("transferDate", today);
        request.put("transferDescription", description != null ? description : "P2P");
        request.put("locale", "en");
        request.put("dateFormat", "dd MMMM yyyy");

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        Map<String, Object> body = response.getBody();
        Long resourceId = body != null ? toLong(body.get("resourceId")) : null;
        log.info("Fineract transfer OK fromSavings={} toSavings={} amount={} resourceId={}",
                fromSavingsId, toSavingsId, amount, resourceId);
        return resourceId;
    }

    private Long postSavingsTransaction(String url, BigDecimal amount, String externalRef, String op) {
        String today = LocalDate.now().format(FINERACT_DATE);
        Map<String, Object> request = new HashMap<>();
        request.put("locale", "en");
        request.put("dateFormat", "dd MMMM yyyy");
        request.put("transactionDate", today);
        request.put("transactionAmount", amount);
        request.put("paymentTypeId", 1);
        if (externalRef != null) {
            request.put("note", externalRef);
        }

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        Map<String, Object> body = response.getBody();
        Long txnId = body != null ? toLong(body.get("resourceId")) : null;
        log.info("Fineract {} OK amount={} txnId={} ref={}", op, amount, txnId, externalRef);
        return txnId;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof Number n) return new BigDecimal(n.toString());
        try { return new BigDecimal(o.toString()); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    @Override
    @SuppressWarnings("unchecked")
    public java.util.List<SavingsTransaction> getTransactions(Long savingsId) {
        String url = config.getBaseUrl() + "/savingsaccounts/" + savingsId + "?associations=transactions";
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

        Map<String, Object> body = response.getBody();
        if (body == null) return java.util.Collections.emptyList();

        Object txsRaw = body.get("transactions");
        if (!(txsRaw instanceof java.util.List)) return java.util.Collections.emptyList();

        java.util.List<Map<String, Object>> txs = (java.util.List<Map<String, Object>>) txsRaw;
        java.util.List<SavingsTransaction> result = new java.util.ArrayList<>(txs.size());
        for (Map<String, Object> t : txs) {
            Map<String, Object> typeMap = (Map<String, Object>) t.getOrDefault("transactionType", java.util.Collections.emptyMap());
            Object dateObj = t.get("date");
            String dateStr = null;
            if (dateObj instanceof java.util.List<?> parts && parts.size() >= 3) {
                dateStr = String.format("%04d-%02d-%02d",
                        ((Number) parts.get(0)).intValue(),
                        ((Number) parts.get(1)).intValue(),
                        ((Number) parts.get(2)).intValue());
            } else if (dateObj != null) {
                dateStr = dateObj.toString();
            }

            Object pd = t.get("paymentDetailData");
            String pdStr = pd != null ? pd.toString() : null;

            result.add(new SavingsTransaction(
                    toLong(t.get("id")),
                    (String) typeMap.get("value"),
                    toBigDecimal(t.get("amount")),
                    toBigDecimal(t.get("runningBalance")),
                    dateStr,
                    Boolean.TRUE.equals(t.get("reversed")),
                    pdStr));
        }
        // Newest first (Fineract returns chronological — reverse)
        java.util.Collections.reverse(result);
        return result;
    }
}
