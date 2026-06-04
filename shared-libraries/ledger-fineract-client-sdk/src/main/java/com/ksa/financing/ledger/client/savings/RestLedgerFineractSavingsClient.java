package com.ksa.financing.ledger.client.savings;

import com.ksa.financing.ledger.client.config.LedgerClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * REST implementation of {@link LedgerFineractSavingsClient}.
 * <p>
 * Calls ledger-service proxy endpoints — never Fineract directly.
 * Auto-selects between {@code /api/v1/fineract-proxy/savings/*} (JWT) and
 * {@code /internal/fineract-proxy/savings/*} (no JWT) based on
 * {@link LedgerClientProperties#isUseAuthenticatedEndpoints()}.
 */
@Slf4j
@RequiredArgsConstructor
public class RestLedgerFineractSavingsClient implements LedgerFineractSavingsClient {

    private static final DateTimeFormatter FINERACT_DATE = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final String LOCALE = "en";
    private static final String DATE_FORMAT = "dd MMMM yyyy";

    private final RestTemplate restTemplate;
    private final LedgerClientProperties props;

    private String basePath() {
        return props.isUseAuthenticatedEndpoints()
                ? "/api/v1/fineract-proxy/savings"
                : "/internal/fineract-proxy/savings";
    }

    @Override
    public Long lookupClientByExternalId(UUID tenantId, String externalId) {
        Map<String, Object> resp = exchange(HttpMethod.GET, tenantId, null,
                "/clients/by-external/" + externalId, null);
        return extractClientIdFromLookup(resp);
    }

    @Override
    public Long createClient(UUID tenantId, String externalId, String displayName,
                             Integer officeId, String idempotencyKey) {
        String today = LocalDate.now().format(FINERACT_DATE);
        String firstName = "Customer";
        String lastName = externalId.length() > 8
                ? externalId.substring(externalId.length() - 8) : externalId;
        if (displayName != null && !displayName.isBlank()) {
            String[] parts = displayName.trim().split("\\s+", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1]
                    : externalId.substring(Math.max(0, externalId.length() - 8));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("officeId", officeId != null ? officeId : 1);
        body.put("firstname", firstName);
        body.put("lastname", lastName);
        body.put("externalId", externalId);
        body.put("active", true);
        body.put("activationDate", today);
        body.put("locale", LOCALE);
        body.put("dateFormat", DATE_FORMAT);
        body.put("legalFormId", 1);

        Map<String, Object> resp = exchange(HttpMethod.POST, tenantId, idempotencyKey,
                "/clients", body);
        return extractLong(resp, "clientId", "resourceId");
    }

    @Override
    public Long createSavings(UUID tenantId, Long fineractClientId, String walletNumber,
                              Integer savingsProductId, String idempotencyKey) {
        String today = LocalDate.now().format(FINERACT_DATE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientId", fineractClientId);
        body.put("productId", savingsProductId != null ? savingsProductId : 1);
        body.put("locale", LOCALE);
        body.put("dateFormat", DATE_FORMAT);
        body.put("submittedOnDate", today);
        body.put("externalId", walletNumber);
        body.put("nominalAnnualInterestRate", 0);
        body.put("interestCompoundingPeriodType", 1);
        body.put("interestPostingPeriodType", 4);
        body.put("interestCalculationType", 1);
        body.put("interestCalculationDaysInYearType", 365);
        body.put("withdrawalFeeForTransfers", false);
        body.put("allowOverdraft", false);
        body.put("enforceMinRequiredBalance", false);

        Map<String, Object> resp = exchange(HttpMethod.POST, tenantId, idempotencyKey,
                "/accounts", body);
        return extractLong(resp, "savingsId", "resourceId");
    }

    @Override
    public void approveSavings(UUID tenantId, Long savingsId, String idempotencyKey) {
        Map<String, Object> body = dateOnlyBody("approvedOnDate");
        exchange(HttpMethod.POST, tenantId, idempotencyKey,
                "/accounts/" + savingsId + "/approve", body);
    }

    @Override
    public void activateSavings(UUID tenantId, Long savingsId, String idempotencyKey) {
        Map<String, Object> body = dateOnlyBody("activatedOnDate");
        exchange(HttpMethod.POST, tenantId, idempotencyKey,
                "/accounts/" + savingsId + "/activate", body);
    }

    @Override
    public void deleteSavings(UUID tenantId, Long savingsId) {
        try {
            exchange(HttpMethod.DELETE, tenantId, null,
                    "/accounts/" + savingsId, null);
        } catch (Exception e) {
            log.warn("Compensation delete savingsId={} failed: {}", savingsId, e.getMessage());
        }
    }

    @Override
    public SavingsAccountInfo getAccountInfo(UUID tenantId, Long savingsId) {
        Map<String, Object> resp = exchange(HttpMethod.GET, tenantId, null,
                "/accounts/" + savingsId, null);
        if (resp == null) {
            throw new IllegalStateException("Fineract savings " + savingsId + " not found");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) resp.get("summary");
        @SuppressWarnings("unchecked")
        Map<String, Object> status = (Map<String, Object>) resp.get("status");
        @SuppressWarnings("unchecked")
        Map<String, Object> currency = (Map<String, Object>) resp.get("currency");

        return new SavingsAccountInfo(
                savingsId,
                toLong(resp.get("clientId")),
                stringValue(resp.get("externalId")),
                status != null ? stringValue(status.get("value")) : null,
                toBigDecimal(summary != null ? summary.get("accountBalance") : null),
                toBigDecimal(summary != null ? summary.get("availableBalance") : null),
                currency != null ? stringValue(currency.get("code")) : null);
    }

    @Override
    public Long deposit(UUID tenantId, Long savingsId, BigDecimal amount,
                        String externalReference, String idempotencyKey) {
        BigDecimal normalizedAmount = amount != null ? amount.setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        Map<String, Object> body = txBody(normalizedAmount, externalReference);
        Map<String, Object> resp = exchange(HttpMethod.POST, tenantId, idempotencyKey,
                "/accounts/" + savingsId + "/deposit", body);
        return extractLong(resp, "resourceId");
    }

    @Override
    public Long withdraw(UUID tenantId, Long savingsId, BigDecimal amount,
                         String externalReference, String idempotencyKey) {
        BigDecimal normalizedAmount = amount != null ? amount.setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        Map<String, Object> body = txBody(normalizedAmount, externalReference);
        Map<String, Object> resp = exchange(HttpMethod.POST, tenantId, idempotencyKey,
                "/accounts/" + savingsId + "/withdraw", body);
        return extractLong(resp, "resourceId");
    }

    @Override
    public Long holdAmount(UUID tenantId, Long savingsId, BigDecimal amount,
                           String externalReference, String idempotencyKey) {
        BigDecimal normalizedAmount = amount != null ? amount.setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        // Fineract holdAmount supports only: locale, dateFormat, transactionDate, transactionAmount
        // (+ optional reasonForBlock). 'note' is rejected ("parameter note is not supported").
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locale", LOCALE);
        body.put("dateFormat", DATE_FORMAT);
        body.put("transactionDate", LocalDate.now().format(FINERACT_DATE));
        body.put("transactionAmount", normalizedAmount);
        Map<String, Object> resp = exchange(HttpMethod.POST, tenantId, idempotencyKey,
                "/accounts/" + savingsId + "/hold-amount", body);
        return extractLong(resp, "resourceId", "subResourceId");
    }

    @Override
    public void releaseHold(UUID tenantId, Long savingsId, Long holdTransactionId, String idempotencyKey) {
        exchange(HttpMethod.POST, tenantId, idempotencyKey,
                "/accounts/" + savingsId + "/transactions/" + holdTransactionId + "/release-amount",
                new LinkedHashMap<>());
    }

    @Override
    public Long transferBetweenSavings(UUID tenantId,
                                       Long fromOfficeId, Long fromClientId, Long fromSavingsId,
                                       Long toOfficeId, Long toClientId, Long toSavingsId,
                                       BigDecimal amount, String description, String idempotencyKey) {
        String today = LocalDate.now().format(FINERACT_DATE);
        BigDecimal normalizedAmount = amount != null ? amount.setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fromOfficeId", fromOfficeId != null ? fromOfficeId : 1);
        body.put("fromClientId", fromClientId);
        body.put("fromAccountType", 2);
        body.put("fromAccountId", fromSavingsId);
        body.put("toOfficeId", toOfficeId != null ? toOfficeId : 1);
        body.put("toClientId", toClientId);
        body.put("toAccountType", 2);
        body.put("toAccountId", toSavingsId);
        body.put("transferAmount", normalizedAmount);
        body.put("transferDate", today);
        body.put("transferDescription", description != null ? description : "P2P");
        body.put("locale", LOCALE);
        body.put("dateFormat", DATE_FORMAT);

        Map<String, Object> resp = exchange(HttpMethod.POST, tenantId, idempotencyKey,
                "/transfers", body);
        return extractLong(resp, "resourceId");
    }

    @Override
    public List<SavingsTransaction> getTransactions(UUID tenantId, Long savingsId) {
        Map<String, Object> resp = exchange(HttpMethod.GET, tenantId, null,
                "/accounts/" + savingsId + "/transactions", null);
        if (resp == null) return Collections.emptyList();
        Object txsRaw = resp.get("transactions");
        if (!(txsRaw instanceof List<?> rawList)) return Collections.emptyList();

        List<SavingsTransaction> result = new ArrayList<>(rawList.size());
        for (Object item : rawList) {
            if (!(item instanceof Map<?, ?> tMap)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> t = (Map<String, Object>) tMap;

            @SuppressWarnings("unchecked")
            Map<String, Object> typeMap = t.get("transactionType") instanceof Map<?, ?>
                    ? (Map<String, Object>) t.get("transactionType")
                    : Collections.emptyMap();

            String dateStr = formatFineractDate(t.get("date"));
            String pdStr = t.get("paymentDetailData") != null
                    ? t.get("paymentDetailData").toString() : null;

            result.add(new SavingsTransaction(
                    toLong(t.get("id")),
                    stringValue(typeMap.get("value")),
                    toBigDecimal(t.get("amount")),
                    toBigDecimal(t.get("runningBalance")),
                    dateStr,
                    Boolean.TRUE.equals(t.get("reversed")),
                    pdStr));
        }
        Collections.reverse(result);
        return result;
    }

    // ── HTTP plumbing ────────────────────────────────────────────────────────

    private Map<String, Object> exchange(HttpMethod method, UUID tenantId, String idempotencyKey,
                                         String relativePath, Map<String, Object> body) {
        String url = props.getBaseUrl() + basePath() + relativePath;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (tenantId != null) headers.set("X-Tenant-Id", tenantId.toString());
        if (props.getCallerService() != null) headers.set("X-Caller-Service", props.getCallerService());
        if (idempotencyKey != null) headers.set("X-Idempotency-Key", idempotencyKey);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, method, new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<>() {});
            return unwrapEnvelope(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("Ledger Fineract proxy {} {} -> {} : {}",
                    method, relativePath, e.getStatusCode(), e.getResponseBodyAsString());
            throw new LedgerProxyException(e.getStatusCode().value(),
                    "Ledger Fineract proxy rejected " + relativePath + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Ledger Fineract proxy {} {} failed: {}", method, relativePath, e.getMessage(), e);
            throw new LedgerProxyException(503,
                    "Ledger-service unreachable for " + relativePath + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrapEnvelope(Map<String, Object> body) {
        if (body == null) return null;
        Object data = body.get("data");
        if (data instanceof Map<?, ?> dataMap
                && body.containsKey("message")
                && body.containsKey("timestamp")) {
            return (Map<String, Object>) dataMap;
        }
        return body;
    }

    private Map<String, Object> dateOnlyBody(String dateField) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(dateField, LocalDate.now().format(FINERACT_DATE));
        body.put("locale", LOCALE);
        body.put("dateFormat", DATE_FORMAT);
        return body;
    }

    private Map<String, Object> txBody(BigDecimal amount, String externalRef) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locale", LOCALE);
        body.put("dateFormat", DATE_FORMAT);
        body.put("transactionDate", LocalDate.now().format(FINERACT_DATE));
        body.put("transactionAmount", amount);
        body.put("paymentTypeId", 1);
        if (externalRef != null) body.put("note", externalRef);
        return body;
    }

    private static Long extractLong(Map<String, Object> resp, String... keys) {
        if (resp == null) return null;
        for (String k : keys) {
            Object v = resp.get(k);
            Long n = toLong(v);
            if (n != null) return n;
        }
        return null;
    }

    private static Long extractClientIdFromLookup(Map<String, Object> resp) {
        if (resp == null) return null;
        Object pageItems = resp.get("pageItems");
        if (pageItems instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> m) return toLong(m.get("id"));
        }
        return toLong(resp.get("id"));
    }

    private static String formatFineractDate(Object dateObj) {
        if (dateObj instanceof List<?> parts && parts.size() >= 3) {
            return String.format("%04d-%02d-%02d",
                    ((Number) parts.get(0)).intValue(),
                    ((Number) parts.get(1)).intValue(),
                    ((Number) parts.get(2)).intValue());
        }
        return dateObj != null ? dateObj.toString() : null;
    }

    private static String stringValue(Object o) {
        return o == null ? null : o.toString();
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
}
