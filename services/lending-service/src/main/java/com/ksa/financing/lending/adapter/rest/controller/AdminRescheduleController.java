package com.ksa.financing.lending.adapter.rest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.lending.infrastructure.persistence.entity.LoanRescheduleJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanRepository;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanRescheduleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Admin-only endpoint for complete reschedule history across all loans.
 *
 * GET /api/v1/admin/loan-reschedules                              — all reschedules (tenant-scoped)
 * GET /api/v1/admin/loan-reschedules?status=PENDING               — filter by status
 * GET /api/v1/admin/loan-reschedules?type=TENURE_EXTENSION        — filter by type
 * GET /api/v1/admin/loan-reschedules/{rescheduleId}               — single reschedule full detail
 * GET /api/v1/admin/loan-reschedules/by-application/{applicationId} — all reschedules for an application
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/loan-reschedules")
@RequiredArgsConstructor
@Tag(name = "Admin - Reschedule History", description = "Admin view: complete reschedule audit trail")
public class AdminRescheduleController {

    private final JpaLoanRescheduleRepository rescheduleRepository;
    private final JpaLoanRepository loanRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    @SecuredEndpoint(obj = "admin.loan-reschedules", act = "read")
    @Operation(summary = "List all reschedule requests (admin). Filter by status, type, or free-text search.")
    public ResponseEntity<JsonNode> listAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        List<LoanRescheduleJpaEntity> records;
        if (status != null && !status.isBlank()) {
            records = rescheduleRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status.toUpperCase());
        } else if (type != null && !type.isBlank()) {
            records = rescheduleRepository.findByTenantIdAndRescheduleTypeOrderByCreatedAtDesc(tenantId, type.toUpperCase());
        } else {
            records = rescheduleRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        }

        String searchTerm = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        if (searchTerm != null) {
            records = records.stream().filter(r -> matchesSearch(r, searchTerm)).toList();
        }

        ArrayNode array = objectMapper.createArrayNode();
        for (var r : records) {
            array.add(buildRecord(r));
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("total", records.size());
        response.put("filter_status", status);
        response.put("filter_type", type);
        response.put("filter_search", search);
        response.set("reschedules", array);

        return ResponseEntity.ok(response);
    }

    private boolean matchesSearch(LoanRescheduleJpaEntity r, String term) {
        return contains(r.getLoanNumber(), term)
                || contains(r.getRescheduleType(), term)
                || contains(r.getStatus(), term)
                || contains(r.getJustification(), term);
    }

    private boolean contains(String f, String term) {
        return f != null && f.toLowerCase().contains(term);
    }

    @GetMapping("/{rescheduleId}")
    @SecuredEndpoint(obj = "admin.loan-reschedules", act = "read")
    @Operation(summary = "Get full detail for a single reschedule (admin)")
    public ResponseEntity<JsonNode> getDetail(
            @PathVariable UUID rescheduleId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        var record = rescheduleRepository.findById(rescheduleId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND,
                        "Reschedule not found: " + rescheduleId));

        return ResponseEntity.ok(buildRecord(record));
    }

    @GetMapping("/by-application/{applicationId}")
    @SecuredEndpoint(obj = "admin.loan-reschedules", act = "read")
    @Operation(summary = "List all reschedules for a given applicationId (admin)")
    public ResponseEntity<JsonNode> listByApplication(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        var loan = loanRepository.findByTenantIdAndApplicationId(tenantId, applicationId)
                .orElse(null);

        ArrayNode array = objectMapper.createArrayNode();
        if (loan != null) {
            var records = rescheduleRepository.findByTenantIdAndLoanId(tenantId, loan.getId());
            records.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            for (var r : records) {
                array.add(buildRecord(r));
            }
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("applicationId", applicationId.toString());
        response.put("total", array.size());
        response.set("reschedules", array);

        return ResponseEntity.ok(response);
    }

    // ─── build rich history record ─────────────────────────────────────────────

    private ObjectNode buildRecord(LoanRescheduleJpaEntity r) {
        ObjectNode node = objectMapper.createObjectNode();

        // Identity
        node.put("rescheduleId",   r.getId().toString());
        node.put("loanId",         r.getLoanId() != null ? r.getLoanId().toString() : null);
        node.put("loanNumber",     r.getLoanNumber());
        node.put("rescheduleType", r.getRescheduleType());
        node.put("status",         r.getStatus());
        node.put("details",        generateRescheduleDetails(r));

        // Resolve applicationId from loan
        try {
            var loan = loanRepository.findById(r.getLoanId());
            if (loan.isPresent()) {
                node.put("applicationId", loan.get().getApplicationId().toString());
                node.put("customerId",    loan.get().getCustomerId().toString());
                node.put("principalAmount", loan.get().getPrincipalAmount() != null
                        ? loan.get().getPrincipalAmount().toPlainString() : null);
            }
        } catch (Exception ignored) {}

        // Request details
        node.put("justification",        r.getJustification());
        node.put("requestedBy",          r.getRequestedBy() != null ? r.getRequestedBy().toString() : null);
        node.put("extensionMonths",      r.getExtensionMonths());
        node.put("holidayMonths",        r.getHolidayMonths());
        node.put("requestedSkipMonth",   r.getRequestedSkipMonth() != null ? r.getRequestedSkipMonth().toString() : null);
        node.put("newProfitRate",        r.getNewProfitRate() != null ? r.getNewProfitRate().toPlainString() : null);
        node.put("writeOffAmount",       r.getWriteOffAmount() != null ? r.getWriteOffAmount().toPlainString() : null);
        node.put("profitWaiverAmount",   r.getProfitWaiverAmount() != null ? r.getProfitWaiverAmount().toPlainString() : null);

        // Before/after schedule change
        ObjectNode before = objectMapper.createObjectNode();
        Integer oldTenure = r.getOldTenureMonths();
        BigDecimal oldInstallment = r.getOldInstallmentAmount();
        LocalDate oldMaturity = r.getOldMaturityDate();

        // Fallback to loan's current data if old data was not captured (for older records)
        try {
            var loanOpt = loanRepository.findById(r.getLoanId());
            if (loanOpt.isPresent()) {
                var loan = loanOpt.get();
                if (oldTenure == null) oldTenure = loan.getTenureMonths();
                if (oldInstallment == null) oldInstallment = loan.getInstallmentAmount();
                if (oldMaturity == null) oldMaturity = loan.getMaturityDate();
            }
        } catch (Exception ignored) {}

        before.put("tenureMonths",      oldTenure);
        before.put("installmentAmount", oldInstallment != null ? oldInstallment.toPlainString() : null);
        before.put("maturityDate",      oldMaturity != null ? oldMaturity.toString() : null);
        node.set("before", before);

        ObjectNode after = objectMapper.createObjectNode();
        after.put("tenureMonths",      r.getNewTenureMonths());
        after.put("installmentAmount", r.getNewInstallmentAmount() != null ? r.getNewInstallmentAmount().toPlainString() : null);
        after.put("maturityDate",      r.getNewMaturityDate() != null ? r.getNewMaturityDate().toString() : null);
        node.set("after", after);

        // Approval / rejection timeline
        ObjectNode timeline = objectMapper.createObjectNode();
        timeline.put("requestedAt",  r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        timeline.put("approvedAt",   r.getApprovedAt() != null ? r.getApprovedAt().toString() : null);
        timeline.put("rejectedAt",   r.getRejectedAt() != null ? r.getRejectedAt().toString() : null);
        timeline.put("appliedAt",    r.getAppliedAt() != null ? r.getAppliedAt().toString() : null);
        timeline.put("cancelledAt",  r.getCancelledAt() != null ? r.getCancelledAt().toString() : null);
        node.set("timeline", timeline);

        // Approver info
        ObjectNode approver = objectMapper.createObjectNode();
        approver.put("approverId",    r.getApproverId() != null ? r.getApproverId().toString() : null);
        approver.put("approverRole",  r.getApproverRole());
        approver.put("approvalNotes", r.getApprovalNotes());
        node.set("approver", approver);

        // Rejection info
        node.put("rejectionReason", r.getRejectionReason());

        // Fineract / GL sync
        ObjectNode sync = objectMapper.createObjectNode();
        sync.put("fineractRescheduleId", r.getFineractRescheduleId());
        sync.put("fineractSynced",       r.isFineractSynced());
        sync.put("glPosted",             r.isGlPosted());
        node.set("sync", sync);

        return node;
    }

    private String generateRescheduleDetails(LoanRescheduleJpaEntity r) {
        StringBuilder sb = new StringBuilder();
        String type = r.getRescheduleType();

        Integer oldTenure = r.getOldTenureMonths();
        LocalDate oldMaturity = r.getOldMaturityDate();
        BigDecimal oldInstallment = r.getOldInstallmentAmount();

        // Fallback for older records
        try {
            if (oldTenure == null || oldMaturity == null || oldInstallment == null) {
                var loanOpt = loanRepository.findById(r.getLoanId());
                if (loanOpt.isPresent()) {
                    var loan = loanOpt.get();
                    if (oldTenure == null) oldTenure = loan.getTenureMonths();
                    if (oldMaturity == null) oldMaturity = loan.getMaturityDate();
                    if (oldInstallment == null) oldInstallment = loan.getInstallmentAmount();
                }
            }
        } catch (Exception ignored) {}

        if ("SKIP_PAYMENT".equals(type)) {
            sb.append("Requested to skip installment for ").append(r.getRequestedSkipMonth() != null ? r.getRequestedSkipMonth().getMonth().name() + " " + r.getRequestedSkipMonth().getYear() : "selected month").append(". ");
            sb.append("Action: The skipped month is moved to the end of the schedule. ");
            sb.append("Total tenure remains ").append(oldTenure != null ? oldTenure : "?").append(" months, ");
            sb.append("but maturity date is extended to ").append(r.getNewMaturityDate() != null ? r.getNewMaturityDate() : "a later date").append(".");
        } else if ("TENURE_EXTENSION".equals(type)) {
            sb.append("Loan tenure extended by ").append(r.getExtensionMonths()).append(" months. ");
            sb.append("Configuration: Tenure changed from ").append(oldTenure != null ? oldTenure : "?").append(" to ").append(r.getNewTenureMonths()).append(" months. ");
            sb.append("Result: Monthly installment reduced from ").append(scale2(oldInstallment)).append(" to ").append(scale2(r.getNewInstallmentAmount())).append(" SAR.");
        } else if ("PAYMENT_HOLIDAY".equals(type)) {
            sb.append("Payment holiday granted for ").append(r.getHolidayMonths()).append(" months. ");
            sb.append("Impact: Next installments are paused, and maturity is extended to ").append(r.getNewMaturityDate()).append(".");
        } else if ("RESTRUCTURING".equals(type)) {
            sb.append("Loan restructuring performed for financial relief. ");
            if (r.getWriteOffAmount() != null && r.getWriteOffAmount().compareTo(BigDecimal.ZERO) > 0) {
                sb.append("Benefit: Principal write-off of ").append(scale2(r.getWriteOffAmount())).append(" SAR applied. ");
            }
            if (r.getNewProfitRate() != null) {
                sb.append("Change: Profit rate updated to ").append(r.getNewProfitRate().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)).append("%. ");
            }
            sb.append("Impact: Monthly payment adjusted from ").append(scale2(oldInstallment)).append(" to ").append(scale2(r.getNewInstallmentAmount())).append(" SAR.");
        }

        return sb.toString();
    }

    private BigDecimal scale2(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private UUID extractTenantId(Jwt jwt) {
        String t = jwt != null ? jwt.getClaimAsString("tenant_id") : null;
        if (t == null || t.isBlank()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "No tenant_id claim in JWT");
        }
        return UUID.fromString(t);
    }
}
