package com.ksa.financing.middleware.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregated cost reports over the env-specific client_request_* tables.
 *
 * Snapshot pricing: {@code api_cost} on each row is the price that was active at the
 * moment the call was made, so admin price changes never alter past totals.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cost-reports")
@RequiredArgsConstructor
@Tag(name = "Cost Reports", description = "Per-customer / per-application / per-API cost dashboards")
public class CostReportController {

    private final JdbcTemplate jdbcTemplate;

    // ────────────────────────────────────────────────────────────────
    // Per-customer overall summary (lifetime onboarding + applications)
    // ────────────────────────────────────────────────────────────────

    @SecuredEndpoint(obj = "middleware.cost-reports", act = "read")
    @GetMapping("/by-customer/{customerId}")
    @Operation(summary = "Aggregate cost spent on behalf of a single customer, broken down by context type and API")
    public Map<String, Object> byCustomer(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "test") String env,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var table = resolveTable(env);

        String filterClause = " AND created_at BETWEEN COALESCE(?, '-infinity'::timestamptz) AND COALESCE(?, 'infinity'::timestamptz)";

        // Totals
        Map<String, Object> totals = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS call_count, " +
                        "       COALESCE(SUM(api_cost), 0) AS total_cost, " +
                        "       MIN(cost_currency) AS currency, " +
                        "       MIN(created_at) AS first_call, " +
                        "       MAX(created_at) AS last_call " +
                        "FROM " + table +
                        " WHERE tenant_id = ? AND customer_id = ?" + filterClause,
                tenantId, customerId, from, to);

        // Breakdown by context_type (ONBOARDING vs APPLICATION)
        List<Map<String, Object>> byContext = jdbcTemplate.queryForList(
                "SELECT COALESCE(context_type, 'UNCATEGORISED') AS context_type, " +
                        "       COUNT(*) AS call_count, COALESCE(SUM(api_cost), 0) AS total_cost " +
                        "FROM " + table +
                        " WHERE tenant_id = ? AND customer_id = ?" + filterClause +
                        " GROUP BY context_type ORDER BY total_cost DESC",
                tenantId, customerId, from, to);

        // Breakdown by application
        List<Map<String, Object>> byApplication = jdbcTemplate.queryForList(
                "SELECT COALESCE(application_id, '(no-application)') AS application_id, " +
                        "       COUNT(*) AS call_count, COALESCE(SUM(api_cost), 0) AS total_cost " +
                        "FROM " + table +
                        " WHERE tenant_id = ? AND customer_id = ?" + filterClause +
                        " GROUP BY application_id ORDER BY total_cost DESC",
                tenantId, customerId, from, to);

        // Breakdown by API
        List<Map<String, Object>> byApi = jdbcTemplate.queryForList(
                "SELECT api_code, COUNT(*) AS call_count, COALESCE(SUM(api_cost), 0) AS total_cost " +
                        "FROM " + table +
                        " WHERE tenant_id = ? AND customer_id = ?" + filterClause +
                        " GROUP BY api_code ORDER BY total_cost DESC",
                tenantId, customerId, from, to);

        var result = new LinkedHashMap<String, Object>();
        result.put("customerId", customerId);
        result.put("environment", env.toUpperCase());
        result.put("totals", totals);
        result.put("byContextType", byContext);
        result.put("byApplication", byApplication);
        result.put("byApi", byApi);
        return result;
    }

    // ────────────────────────────────────────────────────────────────
    // Per-application drilldown (single loan application)
    // ────────────────────────────────────────────────────────────────

    @SecuredEndpoint(obj = "middleware.cost-reports", act = "read")
    @GetMapping("/by-application/{applicationId}")
    @Operation(summary = "Itemised cost for a single application")
    public Map<String, Object> byApplication(
            @PathVariable String applicationId,
            @RequestParam(defaultValue = "test") String env,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var table = resolveTable(env);

        Map<String, Object> totals = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS call_count, COALESCE(SUM(api_cost), 0) AS total_cost, " +
                        "       MIN(cost_currency) AS currency, MIN(created_at) AS first_call, MAX(created_at) AS last_call " +
                        "FROM " + table + " WHERE tenant_id = ? AND application_id = ?",
                tenantId, applicationId);

        List<Map<String, Object>> items = jdbcTemplate.queryForList(
                "SELECT created_at, api_code, provider_code, status, response_status, " +
                        "       api_cost, cost_currency, caller_service, idempotency_key, customer_id " +
                        "FROM " + table + " WHERE tenant_id = ? AND application_id = ? ORDER BY created_at ASC",
                tenantId, applicationId);

        var result = new LinkedHashMap<String, Object>();
        result.put("applicationId", applicationId);
        result.put("environment", env.toUpperCase());
        result.put("totals", totals);
        result.put("items", items);
        return result;
    }

    // ────────────────────────────────────────────────────────────────
    // Onboarding-only summary for a customer
    // ────────────────────────────────────────────────────────────────

    @SecuredEndpoint(obj = "middleware.cost-reports", act = "read")
    @GetMapping("/onboarding/{customerId}")
    @Operation(summary = "Total cost incurred during the onboarding stage of a customer")
    public Map<String, Object> onboarding(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "test") String env,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var table = resolveTable(env);

        Map<String, Object> totals = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS call_count, COALESCE(SUM(api_cost), 0) AS total_cost, " +
                        "       MIN(cost_currency) AS currency " +
                        "FROM " + table +
                        " WHERE tenant_id = ? AND customer_id = ? AND context_type = 'ONBOARDING'",
                tenantId, customerId);

        List<Map<String, Object>> items = jdbcTemplate.queryForList(
                "SELECT created_at, api_code, provider_code, status, api_cost, cost_currency " +
                        "FROM " + table +
                        " WHERE tenant_id = ? AND customer_id = ? AND context_type = 'ONBOARDING' " +
                        "ORDER BY created_at ASC",
                tenantId, customerId);

        var result = new LinkedHashMap<String, Object>();
        result.put("customerId", customerId);
        result.put("environment", env.toUpperCase());
        result.put("totals", totals);
        result.put("items", items);
        return result;
    }

    // ────────────────────────────────────────────────────────────────
    // Cross-customer summary: top customers by cost
    // ────────────────────────────────────────────────────────────────

    @SecuredEndpoint(obj = "middleware.cost-reports", act = "read")
    @GetMapping("/customers")
    @Operation(summary = "List customers ranked by total third-party API spend")
    public Map<String, Object> listCustomers(
            @RequestParam(defaultValue = "test") String env,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var table = resolveTable(env);
        int safeLimit = Math.min(Math.max(limit, 1), 500);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT customer_id, " +
                        "       COUNT(*) AS call_count, " +
                        "       COALESCE(SUM(api_cost), 0) AS total_cost, " +
                        "       COUNT(DISTINCT application_id) FILTER (WHERE application_id IS NOT NULL) AS application_count, " +
                        "       MIN(cost_currency) AS currency, " +
                        "       MAX(created_at) AS last_call " +
                        "FROM " + table +
                        " WHERE tenant_id = ? AND customer_id IS NOT NULL " +
                        "       AND created_at BETWEEN COALESCE(?, '-infinity'::timestamptz) AND COALESCE(?, 'infinity'::timestamptz) " +
                        "GROUP BY customer_id ORDER BY total_cost DESC LIMIT ?",
                tenantId, from, to, safeLimit);

        var result = new LinkedHashMap<String, Object>();
        result.put("environment", env.toUpperCase());
        result.put("limit", safeLimit);
        result.put("rows", rows);
        return result;
    }

    // ────────────────────────────────────────────────────────────────
    // By API summary
    // ────────────────────────────────────────────────────────────────

    @SecuredEndpoint(obj = "middleware.cost-reports", act = "read")
    @GetMapping("/by-api")
    @Operation(summary = "Total cost grouped by API code")
    public Map<String, Object> byApi(
            @RequestParam(defaultValue = "test") String env,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var table = resolveTable(env);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT api_code, provider_code, COUNT(*) AS call_count, " +
                        "       COALESCE(SUM(api_cost), 0) AS total_cost, MIN(cost_currency) AS currency " +
                        "FROM " + table +
                        " WHERE tenant_id = ? " +
                        "       AND created_at BETWEEN COALESCE(?, '-infinity'::timestamptz) AND COALESCE(?, 'infinity'::timestamptz) " +
                        "GROUP BY api_code, provider_code ORDER BY total_cost DESC",
                tenantId, from, to);

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (var row : rows) {
            Object cost = row.get("total_cost");
            if (cost instanceof BigDecimal bd) grandTotal = grandTotal.add(bd);
        }

        var result = new LinkedHashMap<String, Object>();
        result.put("environment", env.toUpperCase());
        result.put("grandTotal", grandTotal);
        result.put("rows", rows);
        return result;
    }

    // ────────────────────────────────────────────────────────────────
    // By caller service
    // ────────────────────────────────────────────────────────────────

    @SecuredEndpoint(obj = "middleware.cost-reports", act = "read")
    @GetMapping("/by-service")
    @Operation(summary = "Total cost grouped by caller service")
    public Map<String, Object> byService(
            @RequestParam(defaultValue = "test") String env,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var table = resolveTable(env);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT COALESCE(caller_service, '(unknown)') AS caller_service, " +
                        "       COUNT(*) AS call_count, COALESCE(SUM(api_cost), 0) AS total_cost, MIN(cost_currency) AS currency " +
                        "FROM " + table +
                        " WHERE tenant_id = ? " +
                        "       AND created_at BETWEEN COALESCE(?, '-infinity'::timestamptz) AND COALESCE(?, 'infinity'::timestamptz) " +
                        "GROUP BY caller_service ORDER BY total_cost DESC",
                tenantId, from, to);

        var result = new LinkedHashMap<String, Object>();
        result.put("environment", env.toUpperCase());
        result.put("rows", rows);
        return result;
    }

    // ────────────────────────────────────────────────────────────────
    // Relink: rewrite customer_id on past rows.
    // Used by onboarding to swap a temp UUID for the real customer UUID
    // once the customer is created in customer-service.
    // ────────────────────────────────────────────────────────────────

    @SecuredEndpoint(obj = "middleware.cost-reports", act = "manage")
    @PostMapping("/relink-customer")
    @Operation(summary = "Rewrite customer_id on past rows (e.g. swap onboarding temp UUID for real one)")
    public Map<String, Object> relinkCustomer(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        String env = body.getOrDefault("env", "test").toString();
        String table = resolveTable(env);

        UUID toCustomerId = UUID.fromString(body.get("toCustomerId").toString());
        Object fromCustomerRaw = body.get("fromCustomerId");
        Object applicationIdRaw = body.get("applicationId");

        int affected;
        if (applicationIdRaw != null && !applicationIdRaw.toString().isBlank()) {
            // Preferred path: relink by application_id (workflow id) — covers temp/no-customer-id rows too.
            affected = jdbcTemplate.update(
                    "UPDATE " + table + " SET customer_id = ? " +
                            "WHERE tenant_id = ? AND application_id = ? " +
                            "  AND (customer_id IS NULL OR customer_id <> ?)",
                    toCustomerId, tenantId, applicationIdRaw.toString(), toCustomerId);
        } else if (fromCustomerRaw != null && !fromCustomerRaw.toString().isBlank()) {
            UUID fromCustomerId = UUID.fromString(fromCustomerRaw.toString());
            affected = jdbcTemplate.update(
                    "UPDATE " + table + " SET customer_id = ? " +
                            "WHERE tenant_id = ? AND customer_id = ?",
                    toCustomerId, tenantId, fromCustomerId);
        } else {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Provide either applicationId or fromCustomerId");
        }

        log.info("Relinked {} rows to customer {} in {}", affected, toCustomerId, table);
        return Map.of(
                "rowsAffected", affected,
                "toCustomerId", toCustomerId,
                "environment", env.toUpperCase()
        );
    }

    private String resolveTable(String env) {
        if (env == null) return "client_request_test";
        return switch (env.toLowerCase()) {
            case "test" -> "client_request_test";
            case "dev"  -> "client_request_dev";
            case "prod" -> "client_request_prod";
            default     -> throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Unknown environment: " + env + " (expected test/dev/prod)");
        };
    }

    private UUID extractTenantId(Jwt jwt) {
        Object tenantClaim = jwt.getClaim("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim.toString());
    }
}
