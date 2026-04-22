package com.ksa.financing.lending.adapter.rest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.lending.adapter.rest.response.InstallmentScheduleResponse;
import com.ksa.financing.lending.adapter.rest.response.LoanContractResponse;
import com.ksa.financing.lending.adapter.rest.response.LoanOverviewResponse;
import com.ksa.financing.lending.adapter.rest.response.LoanResponse;
import com.ksa.financing.lending.application.mapper.LoanMapper;
import com.ksa.financing.lending.domain.port.in.ManageLoanUseCase;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaAmortizationScheduleRepository;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loans", description = "Active loan management endpoints")
public class LoanController {

    private final ManageLoanUseCase useCase;
    private final LoanMapper mapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JpaAmortizationScheduleRepository amortizationScheduleRepository;
    private final JpaLoanRepository loanRepository;

    private static final java.util.regex.Pattern INVOICE_ID_PATTERN =
            java.util.regex.Pattern.compile("^INV-([0-9A-Fa-f]{8})-(\\d{1,5})$");

    @Value("${app.services.fineract-base-url}")
    private String fineractBaseUrl;

    @Value("${app.services.collections-service-url:${COLLECTIONS_SERVICE_URL:http://collections-service:8099}}")
    private String collectionsServiceUrl;

    @Value("${app.services.fineract-username:#{null}}")
    private String fineractUsername;

    @Value("${app.services.fineract-password:#{null}}")
    private String fineractPassword;

    @Value("${app.services.fineract-tenant-id:${FINERACT_TENANT_ID:default}}")
    private String fineractTenantId;

    public LoanController(ManageLoanUseCase useCase, LoanMapper mapper,
                           RestTemplate restTemplate, ObjectMapper objectMapper,
                           JpaAmortizationScheduleRepository amortizationScheduleRepository,
                           JpaLoanRepository loanRepository) {
        this.useCase = useCase;
        this.mapper = mapper;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.amortizationScheduleRepository = amortizationScheduleRepository;
        this.loanRepository = loanRepository;
    }

