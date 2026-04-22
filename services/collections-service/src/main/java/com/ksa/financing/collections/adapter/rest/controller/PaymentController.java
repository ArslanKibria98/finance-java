package com.ksa.financing.collections.adapter.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.collections.adapter.rest.request.CompletePaymentRequest;
import com.ksa.financing.collections.adapter.rest.request.FailPaymentRequest;
import com.ksa.financing.collections.adapter.rest.request.ProcessPaymentRequest;
import com.ksa.financing.collections.adapter.rest.response.PaymentResponse;
import com.ksa.financing.collections.application.service.PaymentAutoCompleteService;
import com.ksa.financing.collections.domain.model.PaymentAggregate;
import com.ksa.financing.collections.domain.model.PaymentMethod;
import com.ksa.financing.collections.domain.model.PaymentStatus;
import com.ksa.financing.collections.domain.port.in.ManageRepaymentScheduleUseCase;
import com.ksa.financing.collections.domain.port.in.ProcessPaymentUseCase;
import com.ksa.financing.collections.domain.port.in.ProcessPaymentUseCase.*;
import com.ksa.financing.collections.infrastructure.persistence.repository.JpaPaymentRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Loan repayment payment processing")
public class PaymentController {

    private final ProcessPaymentUseCase paymentUseCase;
    private final ManageRepaymentScheduleUseCase scheduleUseCase;
    private final JpaPaymentRepository paymentRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    @Value("${app.services.lending-service-url:${LENDING_SERVICE_URL:http://lending-service:8097}}")
    private String lendingServiceUrl;

