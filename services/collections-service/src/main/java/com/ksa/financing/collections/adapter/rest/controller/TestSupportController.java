package com.ksa.financing.collections.adapter.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.collections.application.service.DelinquencyEngine;
import com.ksa.financing.collections.domain.model.Installment;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.port.in.ManageRepaymentScheduleUseCase;
import com.ksa.financing.collections.domain.port.in.ManageRepaymentScheduleUseCase.CreateScheduleCommand;
import com.ksa.financing.collections.domain.port.in.ManageRepaymentScheduleUseCase.CreateScheduleCommand.InstallmentEntry;
import com.ksa.financing.collections.domain.port.out.RepaymentScheduleRepository;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-only endpoints for shifting an installment's due_date and re-running
 * {@link DelinquencyEngine#tickAndAssess}. Auto-seeds the schedule from
 * lending-service if it doesn't yet exist in collections-service (schedules
 * are normally created lazily on the first payment).
 *
 * Guarded by delinquency.rules:manage.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Installment Date Override",
        description = "Shift an installment's due date and re-run delinquency. Requires delinquency.rules:manage.")
public class TestSupportController {

    private final RepaymentScheduleRepository scheduleRepository;
    private final ManageRepaymentScheduleUseCase scheduleUseCase;
    private final DelinquencyEngine delinquencyEngine;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.services.lending-service-url:${LENDING_SERVICE_URL:http://lending-service:8097}}")
    private String lendingServiceUrl;

    @PatchMapping("/invoices/{invoiceId}/due-date")
    @Operation(summary = "Shift an installment's due_date by its invoice_id and re-run delinquency. "
            + "Auto-seeds the schedule from lending-service if not yet in collections-service.")
    @SecuredEndpoint(obj = "delinquency.rules", act = "manage")
    @Transactional
    public ResponseEntity<Map<String, Object>> shiftByInvoiceId(
            @PathVariable("invoiceId") String invoiceId,
            @Valid @RequestBody ShiftByInvoiceRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        // Always consult lending's /lookup/by-invoice so we get BOTH loanId and productId
        // in one round-trip. Without productId, the LATE_PAYMENT rule lookup returns empty
        // and no penalty accrues on auto-seeded schedules.
        var lookup = lookupFromLending(invoiceId, jwt);
        UUID loanId = lookup != null ? lookup.loanId() : null;
        UUID productId = lookup != null ? lookup.productId() : null;

        if (loanId == null) loanId = req.loanId();
        if (loanId == null) loanId = extractLoanIdFromInvoice(invoiceId, tenantId);
        if (loanId == null) {
            throw NotFoundException.forEntity("Invoice", invoiceId);
        }

        ensureScheduleExists(tenantId, loanId, productId, jwt);
        if (productId != null) {
            backfillScheduleProductId(tenantId, loanId, productId);
        }

        // Try the real invoice_id column first; if not populated (collections seeds
        // installments without invoice_id), fall back to (loan_id, installment_number)
        // parsed from the INV-XXXXXXXX-NNN suffix.
        UUID installmentId = findByInvoiceIdColumn(tenantId, invoiceId);
        if (installmentId == null) {
            Integer installmentNumber = extractInstallmentNumber(invoiceId);
            if (installmentNumber == null) {
                throw NotFoundException.forEntity("Installment(invoiceId)", invoiceId);
            }
            installmentId = findByLoanAndNumber(tenantId, loanId, installmentNumber);
        }
        if (installmentId == null) {
            throw NotFoundException.forEntity("Installment(invoiceId)", invoiceId);
        }

        return shift(tenantId, loanId, installmentId, req.newDueDate());
    }

    private UUID findByInvoiceIdColumn(UUID tenantId, String invoiceId) {
        try {
            return (UUID) entityManager.createNativeQuery(
                            "SELECT i.id FROM installments i " +
                                    "JOIN repayment_schedules rs ON rs.id = i.schedule_id " +
                                    "WHERE i.tenant_id = ?1 AND i.invoice_id = ?2 AND rs.is_active = true " +
                                    "LIMIT 1")
                    .setParameter(1, tenantId)
                    .setParameter(2, invoiceId)
                    .getSingleResult();
        } catch (jakarta.persistence.NoResultException ex) {
            return null;
        }
    }

    private UUID findByLoanAndNumber(UUID tenantId, UUID loanId, int installmentNumber) {
        try {
            return (UUID) entityManager.createNativeQuery(
                            "SELECT i.id FROM installments i " +
                                    "JOIN repayment_schedules rs ON rs.id = i.schedule_id " +
                                    "WHERE i.tenant_id = ?1 AND rs.loan_id = ?2 AND rs.is_active = true AND i.installment_number = ?3 " +
                                    "LIMIT 1")
                    .setParameter(1, tenantId)
                    .setParameter(2, loanId)
                    .setParameter(3, installmentNumber)
                    .getSingleResult();
        } catch (jakarta.persistence.NoResultException ex) {
            return null;
        }
    }

    private Integer extractInstallmentNumber(String invoiceId) {
        var m = java.util.regex.Pattern.compile("^INV-[0-9A-Fa-f]{8}-(\\d{1,5})$").matcher(invoiceId);
        return m.matches() ? Integer.parseInt(m.group(1)) : null;
    }

    @PatchMapping("/installments/{installmentId}/due-date")
    @Operation(summary = "Shift an installment's due_date by UUID.")
    @SecuredEndpoint(obj = "delinquency.rules", act = "manage")
    @Transactional
    public ResponseEntity<Map<String, Object>> shiftByInstallmentId(
            @PathVariable("installmentId") UUID installmentId,
            @Valid @RequestBody ShiftByInstallmentRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        ensureScheduleExists(tenantId, req.loanId(), null, jwt);
        return shift(tenantId, req.loanId(), installmentId, req.newDueDate());
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> shift(UUID tenantId, UUID loanId, UUID installmentId, LocalDate newDueDate) {
        RepaymentScheduleAggregate schedule = scheduleRepository.findActiveByLoanId(tenantId, loanId)
                .orElseThrow(() -> NotFoundException.forEntity("RepaymentSchedule", loanId.toString()));

        Installment target = schedule.getInstallments().stream()
                .filter(i -> i.getId().equals(installmentId))
                .findFirst()
                .orElseThrow(() -> NotFoundException.forEntity("Installment", installmentId.toString()));

        LocalDate previousDueDate = target.getDueDate();
        target.changeDueDate(newDueDate);

        var assessment = delinquencyEngine.tickAndAssess(tenantId, null, schedule, LocalDate.now());
        scheduleRepository.save(schedule);

        log.info("Installment {} due_date {}→{} loanDpd={} stage={} latePenalty={}",
                installmentId, previousDueDate, newDueDate,
                assessment.loanDpd(), assessment.loanStage(), target.getLatePenaltyAmount());

        var response = new LinkedHashMap<String, Object>();
        response.put("installmentId", installmentId);
        response.put("loanId", loanId);
        response.put("previousDueDate", previousDueDate);
        response.put("newDueDate", newDueDate);
        response.put("newStatus", target.getStatus());
        response.put("installmentDpd", target.getDpd());
        response.put("latePenaltyAmount", target.getLatePenaltyAmount());
        response.put("loanDpd", assessment.loanDpd());
        response.put("loanStage", assessment.loanStage());
        response.put("policySource", assessment.policySource());
        return ResponseEntity.ok(response);
    }

    /**
     * If no active schedule exists for this loan in collections-service, fetch its
     * installments from lending-service and create one. Mirrors the same lazy-seed
     * flow that PaymentController runs before the first payment.
     */
    private void ensureScheduleExists(UUID tenantId, UUID loanId, UUID productId, Jwt jwt) {
        if (scheduleRepository.findActiveByLoanId(tenantId, loanId).isPresent()) {
            return;
        }

        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());

            var installmentsResp = restTemplate.exchange(
                    lendingServiceUrl + "/api/v1/loans/" + loanId + "/installments",
                    HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (!installmentsResp.getStatusCode().is2xxSuccessful() || installmentsResp.getBody() == null) {
                throw NotFoundException.forEntity("Loan-installments-from-lending", loanId.toString());
            }

            var data = objectMapper.readTree(installmentsResp.getBody()).path("data");
            if (!data.isArray() || data.isEmpty()) {
                throw NotFoundException.forEntity("Loan-installments-from-lending", loanId.toString());
            }

            UUID effectiveProductId = productId != null ? productId : fetchProductId(loanId, jwt);

            var entries = new ArrayList<InstallmentEntry>();
            BigDecimal totalPrincipal = BigDecimal.ZERO;
            BigDecimal totalProfit = BigDecimal.ZERO;
            BigDecimal totalFee = BigDecimal.ZERO;
            LocalDate firstDueDate = null;
            LocalDate lastDueDate = null;

            for (var item : data) {
                int number = item.path("installmentNumber").asInt();
                LocalDate dueDate = LocalDate.parse(item.path("dueDate").asText());
                BigDecimal principal = item.path("principalComponent").decimalValue();
                BigDecimal profit = item.path("profitComponent").decimalValue();
                var feeNode = item.path("feeComponent");
                BigDecimal fee = feeNode.isMissingNode() || feeNode.isNull()
                        ? BigDecimal.ZERO : feeNode.decimalValue();

                entries.add(new InstallmentEntry(number, dueDate, principal, profit, fee));
                totalPrincipal = totalPrincipal.add(principal);
                totalProfit = totalProfit.add(profit);
                totalFee = totalFee.add(fee);
                if (firstDueDate == null || dueDate.isBefore(firstDueDate)) firstDueDate = dueDate;
                if (lastDueDate == null || dueDate.isAfter(lastDueDate)) lastDueDate = dueDate;
            }

            var cmd = new CreateScheduleCommand(
                    tenantId,
                    loanId,
                    effectiveProductId,
                    "SCH-" + loanId.toString().substring(0, 8).toUpperCase() + "-001",
                    totalPrincipal, totalProfit, totalFee,
                    firstDueDate, lastDueDate,
                    entries,
                    UUID.fromString(jwt.getSubject()));
            scheduleUseCase.createSchedule(cmd);
            log.info("Auto-seeded schedule for loanId={} productId={} installments={}",
                    loanId, effectiveProductId, entries.size());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR,
                    "Failed to auto-seed schedule from lending-service: " + ex.getMessage());
        }
    }