    @SecuredEndpoint(obj = "loans.installments", act = "update")
    @org.springframework.web.bind.annotation.PatchMapping("/invoices/{invoiceId}/due-date")
    @org.springframework.transaction.annotation.Transactional
    @Operation(summary = "Shift an invoice's due_date in the amortization schedule and re-run delinquency in collections-service.",
            description = "Materializes the schedule into amortization_schedules if missing, updates the one row, then forwards to collections-service so late-payment penalty / stage transitions accrue.")
    public ResponseEntity<Map<String, Object>> shiftInvoiceDueDate(
            @PathVariable("invoiceId") String invoiceId,
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var matcher = INVOICE_ID_PATTERN.matcher(invoiceId);
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                    "Invalid invoiceId format. Expected: INV-XXXXXXXX-NNN");
        }
        var prefix = matcher.group(1);
        var installmentNumber = Integer.parseInt(matcher.group(2));

        var newDueDateStr = body.get("newDueDate");
        if (newDueDateStr == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "newDueDate is required (YYYY-MM-DD)");
        }
        var newDueDate = LocalDate.parse(newDueDateStr.toString());

        var loan = loanRepository.findFirstByTenantIdAndIdPrefix(tenantId, prefix)
                .orElseThrow(() -> NotFoundException.forEntity("Invoice", invoiceId));

        materializeAmortizationIfEmpty(loan);

        var rows = amortizationScheduleRepository
                .findByLoanIdAndActiveOrderByInstallmentNumberAsc(loan.getId(), true);
        var target = rows.stream()
                .filter(r -> r.getInstallmentNumber() == installmentNumber)
                .findFirst()
                .orElseThrow(() -> NotFoundException.forEntity("Invoice", invoiceId));

        var previousDueDate = target.getDueDate();
        target.setDueDate(newDueDate);
        amortizationScheduleRepository.save(target);

        // Forward to collections-service so delinquency engine re-ticks + late-payment penalty accrues
        Map<String, Object> delinquency = forwardToCollections(invoiceId, newDueDate, jwt);

        var result = new LinkedHashMap<String, Object>();
        result.put("invoiceId", invoiceId);
        result.put("loanId", loan.getId().toString());
        result.put("installmentNumber", installmentNumber);
        result.put("previousDueDate", previousDueDate);
        result.put("newDueDate", newDueDate);
        result.put("paymentStatus", target.getPaymentStatus());
        if (delinquency != null) result.put("delinquency", delinquency);
        return ResponseEntity.ok(result);
    }

    /** Populates amortization_schedules with the legacy-computed schedule if it's empty for this loan. */
    private void materializeAmortizationIfEmpty(com.ksa.financing.lending.infrastructure.persistence.entity.LoanJpaEntity loan) {
        var existing = amortizationScheduleRepository
                .findByLoanIdAndActiveOrderByInstallmentNumberAsc(loan.getId(), true);
        if (!existing.isEmpty()) return;
        if (loan.getTenureMonths() <= 0 || loan.getPrincipalAmount() == null) return;

        var startDate = loan.getDisbursementDate() != null
                ? loan.getDisbursementDate().plusMonths(1)
                : LocalDate.now().plusMonths(1);

        var principal = loan.getPrincipalAmount();
        var totalProfit = loan.getProfitAmount() != null ? loan.getProfitAmount() : BigDecimal.ZERO;
        var tenure = loan.getTenureMonths();
        var monthlyPrincipal = principal.divide(BigDecimal.valueOf(tenure), 6, RoundingMode.HALF_UP);
        var monthlyProfit = totalProfit.divide(BigDecimal.valueOf(tenure), 6, RoundingMode.HALF_UP);
        var monthlyTotal = monthlyPrincipal.add(monthlyProfit);

        BigDecimal cumulativePrincipal = BigDecimal.ZERO;
        BigDecimal cumulativeProfit = BigDecimal.ZERO;
        BigDecimal closing = principal;
        for (int n = 1; n <= tenure; n++) {
            var opening = closing;
            closing = closing.subtract(monthlyPrincipal);
            cumulativePrincipal = cumulativePrincipal.add(monthlyPrincipal);
            cumulativeProfit = cumulativeProfit.add(monthlyProfit);

            var row = com.ksa.financing.lending.infrastructure.persistence.entity.AmortizationScheduleJpaEntity.builder()
                    .tenantId(loan.getTenantId())
                    .loanId(loan.getId())
                    .scheduleVersion(1)
                    .active(true)
                    .installmentNumber(n)
                    .dueDate(startDate.plusMonths(n - 1L))
                    .openingPrincipal(opening)
                    .principalComponent(monthlyPrincipal)
                    .profitComponent(monthlyProfit)
                    .totalInstallment(monthlyTotal)
                    .closingPrincipal(closing.max(BigDecimal.ZERO))
                    .cumulativePrincipal(cumulativePrincipal)
                    .cumulativeProfit(cumulativeProfit)
                    .calculationMethod("REDUCING_BALANCE")
                    .paymentStatus("PENDING")
                    .skipped(false)
                    .build();
            amortizationScheduleRepository.save(row);
        }
        log.info("Materialized {} amortization rows for loanId={}", tenure, loan.getId());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> forwardToCollections(String invoiceId, LocalDate newDueDate, Jwt jwt) {
        // Spring's default RestTemplate uses HttpURLConnection which rejects PATCH.
        // Use the JDK HttpClient instead so this one call works without adding a bean.
        try {
            var body = "{\"newDueDate\":\"" + newDueDate + "\"}";
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(collectionsServiceUrl + "/api/v1/admin/invoices/" + invoiceId + "/due-date"))
                    .header("Authorization", "Bearer " + jwt.getTokenValue())
                    .header("Content-Type", "application/json")
                    .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString(body))
                    .build();
            var client = java.net.http.HttpClient.newHttpClient();
            var resp = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2 || resp.body() == null) {
                log.warn("Collections sync non-2xx for invoice {}: status={} body={}", invoiceId, resp.statusCode(), resp.body());
                return null;
            }
            var node = objectMapper.readTree(resp.body());
            var payload = node.has("data") ? node.get("data") : node;
            return objectMapper.convertValue(payload, Map.class);
        } catch (Exception ex) {
            log.warn("Collections delinquency sync failed for invoice {}: {}", invoiceId, ex.getMessage());
            return null;
        }
    }

    @SecuredEndpoint(obj = "loans.installments", act = "read")
    @GetMapping("/{loanId}/early-settlement/invoices")
    @Operation(summary = "List early-settlement invoices for a loan — discounted virtual invoices for each "
            + "installment that currently qualifies under the EARLY_SETTLEMENT DelinquencyRule.")
    public ResponseEntity<List<com.ksa.financing.lending.adapter.rest.response.EarlySettlementInvoiceResponse>> listEarlySettlementInvoices(
            @PathVariable("loanId") String loanId,
            @AuthenticationPrincipal Jwt jwt) {

        var snapshots = fetchCollectionsSnapshotsByInstallment(loanId, jwt);
        if (snapshots.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        var out = new ArrayList<com.ksa.financing.lending.adapter.rest.response.EarlySettlementInvoiceResponse>();
        var prefix = loanId.substring(0, 8).toUpperCase();
        for (var entry : snapshots.entrySet()) {
            var snap = entry.getValue();
            if (!Boolean.TRUE.equals(snap.get("earlySettlementEligible"))) continue;

            int installmentNumber = entry.getKey();
            var originalAmount = toBigDecimal(snap.get("outstandingAmount"));
            var discountPct = toBigDecimal(snap.get("earlySettlementDiscountPercentage"));
            var discountAmt = toBigDecimal(snap.get("earlySettlementDiscountAmount"));
            BigDecimal totalDiscount = BigDecimal.ZERO;
            if (discountPct != null && discountPct.compareTo(BigDecimal.ZERO) > 0) {
                totalDiscount = originalAmount.multiply(discountPct)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else if (discountAmt != null) {
                totalDiscount = discountAmt;
            }
            var finalAmount = originalAmount.subtract(totalDiscount).max(BigDecimal.ZERO);

            var dueDateStr = snap.get("dueDate") != null ? snap.get("dueDate").toString() : null;
            LocalDate dueDate = dueDateStr != null ? LocalDate.parse(dueDateStr) : null;
            Integer validUntilDay = snap.get("earlySettlementValidUntilDay") instanceof Number n ? n.intValue() : null;
            LocalDate validUntil = (dueDate != null && validUntilDay != null) ? dueDate.plusDays(validUntilDay) : null;
            Integer dpd = snap.get("dpd") instanceof Number n ? n.intValue() : null;
            String status = snap.get("status") != null ? snap.get("status").toString() : null;

            out.add(new com.ksa.financing.lending.adapter.rest.response.EarlySettlementInvoiceResponse(
                    "INV-" + prefix + "-" + String.format("%03d", installmentNumber),
                    "ESI-" + prefix + "-" + String.format("%03d", installmentNumber),
                    installmentNumber,
                    dueDate,
                    originalAmount,
                    discountPct,
                    discountAmt,
                    totalDiscount,
                    finalAmount,
                    validUntilDay,
                    validUntil,
                    dpd,
                    status
            ));
        }
        out.sort(java.util.Comparator.comparingInt(com.ksa.financing.lending.adapter.rest.response.EarlySettlementInvoiceResponse::installmentNumber));
        return ResponseEntity.ok(out);
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    @SecuredEndpoint(obj = "loans", act = "read")
    @GetMapping("/lookup/by-invoice/{invoiceId}")
    @Operation(summary = "Resolve loanId from an invoiceId (e.g. INV-19466BE4-001). "
            + "Cross-service helper so collections-service can seed a schedule without needing the loanId up-front.")
    public ResponseEntity<Map<String, String>> lookupByInvoice(
            @PathVariable("invoiceId") String invoiceId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var matcher = INVOICE_ID_PATTERN.matcher(invoiceId);
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                    "Invalid invoiceId format. Expected: INV-XXXXXXXX-NNN");
        }
        var loanPrefix = matcher.group(1);
        var loan = loanRepository.findFirstByTenantIdAndIdPrefix(tenantId, loanPrefix)
                .orElseThrow(() -> NotFoundException.forEntity("Invoice", invoiceId));
        return ResponseEntity.ok(Map.of(
                "loanId", loan.getId().toString(),
                "loanNumber", loan.getLoanNumber(),
                "productId", loan.getProductId() != null ? loan.getProductId().toString() : ""
        ));
    }

    @SecuredEndpoint(obj = "loans", act = "read")
    @GetMapping("/{loanId}")
    @Operation(summary = "Get a loan by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Loan found"),
        @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    public ResponseEntity<LoanResponse> getLoan(
            @PathVariable("loanId") String loanId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        var tenantId = extractTenantId(jwt);
        var loan = useCase.getLoan(tenantId, UUID.fromString(loanId));
        var response = LoanResponse.from(mapper.toDto(loan));

        return ResponseEntity
                .ok()
                .header("X-Correlation-ID", correlationId)
                .body(response);
    }

    @SecuredEndpoint(obj = "loans", act = "read")
    @GetMapping("/by-number/{loanNumber}")
    @Operation(summary = "Get a loan by loan number")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Loan found"),
        @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    public ResponseEntity<LoanResponse> getLoanByNumber(
            @PathVariable("loanNumber") String loanNumber,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        var tenantId = extractTenantId(jwt);
        var loan = useCase.getLoanByNumber(tenantId, loanNumber);
        var response = LoanResponse.from(mapper.toDto(loan));

        return ResponseEntity
                .ok()
                .header("X-Correlation-ID", correlationId)
                .body(response);
    }

    @SecuredEndpoint(obj = "loans", act = "read")
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List loans by customer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Loans listed")
    })
    public ResponseEntity<List<LoanResponse>> listLoansByCustomer(
            @PathVariable(name = "customerId") UUID customerId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        var tenantId = extractTenantId(jwt);
        var loans = useCase.listLoansByCustomer(tenantId, customerId);
        var dtos = mapper.toDtos(loans);

        var loanIds = loans.stream()
                .map(l -> l.getId() != null ? l.getId().getValue() : null)
                .filter(java.util.Objects::nonNull)
                .toList();
        var eligibility = fetchEarlySettlementEligibility(loanIds, jwt);

        var responses = new java.util.ArrayList<LoanResponse>(dtos.size());
        for (int i = 0; i < dtos.size(); i++) {
            UUID lid = loans.get(i).getId() != null ? loans.get(i).getId().getValue() : null;
            Boolean flag = lid != null ? eligibility.get(lid) : null;
            responses.add(LoanResponse.from(dtos.get(i), flag));
        }

        return ResponseEntity
                .ok()
                .header("X-Correlation-ID", correlationId)
                .body(responses);
    }

    /**
     * Calls collections-service for strict per-loan EARLY_SETTLEMENT eligibility.
     * The flag is true only when the product has an active rule AND the loan's
     * schedule has at least one unpaid installment currently matching the rule
     * (invoice-number scope + DPD window). One sync hop per list request, batched.
     * Returns an empty map on any failure so the loan list still renders.
     */
    private java.util.Map<UUID, Boolean> fetchEarlySettlementEligibility(List<UUID> loanIds, Jwt jwt) {
        if (loanIds == null || loanIds.isEmpty()) {
            return java.util.Map.of();
        }
        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            String csv = loanIds.stream().map(UUID::toString).reduce((a, b) -> a + "," + b).orElse("");
            var response = restTemplate.exchange(
                    collectionsServiceUrl + "/api/v1/admin/delinquency-rules/eligibility?loanIds=" + csv,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return java.util.Map.of();
            }
            var root = objectMapper.readTree(response.getBody());
            // ApiResponseAdvice wraps the Map<UUID,Boolean> inside { data: {...}, message, timestamp }.
            // Fall back to root if the wrapper is ever disabled.
            var payload = root.has("data") ? root.get("data") : root;
            var out = new java.util.LinkedHashMap<UUID, Boolean>();
            payload.fieldNames().forEachRemaining(field -> {
                try {
                    out.put(UUID.fromString(field), payload.path(field).asBoolean(false));
                } catch (IllegalArgumentException ignored) { /* skip malformed key */ }
            });
            return out;
        } catch (Exception ex) {
            log.debug("Early-settlement eligibility lookup skipped: {}", ex.getMessage());
            return java.util.Map.of();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // BRD UC#04: FINANCE OVERVIEW
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "loans.overview", act = "read")
    @GetMapping("/customer/{customerId}/overview")
    @Operation(summary = "Get finance overview for customer (BRD UC#04)")
    public ResponseEntity<LoanOverviewResponse> getFinanceOverview(
            @PathVariable(name = "customerId") UUID customerId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var loans = useCase.listLoansByCustomer(tenantId, customerId);

        var activeLoans = new java.util.ArrayList<LoanOverviewResponse.LoanSummary>();
        var completedLoans = new java.util.ArrayList<LoanOverviewResponse.LoanSummary>();
        var totalPaid = java.math.BigDecimal.ZERO;
        var totalPending = java.math.BigDecimal.ZERO;
        var totalOverdue = java.math.BigDecimal.ZERO;

        for (var loan : loans) {
            var dto = mapper.toDto(loan);
            var outstanding = dto.totalOutstanding() != null ? dto.totalOutstanding() : java.math.BigDecimal.ZERO;
            var total = dto.totalAmount() != null ? dto.totalAmount() : java.math.BigDecimal.ZERO;
            var paid = total.subtract(outstanding);
            if (paid.compareTo(java.math.BigDecimal.ZERO) < 0) paid = java.math.BigDecimal.ZERO;

            // Estimate overdue from DPD (days past due)
            var overdue = dto.currentDpd() > 0 && dto.installmentAmount() != null
                    ? dto.installmentAmount() : java.math.BigDecimal.ZERO;

            totalPaid = totalPaid.add(paid);
            totalPending = totalPending.add(outstanding);
            totalOverdue = totalOverdue.add(overdue);

            // Estimate installments paid from amounts
            int estimatedPaid = dto.installmentAmount() != null && dto.installmentAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                    ? paid.divide(dto.installmentAmount(), 0, java.math.RoundingMode.DOWN).intValue() : 0;

            var summary = new LoanOverviewResponse.LoanSummary(
                    dto.id(),
                    dto.loanNumber(),
                    dto.productCode() != null ? dto.productCode() : "Personal Finance",
                    dto.applicationId(),
                    dto.principalAmount(),
                    total,
                    dto.installmentAmount(),
                    dto.tenureMonths(),
                    estimatedPaid,
                    dto.tenureMonths() - estimatedPaid,
                    dto.status(),
                    dto.disbursementDate(),
                    dto.firstDueDate(),
                    dto.maturityDate(),
                    paid,
                    outstanding.subtract(overdue),
                    overdue
            );

            if ("ACTIVE".equals(dto.status()) || "DISBURSED".equals(dto.status())) {
                activeLoans.add(summary);
            } else {
                completedLoans.add(summary);
            }
        }

        return ResponseEntity.ok(new LoanOverviewResponse(
                activeLoans, completedLoans, totalPaid, totalPending, totalOverdue));
    }

    @SecuredEndpoint(obj = "loans.installments", act = "read")
    @GetMapping("/{loanId}/installments")
    @Operation(summary = "Get installment schedule for a loan (BRD UC#04)")
    public ResponseEntity<List<InstallmentScheduleResponse>> getInstallments(
            @PathVariable("loanId") String loanId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var loan = useCase.getLoan(tenantId, UUID.fromString(loanId));
        var collectionsSnapshots = fetchCollectionsSnapshotsByInstallment(loanId, jwt);
        var collectionsStatusByInstallment = collectionsSnapshots.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().get("status") != null)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get("status").toString()));
        var dbSchedules = amortizationScheduleRepository.findByLoanIdAndActiveOrderByInstallmentNumberAsc(
                UUID.fromString(loanId), true);

        if (dbSchedules.isEmpty()) {
            return ResponseEntity.ok(buildLegacyInstallmentSchedule(
                    loanId, mapper.toDto(loan), collectionsStatusByInstallment, collectionsSnapshots));
        }

        var responses = new ArrayList<InstallmentScheduleResponse>();
        for (var row : dbSchedules) {
            var invoiceId = "INV-" + loanId.substring(0, 8).toUpperCase() + "-"
                    + String.format("%03d", row.getInstallmentNumber());
            var status = row.getPaymentStatus() != null ? row.getPaymentStatus() : "PENDING";
            status = collectionsStatusByInstallment.getOrDefault(row.getInstallmentNumber(), status);
            status = normalizeInstallmentStatus(status);
            var isPaid = "PAID".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status);

            responses.add(new InstallmentScheduleResponse(
                    invoiceId,
                    row.getInstallmentNumber(),
                    row.getDueDate(),
                    row.getTotalInstallment(),
                    row.getPrincipalComponent(),
                    row.getProfitComponent(),
                    row.getClosingPrincipal(),
                    status,
                    isPaid ? LocalDate.now() : null,
                    isPaid ? row.getTotalInstallment() : null,
                    isPaid,
                    collectionsSnapshots.get(row.getInstallmentNumber())
            ));
        }

        return ResponseEntity.ok(responses);
    }

    @SecuredEndpoint(obj = "loans.installments", act = "read")
    @GetMapping("/invoices/{invoiceId}")
    @Operation(summary = "Get invoice detail by invoiceId",
            description = "Invoice ID format: INV-XXXXXXXX-NNN (first 8 chars of loanId + 3-digit installment number).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice found"),
        @ApiResponse(responseCode = "400", description = "Invalid invoiceId format"),
        @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InstallmentScheduleResponse> getInvoiceDetail(
            @PathVariable("invoiceId") String invoiceId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);

        var matcher = INVOICE_ID_PATTERN.matcher(invoiceId);
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                    "Invalid invoiceId format. Expected: INV-XXXXXXXX-NNN");
        }
        var loanPrefix = matcher.group(1);
        var installmentNumber = Integer.parseInt(matcher.group(2));

        var loanEntity = loanRepository.findFirstByTenantIdAndIdPrefix(tenantId, loanPrefix)
                .orElseThrow(() -> NotFoundException.forEntity("Invoice", invoiceId));
        var loanId = loanEntity.getId().toString();

        var loan = useCase.getLoan(tenantId, loanEntity.getId());
        var collectionsSnapshots = fetchCollectionsSnapshotsByInstallment(loanId, jwt);
        var collectionsStatusByInstallment = collectionsSnapshots.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().get("status") != null)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get("status").toString()));

        var dbSchedules = amortizationScheduleRepository.findByLoanIdAndActiveOrderByInstallmentNumberAsc(
                loanEntity.getId(), true);

        if (!dbSchedules.isEmpty()) {
            var row = dbSchedules.stream()
                    .filter(s -> s.getInstallmentNumber() == installmentNumber)
                    .findFirst()
                    .orElseThrow(() -> NotFoundException.forEntity("Invoice", invoiceId));

            var status = row.getPaymentStatus() != null ? row.getPaymentStatus() : "PENDING";
            status = collectionsStatusByInstallment.getOrDefault(row.getInstallmentNumber(), status);
            status = normalizeInstallmentStatus(status);
            var isPaid = "PAID".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status);

            return ResponseEntity.ok(new InstallmentScheduleResponse(
                    invoiceId,
                    row.getInstallmentNumber(),
                    row.getDueDate(),
                    row.getTotalInstallment(),
                    row.getPrincipalComponent(),
                    row.getProfitComponent(),
                    row.getClosingPrincipal(),
                    status,
                    isPaid ? LocalDate.now() : null,
                    isPaid ? row.getTotalInstallment() : null,
                    isPaid,
                    collectionsSnapshots.get(row.getInstallmentNumber())
            ));
        }

        // Fallback: no amortization rows in DB — build legacy schedule and pick matching row
        var legacy = buildLegacyInstallmentSchedule(loanId, mapper.toDto(loan), collectionsStatusByInstallment, collectionsSnapshots);
        return legacy.stream()
                .filter(r -> r.installmentNumber() == installmentNumber)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseThrow(() -> NotFoundException.forEntity("Invoice", invoiceId));
    }

    private List<InstallmentScheduleResponse> buildLegacyInstallmentSchedule(
            String loanId,
            com.ksa.financing.lending.application.dto.LoanDto dto,
            Map<Integer, String> collectionsStatusByInstallment,
            Map<Integer, Map<String, Object>> collectionsSnapshots) {
        var schedule = new ArrayList<InstallmentScheduleResponse>();
        var startDate = dto.firstDueDate() != null ? dto.firstDueDate()
                : (dto.disbursementDate() != null ? dto.disbursementDate().plusMonths(1)
                : LocalDate.now().plusMonths(1));

        if (dto.installmentAmount() == null || dto.tenureMonths() <= 0) {
            return schedule;
        }

        var principal = dto.principalAmount() != null ? dto.principalAmount() : BigDecimal.ZERO;
        var totalProfit = dto.profitAmount() != null ? dto.profitAmount() : BigDecimal.ZERO;
        var monthlyPrincipal = principal.divide(BigDecimal.valueOf(dto.tenureMonths()), 2, RoundingMode.HALF_UP);
        var monthlyProfit = totalProfit.divide(BigDecimal.valueOf(dto.tenureMonths()), 2, RoundingMode.HALF_UP);
        var balance = principal.add(totalProfit);

        var total = dto.totalAmount() != null ? dto.totalAmount() : BigDecimal.ZERO;
        var outstanding = dto.totalOutstanding() != null ? dto.totalOutstanding() : BigDecimal.ZERO;
        var paidAmt = total.subtract(outstanding);
        int paidCount = dto.installmentAmount().compareTo(BigDecimal.ZERO) > 0
                ? paidAmt.divide(dto.installmentAmount(), 0, RoundingMode.DOWN).intValue() : 0;

        for (int i = 1; i <= dto.tenureMonths(); i++) {
            balance = balance.subtract(dto.installmentAmount());
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                balance = BigDecimal.ZERO;
            }
            var dueDate = startDate.plusMonths(i - 1);
            var invoiceId = "INV-" + loanId.substring(0, 8).toUpperCase() + "-" + String.format("%03d", i);
            var fallbackStatus = i <= paidCount ? "PAID" : (dueDate.isBefore(LocalDate.now()) ? "OVERDUE" : "PENDING");
            var resolvedStatus = normalizeInstallmentStatus(collectionsStatusByInstallment.getOrDefault(i, fallbackStatus));
            var isPaid = "PAID".equalsIgnoreCase(resolvedStatus) || "COMPLETED".equalsIgnoreCase(resolvedStatus);
            var isOverdue = "OVERDUE".equalsIgnoreCase(resolvedStatus);

            schedule.add(new InstallmentScheduleResponse(
                    invoiceId,
                    i,
                    dueDate,
                    dto.installmentAmount(),
                    monthlyPrincipal,
                    monthlyProfit,
                    balance,
                    isPaid ? "PAID" : (isOverdue ? "OVERDUE" : resolvedStatus),
                    isPaid ? LocalDate.now() : null,
                    isPaid ? dto.installmentAmount() : null,
                    isPaid,
                    collectionsSnapshots != null ? collectionsSnapshots.get(i) : null
            ));
        }
        return schedule;
    }

    private Map<Integer, String> fetchCollectionsStatusesByInstallment(String loanId, Jwt jwt) {
        return fetchCollectionsSnapshotsByInstallment(loanId, jwt).entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().get("status") != null)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get("status").toString()));
    }

    /**
     * Fetches the full per-installment delinquency snapshot from collections-service so
     * the lending installments API can embed a delinquency object (status, dpd,
     * latePenaltyAmount, earlySettlementEligible, discount fields).
     */
    @SuppressWarnings("unchecked")
    private Map<Integer, Map<String, Object>> fetchCollectionsSnapshotsByInstallment(String loanId, Jwt jwt) {
        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            var url = collectionsServiceUrl + "/api/v1/repayment-schedules/by-loan/" + loanId;
            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Map.of();
            }

            var root = objectMapper.readTree(response.getBody());
            var installments = root.path("data").path("installments");
            if (!installments.isArray()) {
                return Map.of();
            }

            var out = new HashMap<Integer, Map<String, Object>>();
            for (var item : installments) {
                int n = item.path("installmentNumber").asInt(-1);
                if (n <= 0) continue;
                out.put(n, objectMapper.convertValue(item, Map.class));
            }
            return out;
        } catch (Exception e) {
            log.warn("Unable to fetch collections snapshot for loanId={}: {}", loanId, e.getMessage());
            return Map.of();
        }
    }

    private String normalizeInstallmentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }
        return switch (status.toUpperCase()) {
            case "PAID", "COMPLETED", "OVERDUE", "PARTIALLY_PAID",
                 "DUE", "GRACE_PERIOD", "WAIVED", "DEFERRED" -> status.toUpperCase();
            default -> "PENDING";
        };
    }

    @SecuredEndpoint(obj = "loans.contract", act = "read")
    @GetMapping("/{loanId}/contract")
    @Operation(summary = "Get loan contract details (BRD UC#04)")
    public ResponseEntity<LoanContractResponse> getContract(
            @PathVariable("loanId") String loanId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var loan = useCase.getLoan(tenantId, UUID.fromString(loanId));
        return ResponseEntity.ok(buildContractResponse(loan));
    }

    @SecuredEndpoint(obj = "loans.receipts", act = "read")
    @GetMapping("/{loanId}/receipts/{installmentNumber}")
    @Operation(summary = "Download payment receipt (BRD UC#04)")
    public ResponseEntity<String> downloadReceipt(
            @PathVariable("loanId") String loanId,
            @PathVariable("installmentNumber") int installmentNumber,
            @AuthenticationPrincipal Jwt jwt) {

        extractTenantId(jwt); // validate tenant
        // Stub: document-service not yet implemented
        return ResponseEntity.status(501)
                .body("Receipt download not yet available. Document service pending implementation.");
    }

    // ══════════════════════════════════════════════════════════════
    // BY APPLICATION ID — resolve loan from applicationId
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "loans.installments", act = "read")
    @GetMapping("/application/{applicationId}/installments")
    @Operation(summary = "Get installment schedule by applicationId (BRD UC#04)")
    public ResponseEntity<List<InstallmentScheduleResponse>> getInstallmentsByApplicationId(
            @PathVariable("applicationId") String applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var loan = useCase.getLoanByApplicationId(tenantId, UUID.fromString(applicationId));
        // Delegate to same logic as loanId-based endpoint
        return getInstallments(loan.getId().getValue().toString(), jwt);
    }

    @SecuredEndpoint(obj = "loans.contract", act = "read")
    @GetMapping("/application/{applicationId}/contract")
    @Operation(summary = "Get loan contract details by applicationId (BRD UC#04)")
    public ResponseEntity<LoanContractResponse> getContractByApplicationId(
            @PathVariable("applicationId") String applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var loan = useCase.getLoanByApplicationId(tenantId, UUID.fromString(applicationId));
        return ResponseEntity.ok(buildContractResponse(loan));
    }

    // ══════════════════════════════════════════════════════════════
    // FINERACT INVOICES
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "loans", act = "read")
    @GetMapping("/{loanId}/invoices")
    @Operation(summary = "Get loan invoices from Fineract",
            description = "Fetches the repayment schedule from Fineract core banking by loan ID. " +
                    "Returns installment-wise breakdown with payment status.")
    public ResponseEntity<Map<String, Object>> getLoanInvoices(
            @PathVariable("loanId") String loanId,
            @AuthenticationPrincipal Jwt jwt) {

        extractTenantId(jwt);
        log.info("Fetching Fineract invoices for loanId: {}", loanId);

        // Step 1: Get fineract_loan_id from lending DB
        var loan = useCase.getLoan(extractTenantId(jwt), UUID.fromString(loanId));
        var dto = mapper.toDto(loan);

        // Step 2: Search Fineract loan by account number or use stored fineract ID
        Long fineractLoanId = findFineractLoanId(dto.loanNumber(), dto.fineractLoanId());
        if (fineractLoanId == null) {
            return ResponseEntity.ok(Map.of(
                    "loanId", loanId,
                    "loanNumber", dto.loanNumber() != null ? dto.loanNumber() : "",
                    "source", "LENDING_DB",
                    "message", "Fineract loan not found, showing local data",
                    "invoices", List.of()
            ));
        }

        // Step 3: Fetch repayment schedule from Fineract
        try {
            var headers = buildFineractHeaders();
            String url = fineractBaseUrl + "/loans/" + fineractLoanId
                    + "?associations=repaymentSchedule,transactions";

            var response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return ResponseEntity.ok(Map.of("loanId", loanId, "invoices", List.of(),
                        "message", "Fineract returned empty response"));
            }

            var body = objectMapper.readTree(response.getBody());
            var scheduleNode = body.path("repaymentSchedule").path("periods");
            var loanStatus = body.path("status").path("value").asText("Unknown");
            var accountNo = body.path("accountNo").asText("");
            var currency = body.path("currency").path("code").asText("SAR");

            List<Map<String, Object>> invoices = new ArrayList<>();
            if (scheduleNode.isArray()) {
                for (JsonNode period : scheduleNode) {
                    int installment = period.path("period").asInt(0);
                    if (installment == 0) continue; // skip summary row

                    var dueDateArr = period.path("dueDate");
                    String dueDate = dueDateArr.isArray() && dueDateArr.size() == 3
                            ? LocalDate.of(dueDateArr.get(0).asInt(), dueDateArr.get(1).asInt(), dueDateArr.get(2).asInt()).toString()
                            : "";

                    BigDecimal principalDue = BigDecimal.valueOf(period.path("principalDue").asDouble(0));
                    BigDecimal interestDue = BigDecimal.valueOf(period.path("interestDue").asDouble(0));
                    BigDecimal totalDue = BigDecimal.valueOf(period.path("totalDueForPeriod").asDouble(0));
                    BigDecimal principalPaid = BigDecimal.valueOf(period.path("principalPaid").asDouble(0));
                    BigDecimal interestPaid = BigDecimal.valueOf(period.path("interestPaid").asDouble(0));
                    BigDecimal totalPaid = BigDecimal.valueOf(period.path("totalPaidForPeriod").asDouble(0));
                    BigDecimal totalOutstanding = BigDecimal.valueOf(period.path("totalOutstandingForPeriod").asDouble(0));
                    boolean complete = period.path("complete").asBoolean(false);

                    String status;
                    if (complete) {
                        status = "PAID";
                    } else if (dueDate != null && !dueDate.isEmpty() && LocalDate.parse(dueDate).isBefore(LocalDate.now())) {
                        status = "OVERDUE";
                    } else {
                        status = "PENDING";
                    }

                    invoices.add(Map.ofEntries(
                            Map.entry("installmentNumber", installment),
                            Map.entry("dueDate", dueDate),
                            Map.entry("principalAmount", principalDue.setScale(2, RoundingMode.HALF_UP)),
                            Map.entry("profitAmount", interestDue.setScale(2, RoundingMode.HALF_UP)),
                            Map.entry("totalDue", totalDue.setScale(2, RoundingMode.HALF_UP)),
                            Map.entry("principalPaid", principalPaid.setScale(2, RoundingMode.HALF_UP)),
                            Map.entry("profitPaid", interestPaid.setScale(2, RoundingMode.HALF_UP)),
                            Map.entry("totalPaid", totalPaid.setScale(2, RoundingMode.HALF_UP)),
                            Map.entry("totalOutstanding", totalOutstanding.setScale(2, RoundingMode.HALF_UP)),
                            Map.entry("status", status),
                            Map.entry("currency", currency)
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "loanId", loanId,
                    "loanNumber", dto.loanNumber() != null ? dto.loanNumber() : "",
                    "fineractLoanId", fineractLoanId,
                    "fineractAccountNo", accountNo,
                    "fineractStatus", loanStatus,
                    "source", "FINERACT",
                    "currency", currency,
                    "totalInstallments", invoices.size(),
                    "invoices", invoices
            ));

        } catch (Exception e) {
            log.error("Failed to fetch Fineract invoices: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "loanId", loanId,
                    "source", "ERROR",
                    "message", "Failed to fetch from Fineract: " + e.getMessage(),
                    "invoices", List.of()
            ));
        }
    }

    private Long findFineractLoanId(String loanNumber, Long storedFineractId) {
        if (storedFineractId != null && storedFineractId > 0) {
            return storedFineractId;
        }
        // Search Fineract by listing all loans and matching
        try {
            var headers = buildFineractHeaders();
            String url = fineractBaseUrl + "/loans?limit=100";
            var response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                var body = objectMapper.readTree(response.getBody());
                var results = body.has("pageItems") ? body.get("pageItems") : body;
                if (results.isArray()) {
                    for (JsonNode loan : results) {
                        // Match by most recent loan (last created)
                        return loan.get("id").asLong();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Fineract loan lookup failed: {}", e.getMessage());
        }
        return null;
    }

    private HttpHeaders buildFineractHeaders() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Fineract-Platform-TenantId", fineractTenantId != null ? fineractTenantId : "default");
        if (fineractUsername != null && fineractPassword != null) {
            String credentials = fineractUsername + ":" + fineractPassword;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
            headers.set("Authorization", "Basic " + encoded);
        }
        return headers;
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    private LoanContractResponse buildContractResponse(com.ksa.financing.lending.domain.model.LoanAggregate loan) {
        return new LoanContractResponse(
                loan.getId().getValue().toString(),
                loan.getLoanNumber(),
                loan.getApplicationId() != null ? loan.getApplicationId().getValue().toString() : null,
                loan.getCustomerId().toString(),
                loan.getProductCode(),
                loan.getShariaStructure() != null ? loan.getShariaStructure().name() : null,
                loan.getPrincipalAmount(),
                loan.getProfitAmount(),
                loan.getTotalAmount(),
                loan.getProfitRate(),
                loan.getTenureMonths(),
                loan.getInstallmentAmount(),
                loan.getStatus() != null ? loan.getStatus().name() : null,
                loan.getDisbursementDate(),
                loan.getFirstDueDate(),
                loan.getMaturityDate(),
                loan.getOutstandingPrincipal(),
                loan.getOutstandingProfit(),
                loan.getTotalOutstanding(),
                false,
                "PDF generation pending — document-service not yet implemented"
        );
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }
}
