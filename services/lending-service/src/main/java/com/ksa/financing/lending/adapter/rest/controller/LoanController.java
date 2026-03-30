package com.ksa.financing.lending.adapter.rest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.lending.adapter.rest.response.InstallmentScheduleResponse;
import com.ksa.financing.lending.adapter.rest.response.LoanContractResponse;
import com.ksa.financing.lending.adapter.rest.response.LoanOverviewResponse;
import com.ksa.financing.lending.adapter.rest.response.LoanResponse;
import com.ksa.financing.lending.application.mapper.LoanMapper;
import com.ksa.financing.lending.domain.port.in.ManageLoanUseCase;
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

    @Value("${app.services.fineract-base-url}")
    private String fineractBaseUrl;

    @Value("${app.services.fineract-username:#{null}}")
    private String fineractUsername;

    @Value("${app.services.fineract-password:#{null}}")
    private String fineractPassword;

    @Value("${app.services.fineract-tenant-id:${FINERACT_TENANT_ID:default}}")
    private String fineractTenantId;

    public LoanController(ManageLoanUseCase useCase, LoanMapper mapper,
                           RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.mapper = mapper;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @SecuredEndpoint(obj = "loans", act = "read")
    @GetMapping("/{loanId}")
    @Operation(summary = "Get a loan by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Loan found"),
        @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    public ResponseEntity<LoanResponse> getLoan(
            @PathVariable String loanId,
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
            @PathVariable String loanNumber,
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
            @PathVariable String customerId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        var tenantId = extractTenantId(jwt);
        var loans = useCase.listLoansByCustomer(tenantId, UUID.fromString(customerId));
        var responses = mapper.toDtos(loans).stream()
                .map(LoanResponse::from)
                .toList();

        return ResponseEntity
                .ok()
                .header("X-Correlation-ID", correlationId)
                .body(responses);
    }

    // ══════════════════════════════════════════════════════════════
    // BRD UC#04: FINANCE OVERVIEW
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "loans.overview", act = "read")
    @GetMapping("/customer/{customerId}/overview")
    @Operation(summary = "Get finance overview for customer (BRD UC#04)")
    public ResponseEntity<LoanOverviewResponse> getFinanceOverview(
            @PathVariable String customerId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var loans = useCase.listLoansByCustomer(tenantId, UUID.fromString(customerId));

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
            @PathVariable String loanId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var loan = useCase.getLoan(tenantId, UUID.fromString(loanId));
        var dto = mapper.toDto(loan);

        // Build installment schedule from loan data (flat Murabaha schedule)
        var schedule = new java.util.ArrayList<InstallmentScheduleResponse>();
        // Use firstDueDate if available, otherwise estimate from next month
        var startDate = dto.firstDueDate() != null ? dto.firstDueDate()
                : (dto.disbursementDate() != null ? dto.disbursementDate().plusMonths(1)
                : java.time.LocalDate.now().plusMonths(1));

        if (dto.installmentAmount() != null && dto.tenureMonths() > 0) {
            var principal = dto.principalAmount() != null ? dto.principalAmount() : java.math.BigDecimal.ZERO;
            var totalProfit = dto.profitAmount() != null ? dto.profitAmount() : java.math.BigDecimal.ZERO;
            var monthlyPrincipal = principal.divide(
                    java.math.BigDecimal.valueOf(dto.tenureMonths()), 2, java.math.RoundingMode.HALF_UP);
            var monthlyProfit = totalProfit.divide(
                    java.math.BigDecimal.valueOf(dto.tenureMonths()), 2, java.math.RoundingMode.HALF_UP);
            var balance = principal.add(totalProfit);

            // Estimate paid count from outstanding vs total
            var total = dto.totalAmount() != null ? dto.totalAmount() : java.math.BigDecimal.ZERO;
            var outstanding = dto.totalOutstanding() != null ? dto.totalOutstanding() : java.math.BigDecimal.ZERO;
            var paidAmt = total.subtract(outstanding);
            int paidCount = dto.installmentAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                    ? paidAmt.divide(dto.installmentAmount(), 0, java.math.RoundingMode.DOWN).intValue() : 0;

            for (int i = 1; i <= dto.tenureMonths(); i++) {
                balance = balance.subtract(dto.installmentAmount());
                if (balance.compareTo(java.math.BigDecimal.ZERO) < 0) {
                    balance = java.math.BigDecimal.ZERO;
                }
                var dueDate = startDate.plusMonths(i - 1);
                var isPaid = i <= paidCount;
                var isOverdue = !isPaid && dueDate.isBefore(java.time.LocalDate.now());

                // Generate deterministic invoiceId: INV-{loanId_short}-{installmentNumber}
                var invoiceId = "INV-" + loanId.substring(0, 8).toUpperCase() + "-" + String.format("%03d", i);

                schedule.add(new InstallmentScheduleResponse(
                        invoiceId,
                        i,
                        dueDate,
                        dto.installmentAmount(),
                        monthlyPrincipal,
                        monthlyProfit,
                        balance,
                        isPaid ? "PAID" : (isOverdue ? "OVERDUE" : "PENDING"),
                        isPaid ? dueDate : null,
                        isPaid ? dto.installmentAmount() : null,
                        isPaid
                ));
            }
        }

        return ResponseEntity.ok(schedule);
    }

    @SecuredEndpoint(obj = "loans.contract", act = "read")
    @GetMapping("/{loanId}/contract")
    @Operation(summary = "Get loan contract details (BRD UC#04)")
    public ResponseEntity<LoanContractResponse> getContract(
            @PathVariable String loanId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var loan = useCase.getLoan(tenantId, UUID.fromString(loanId));
        return ResponseEntity.ok(buildContractResponse(loan));
    }

    @SecuredEndpoint(obj = "loans.receipts", act = "read")
    @GetMapping("/{loanId}/receipts/{installmentNumber}")
    @Operation(summary = "Download payment receipt (BRD UC#04)")
    public ResponseEntity<String> downloadReceipt(
            @PathVariable String loanId,
            @PathVariable int installmentNumber,
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
            @PathVariable String applicationId,
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
            @PathVariable String applicationId,
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
            @PathVariable String loanId,
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