    private UUID fetchProductId(UUID loanId, Jwt jwt) {
        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            var response = restTemplate.exchange(
                    lendingServiceUrl + "/api/v1/loans/" + loanId,
                    HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return null;
            var loanNode = objectMapper.readTree(response.getBody()).path("data");
            var productIdText = loanNode.path("productId").asText(null);
            if (productIdText == null || productIdText.isBlank()) {
                productIdText = loanNode.path("product").path("id").asText(null);
            }
            return productIdText != null && !productIdText.isBlank() ? UUID.fromString(productIdText) : null;
        } catch (Exception ex) {
            log.debug("productId fetch skipped for loan {}: {}", loanId, ex.getMessage());
            return null;
        }
    }

    /**
     * Resolves both loanId and productId from lending-service's /lookup/by-invoice endpoint.
     * productId is critical — without it the LATE_PAYMENT DelinquencyRule cannot be found
     * and no penalty accrues during tickAndAssess.
     */
    private LendingLookup lookupFromLending(String invoiceId, Jwt jwt) {
        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            var response = restTemplate.exchange(
                    lendingServiceUrl + "/api/v1/loans/lookup/by-invoice/" + invoiceId,
                    HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return null;
            var node = objectMapper.readTree(response.getBody());
            var payload = node.has("data") ? node.get("data") : node;
            var loanIdText = payload.path("loanId").asText(null);
            var productIdText = payload.path("productId").asText(null);
            UUID loanId = (loanIdText != null && !loanIdText.isBlank()) ? UUID.fromString(loanIdText) : null;
            UUID productId = (productIdText != null && !productIdText.isBlank()) ? UUID.fromString(productIdText) : null;
            return new LendingLookup(loanId, productId);
        } catch (Exception ex) {
            log.debug("Lending lookup for invoice {} failed: {}", invoiceId, ex.getMessage());
            return null;
        }
    }

