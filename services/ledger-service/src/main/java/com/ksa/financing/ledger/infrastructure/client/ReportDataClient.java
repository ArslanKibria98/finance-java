package com.ksa.financing.ledger.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.pagination.PageQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Forwards caller JWT to collections-service / lending-service to fetch raw data
 * that feeds ledger-service's report services.
 */
@Component
@Slf4j
public class ReportDataClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String collectionsUrl;
    private final String lendingUrl;

    public ReportDataClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.collections-service-url}") String collectionsUrl,
            @Value("${app.services.lending-service-url}") String lendingUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.collectionsUrl = collectionsUrl;
        this.lendingUrl = lendingUrl;
    }

    public JsonNode fetchOverdueInstallments(LocalDate asOfDate, Integer minDaysPastDue, PageQuery query) {
        var builder = UriComponentsBuilder.fromHttpUrl(collectionsUrl)
                .pathSegment("api", "v1", "internal", "reports", "overdue-installments")
                .queryParam("asOfDate", asOfDate)
                .queryParamIfPresent("minDaysPastDue",
                        java.util.Optional.ofNullable(minDaysPastDue));
        
        if (query != null) {
            builder.queryParam("page", query.page())
                   .queryParam("size", query.size())
                   .queryParamIfPresent("search", java.util.Optional.ofNullable(query.search()));
        }
        
        return get(builder.toUriString());
    }

    public JsonNode fetchDueInstallments(LocalDate fromDate, LocalDate toDate, PageQuery query) {
        var builder = UriComponentsBuilder.fromHttpUrl(collectionsUrl)
                .pathSegment("api", "v1", "internal", "reports", "due-installments")
                .queryParam("fromDate", fromDate)
                .queryParam("toDate", toDate);

        if (query != null) {
            builder.queryParam("page", query.page())
                   .queryParam("size", query.size())
                   .queryParamIfPresent("search", java.util.Optional.ofNullable(query.search()));
        }

        return get(builder.toUriString());
    }

    public JsonNode fetchScheduleInstallments(UUID loanId) {
        var url = UriComponentsBuilder.fromHttpUrl(collectionsUrl)
                .pathSegment("api", "v1", "internal", "reports", "schedule-installments")
                .queryParam("loanId", loanId)
                .toUriString();
        return get(url);
    }

    public JsonNode fetchEarlySettlements(LocalDate fromDate, LocalDate toDate, PageQuery query) {
        var builder = UriComponentsBuilder.fromHttpUrl(collectionsUrl)
                .pathSegment("api", "v1", "internal", "reports", "early-settlements")
                .queryParam("fromDate", fromDate)
                .queryParam("toDate", toDate);

        if (query != null) {
            builder.queryParam("page", query.page())
                   .queryParam("size", query.size())
                   .queryParamIfPresent("search", java.util.Optional.ofNullable(query.search()));
        }

        return get(builder.toUriString());
    }

    public JsonNode fetchOutstandingBalances(LocalDate asOfDate, String productCode, PageQuery query) {
        var builder = UriComponentsBuilder.fromHttpUrl(lendingUrl)
                .pathSegment("api", "v1", "internal", "reports", "outstanding-balances");
        if (asOfDate != null) builder.queryParam("asOfDate", asOfDate);
        if (productCode != null) builder.queryParam("productCode", productCode);

        if (query != null) {
            builder.queryParam("page", query.page())
                   .queryParam("size", query.size())
                   .queryParamIfPresent("search", java.util.Optional.ofNullable(query.search()));
        }

        return get(builder.toUriString());
    }

    public JsonNode fetchLoansByCustomer(UUID customerId) {
        var url = UriComponentsBuilder.fromHttpUrl(lendingUrl)
                .pathSegment("api", "v1", "internal", "reports", "loans-by-customer")
                .queryParam("customerId", customerId)
                .toUriString();
        return get(url);
    }

    public JsonNode fetchLoanLookup(List<UUID> loanIds) {
        if (loanIds == null || loanIds.isEmpty()) return objectMapper.createArrayNode();
        var builder = UriComponentsBuilder.fromHttpUrl(lendingUrl)
                .pathSegment("api", "v1", "internal", "reports", "loan-lookup");
        for (var id : loanIds) builder.queryParam("loanIds", id);
        return get(builder.toUriString());
    }

    public JsonNode fetchDisbursedLoans(LocalDate fromDate, LocalDate toDate, String productCode, PageQuery query) {
        var builder = UriComponentsBuilder.fromHttpUrl(lendingUrl)
                .pathSegment("api", "v1", "internal", "reports", "disbursed-loans")
                .queryParam("fromDate", fromDate)
                .queryParam("toDate", toDate);
        if (productCode != null && !productCode.isBlank()) {
            builder.queryParam("productCode", productCode);
        }

        if (query != null) {
            builder.queryParam("page", query.page())
                   .queryParam("size", query.size())
                   .queryParamIfPresent("search", java.util.Optional.ofNullable(query.search()));
        }

        return get(builder.toUriString());
    }

    private JsonNode get(String url) {
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            var authHeader = currentAuthorizationHeader();
            if (authHeader != null) headers.set("Authorization", authHeader);

            var response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);

            var root = objectMapper.readTree(response.getBody() != null ? response.getBody() : "{}");
            return root.has("data") ? root.get("data") : root;
        } catch (Exception e) {
            log.warn("Report data fetch failed for {}: {}", url, e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private String currentAuthorizationHeader() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest req = sra.getRequest();
            return req.getHeader("Authorization");
        }
        return null;
    }
}