    @GetMapping("/test")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Collections service is working!");
    }

    @PostMapping
    @Operation(summary = "Initiate loan repayment payments — one PENDING payment per invoice (call /complete to settle)")
    @SecuredEndpoint(obj = "payments", act = "create")
    public ResponseEntity<List<PaymentResponse>> initiatePayment(
            @Valid @RequestBody ProcessPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID customerId = UUID.fromString(jwt.getSubject());
        UUID resolvedLoanId = resolveLoanId(tenantId, request.loanId(), jwt);
        ensureRepaymentScheduleExists(tenantId, resolvedLoanId, jwt, customerId);

        List<String> invoiceIds = request.invoiceId();
        String parentIdempotencyKey = resolveIdempotencyKey(request.idempotencyKey());
        List<BigDecimal> perInvoiceAmounts = splitAmount(request.amount(), invoiceIds.size());
        List<PaymentResponse> responses = new ArrayList<>(invoiceIds.size());

        for (int i = 0; i < invoiceIds.size(); i++) {
            String invoiceId = invoiceIds.get(i);
            BigDecimal invoiceAmount = perInvoiceAmounts.get(i);
            String perInvoiceIdempotencyKey = invoiceIds.size() == 1
                    ? parentIdempotencyKey
                    : parentIdempotencyKey + ":" + invoiceId;

            UUID installmentId = null;
            if (i == 0 && request.installmentId() != null) {
                installmentId = request.installmentId();
            } else {
                installmentId = lookupInstallmentIdByInvoiceId(tenantId, resolvedLoanId, invoiceId);
            }

            var initiateCommand = new InitiatePaymentCommand(
                    tenantId,
                    resolvedLoanId,
                    installmentId,
                    customerId,
                    invoiceAmount,
                    request.paymentMethod(),
                    invoiceId,
                    LocalDate.now(),
                    null,
                    perInvoiceIdempotencyKey
            );

            var payment = paymentUseCase.initiatePayment(initiateCommand);
            log.info("Payment initiated (PENDING): paymentId={} loanId={} invoiceId={} amount={}",
                    payment.getId().getValue(), resolvedLoanId, invoiceId, invoiceAmount);

            // Simulate async provider webhook for HyperPay flows: auto-complete after 5 seconds.
            // Only schedule for PENDING payments — if idempotent lookup returned an already
            // COMPLETED/FAILED payment, skip (the domain state machine would reject it anyway).
            if (isHyperPayMethod(request.paymentMethod()) && payment.getStatus() == PaymentStatus.PENDING) {
                scheduleAutoComplete(tenantId, payment.getId().getValue());
            }

            responses.add(toResponse(payment));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    private List<BigDecimal> splitAmount(BigDecimal total, int parts) {
        List<BigDecimal> amounts = new ArrayList<>(parts);
        if (parts == 1) {
            amounts.add(total);
            return amounts;
        }
        BigDecimal share = total.divide(BigDecimal.valueOf(parts), 2, RoundingMode.HALF_UP);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < parts - 1; i++) {
            amounts.add(share);
            allocated = allocated.add(share);
        }
        amounts.add(total.subtract(allocated).setScale(2, RoundingMode.HALF_UP));
        return amounts;
    }

    private void scheduleAutoComplete(UUID tenantId, UUID paymentId) {
        log.info("⏳ Scheduling auto-complete for paymentId={} in 5 seconds", paymentId);

        scheduler.schedule(() -> {
            try {
                String providerTxn = "AUTO-" + System.currentTimeMillis();
                var command = new ProcessPaymentUseCase.CompletePaymentCommand(tenantId, paymentId, providerTxn);
                var result = paymentUseCase.completePayment(command);
                log.info("✅ Auto-completed payment: paymentId={} providerTxn={}", paymentId, providerTxn);
            } catch (Exception e) {
                log.error("❌ Auto-complete failed for paymentId={}: {}", paymentId, e.getMessage(), e);
            }
        }, 5, TimeUnit.SECONDS);
    }

    private boolean isHyperPayMethod(PaymentMethod method) {
        return method == PaymentMethod.HYPERPAY_MADA
                || method == PaymentMethod.HYPERPAY_VISA
                || method == PaymentMethod.HYPERPAY_MASTERCARD
                || method == PaymentMethod.HYPERPAY_APPLE_PAY;
    }

    @PostMapping("/{paymentId}/complete")
    @Operation(summary = "Complete a payment (callback from payment provider)")
    @SecuredEndpoint(obj = "payments", act = "update")
    public ResponseEntity<PaymentResponse> completePayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody CompletePaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        var command = new CompletePaymentCommand(tenantId, paymentId, request.providerTransactionId());
        var result = paymentUseCase.completePayment(command);
        log.info("Payment completed: paymentId={}", paymentId);
        return ResponseEntity.ok(toResponse(result.payment()));
    }

    @PostMapping("/{paymentId}/fail")
    @Operation(summary = "Mark a payment as failed")
    @SecuredEndpoint(obj = "payments", act = "update")
    public ResponseEntity<PaymentResponse> failPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody FailPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        var command = new FailPaymentCommand(tenantId, paymentId, request.failureCode(), request.failureMessage());
        var payment = paymentUseCase.failPayment(command);
        log.info("Payment failed: paymentId={} code={}", paymentId, request.failureCode());
        return ResponseEntity.ok(toResponse(payment));
    }

    @PostMapping("/{paymentId}/reverse")
    @Operation(summary = "Reverse a completed payment")
    @SecuredEndpoint(obj = "payments", act = "update")
    public ResponseEntity<PaymentResponse> reversePayment(
            @PathVariable UUID paymentId,
            @RequestParam String reason,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        var command = new ReversePaymentCommand(tenantId, paymentId, reason);
        var payment = paymentUseCase.reversePayment(command);
        log.info("Payment reversed: paymentId={}", paymentId);
        return ResponseEntity.ok(toResponse(payment));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get a payment by ID")
    @SecuredEndpoint(obj = "payments", act = "read")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var payment = paymentUseCase.getPayment(tenantId, paymentId);
        return ResponseEntity.ok(toResponse(payment));
    }

    @GetMapping("/{paymentId}/status")
    @Operation(summary = "Get payment status only")
    @SecuredEndpoint(obj = "payments", act = "read")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var payment = paymentUseCase.getPayment(tenantId, paymentId);

        return ResponseEntity.ok(Map.of(
                "paymentId", payment.getId().getValue(),
                "status", payment.getStatus(),
                "amount", payment.getAmount(),
                "message", payment.getStatus().name()
        ));
    }

    @PostMapping("/{paymentId}/auto-complete-mock")
    @Operation(summary = "Auto-complete payment after 2 seconds (TESTING ONLY)")
    @SecuredEndpoint(obj = "payments", act = "update")
    public ResponseEntity<java.util.Map<String, String>> autoCompleteMock(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        // Schedule completion after 2 seconds in background thread (for testing only)
        new Thread(() -> {
            try {
                Thread.sleep(2000);  // 2 seconds

                String providerTxn = "MOCK-TXN-" + System.currentTimeMillis();

                // Step 1: Mark payment as COMPLETED (using JdbcTemplate - no transaction needed)
                int paymentUpdated = jdbcTemplate.update(
                    "UPDATE payments SET status = 'COMPLETED', provider_transaction_id = ?, updated_at = NOW() " +
                    "WHERE id = ? AND tenant_id = ?",
                    providerTxn, paymentId, tenantId);

                if (paymentUpdated > 0) {
                    log.info("✅ Mock: Payment COMPLETED: {}", paymentId);

                    // Step 2: Mark linked installment as PAID
                    int installmentUpdated = jdbcTemplate.update(
                        "UPDATE installments SET status = 'PAID', paid_date = CURRENT_DATE, updated_at = NOW() " +
                        "WHERE id = (SELECT installment_id FROM payments WHERE id = ? AND tenant_id = ?) " +
                        "AND tenant_id = ?",
                        paymentId, tenantId, tenantId);

                    if (installmentUpdated > 0) {
                        log.info("✅ Mock: Installment marked PAID");
                    } else {
                        log.warn("⚠️  Mock: No installment found to mark PAID");
                    }
                } else {
                    log.warn("⚠️  Mock: Payment not found or not updated: {}", paymentId);
                }
            } catch (Exception e) {
                log.error("❌ Mock: Auto-complete failed for paymentId={}: {}", paymentId, e.getMessage(), e);
            }
        }).start();

        log.info("⏳ Mock: Scheduled auto-complete for payment: {} in 2 seconds", paymentId);
        return ResponseEntity.accepted().body(java.util.Map.of(
            "message", "Payment will auto-complete in 2 seconds",
            "paymentId", paymentId.toString(),
            "status", "PROCESSING"
        ));
    }

    private UUID lookupInstallmentIdByInvoiceId(UUID tenantId, UUID loanId, String invoiceId) {
        try {
            Object result = entityManager.createNativeQuery(
                    "SELECT i.id " +
                            "FROM installments i " +
                            "JOIN repayment_schedules rs ON rs.id = i.schedule_id " +
                            "WHERE i.tenant_id = ?1 AND rs.loan_id = ?2 AND rs.is_active = true AND i.invoice_id = ?3 " +
                            "LIMIT 1")
                .setParameter(1, tenantId)
                .setParameter(2, loanId)
                .setParameter(3, invoiceId)
                .getSingleResult();

            if (result instanceof UUID) {
                log.debug("✅ Auto-resolved installmentId from invoiceId: {}", invoiceId);
                return (UUID) result;
            }
        } catch (Exception e) {
            log.debug("⚠️ Could not auto-resolve installmentId for invoice {}: {}", invoiceId, e.getMessage());
        }

        Integer installmentNumber = extractInstallmentNumber(invoiceId);
        if (installmentNumber != null) {
            try {
                Object result = entityManager.createNativeQuery(
                                "SELECT i.id " +
                                        "FROM installments i " +
                                        "JOIN repayment_schedules rs ON rs.id = i.schedule_id " +
                                        "WHERE i.tenant_id = ?1 AND rs.loan_id = ?2 AND rs.is_active = true AND i.installment_number = ?3 " +
                                        "LIMIT 1")
                        .setParameter(1, tenantId)
                        .setParameter(2, loanId)
                        .setParameter(3, installmentNumber)
                        .getSingleResult();
                if (result instanceof UUID) {
                    log.debug("✅ Resolved installmentId by installmentNumber={} for invoice={}", installmentNumber, invoiceId);
                    return (UUID) result;
                }
            } catch (Exception ignored) {
                log.debug("⚠️ Could not resolve installmentId by installmentNumber for invoice {}", invoiceId);
            }
        }
        return null;
    }

    private Integer extractInstallmentNumber(String invoiceId) {
        if (invoiceId == null) {
            return null;
        }
        int idx = invoiceId.lastIndexOf('-');
        if (idx < 0 || idx == invoiceId.length() - 1) {
            return null;
        }
        try {
            return Integer.parseInt(invoiceId.substring(idx + 1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private UUID resolveLoanId(UUID tenantId, UUID providedLoanId, Jwt jwt) {
        if (hasActiveScheduleForLoan(tenantId, providedLoanId)) {
            return providedLoanId;
        }
        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            var response = restTemplate.exchange(
                    lendingServiceUrl + "/api/v1/loans/application/" + providedLoanId + "/contract",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return providedLoanId;
            }
            var root = objectMapper.readTree(response.getBody());
            var resolved = root.path("data").path("loanId").asText(null);
            if (resolved != null && !resolved.isBlank()) {
                return UUID.fromString(resolved);
            }
        } catch (Exception ex) {
            log.debug("Loan ID resolution skipped for {}: {}", providedLoanId, ex.getMessage());
        }
        return providedLoanId;
    }

    private void ensureRepaymentScheduleExists(UUID tenantId, UUID loanId, Jwt jwt, UUID userId) {
        if (hasActiveScheduleForLoan(tenantId, loanId)) {
            return;
        }
        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            var response = restTemplate.exchange(
                    lendingServiceUrl + "/api/v1/loans/" + loanId + "/installments",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return;
            }

            var data = objectMapper.readTree(response.getBody()).path("data");
            if (!data.isArray() || data.isEmpty()) {
                return;
            }

            var entries = new ArrayList<ManageRepaymentScheduleUseCase.CreateScheduleCommand.InstallmentEntry>();
            BigDecimal totalPrincipal = BigDecimal.ZERO;
            BigDecimal totalProfit = BigDecimal.ZERO;
            LocalDate firstDueDate = null;
            LocalDate lastDueDate = null;

            for (var item : data) {
                int number = item.path("installmentNumber").asInt();
                LocalDate dueDate = LocalDate.parse(item.path("dueDate").asText());
                BigDecimal principal = item.path("principalComponent").decimalValue();
                BigDecimal profit = item.path("profitComponent").decimalValue();

                entries.add(new ManageRepaymentScheduleUseCase.CreateScheduleCommand.InstallmentEntry(
                        number, dueDate, principal, profit, BigDecimal.ZERO));
                totalPrincipal = totalPrincipal.add(principal);
                totalProfit = totalProfit.add(profit);
                if (firstDueDate == null || dueDate.isBefore(firstDueDate)) firstDueDate = dueDate;
                if (lastDueDate == null || dueDate.isAfter(lastDueDate)) lastDueDate = dueDate;
            }

            if (entries.isEmpty() || firstDueDate == null || lastDueDate == null) {
                return;
            }

            UUID productId = fetchProductIdForLoan(loanId, jwt);

            var cmd = new ManageRepaymentScheduleUseCase.CreateScheduleCommand(
                    tenantId,
                    loanId,
                    productId,
                    "SCH-" + loanId.toString().substring(0, 8).toUpperCase() + "-001",
                    totalPrincipal,
                    totalProfit,
                    firstDueDate,
                    lastDueDate,
                    entries,
                    userId
            );
            scheduleUseCase.createSchedule(cmd);
            log.info("Auto-created repayment schedule for loanId={} productId={} from lending-service",
                    loanId, productId);
        } catch (Exception ex) {
            log.warn("Unable to auto-create schedule for loanId={}: {}", loanId, ex.getMessage());
        }
    }

    /**
     * Best-effort productId fetch from lending-service so the schedule can carry
     * the product key for per-product DelinquencyRule lookups. Returns null on
     * any failure — the engine falls back to tenant-default rules.
     */
    private UUID fetchProductIdForLoan(UUID loanId, Jwt jwt) {
        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            var response = restTemplate.exchange(
                    lendingServiceUrl + "/api/v1/loans/" + loanId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }
            var root = objectMapper.readTree(response.getBody()).path("data");
            var productIdText = root.path("productId").asText(null);
            if (productIdText == null || productIdText.isBlank()) {
                productIdText = root.path("product").path("id").asText(null);
            }
            return productIdText != null && !productIdText.isBlank() ? UUID.fromString(productIdText) : null;
        } catch (Exception ex) {
            log.debug("productId fetch skipped for loan {}: {}", loanId, ex.getMessage());
            return null;
        }
    }

    private boolean hasActiveScheduleForLoan(UUID tenantId, UUID loanId) {
        try {
            Object result = entityManager.createNativeQuery(
                            "SELECT COUNT(1) FROM repayment_schedules WHERE tenant_id = ?1 AND loan_id = ?2 AND is_active = true")
                    .setParameter(1, tenantId)
                    .setParameter(2, loanId)
                    .getSingleResult();
            return result instanceof Number && ((Number) result).longValue() > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException("COMMON.AUTH.ACCESS_DENIED", "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantId);
    }

    private String resolveIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return idempotencyKey;
    }

    private PaymentResponse toResponse(PaymentAggregate payment) {
        return new PaymentResponse(
                payment.getId().getValue(),
                payment.getLoanId(),
                payment.getInstallmentId(),
                payment.getInvoiceId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getIdempotencyKey(),
                payment.getSourceReference(),
                payment.getProviderTransactionId(),
                payment.getFailureCode(),
                payment.getFailureMessage(),
                payment.getCreatedAt(),
                payment.getStatus() == com.ksa.financing.collections.domain.model.PaymentStatus.COMPLETED
                        ? payment.getUpdatedAt() : null
        );
    }
}