    private record LendingLookup(UUID loanId, UUID productId) {}

    /**
     * If an existing schedule has product_id=null (seeded before we had it), stamp it now
     * so DelinquencyEngine's LATE_PAYMENT rule lookup works on this tick.
     */
    private void backfillScheduleProductId(UUID tenantId, UUID loanId, UUID productId) {
        try {
            int updated = entityManager.createNativeQuery(
                            "UPDATE repayment_schedules SET product_id = ?1 " +
                                    "WHERE tenant_id = ?2 AND loan_id = ?3 AND is_active = true AND product_id IS NULL")
                    .setParameter(1, productId)
                    .setParameter(2, tenantId)
                    .setParameter(3, loanId)
                    .executeUpdate();
            if (updated > 0) {
                log.info("Backfilled product_id={} on {} schedule(s) for loanId={}", productId, updated, loanId);
            }
        } catch (Exception ex) {
            log.debug("product_id backfill skipped for loanId={}: {}", loanId, ex.getMessage());
        }
    }

    /**
     * Fast local reverse lookup: scans collections-service's own schedules for an active
     * loan whose id prefix matches the invoice. Returns null if absent/ambiguous — the
     * caller then falls back to {@link #lookupLoanIdFromLending}.
     */
    private UUID extractLoanIdFromInvoice(String invoiceId, UUID tenantId) {
        var match = java.util.regex.Pattern.compile("^INV-([0-9A-Fa-f]{8})-\\d+$").matcher(invoiceId);
        if (!match.matches()) return null;
        String prefix = match.group(1).toLowerCase() + "%";
        try {
            @SuppressWarnings("unchecked")
            List<UUID> results = entityManager.createNativeQuery(
                            "SELECT DISTINCT loan_id FROM repayment_schedules " +
                                    "WHERE tenant_id = ?1 AND is_active = true AND CAST(loan_id AS text) LIKE ?2")
                    .setParameter(1, tenantId)
                    .setParameter(2, prefix)
                    .getResultList();
            return results.size() == 1 ? results.get(0) : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantId);
    }

    public record ShiftByInvoiceRequest(
            UUID loanId,            // optional — inferred from invoice_id prefix when schedule exists
            @NotNull LocalDate newDueDate
    ) {}

    public record ShiftByInstallmentRequest(
            @NotNull UUID loanId,
            @NotNull LocalDate newDueDate
    ) {}
}
