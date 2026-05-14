package com.ksa.financing.collections.adapter.rest.controller;

import com.ksa.financing.collections.domain.model.PenaltyWaiverRequest;
import com.ksa.financing.collections.domain.port.in.ApprovePenaltyWaiverUseCase;
import com.ksa.financing.collections.domain.port.in.RequestPenaltyWaiverUseCase;
import com.ksa.financing.collections.domain.port.out.PenaltyWaiverRequestRepository;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/collections/waiver-requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Collections - Penalty Waivers", description = "Customer-initiated penalty waiver requests")
public class PenaltyWaiverRequestController {

    private final RequestPenaltyWaiverUseCase requestUseCase;
    private final ApprovePenaltyWaiverUseCase approveUseCase;
    private final PenaltyWaiverRequestRepository requestRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping
    @Operation(summary = "Submit a penalty waiver request (Customer) — penalty is waived against an invoice")
    public ResponseEntity<PenaltyWaiverRequest> submit(
            @Valid @RequestBody SubmitWaiverRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID customerId = UUID.fromString(jwt.getSubject());

        InstallmentLookup lookup = lookupInstallmentByInvoice(tenantId, request.invoiceId());

        var command = new RequestPenaltyWaiverUseCase.SubmitWaiverRequestCommand(
                tenantId,
                lookup.loanId(),
                request.applicationId(),
                request.invoiceId(),
                lookup.installmentId(),
                null,
                request.reason(),
                customerId);

        return ResponseEntity.ok(requestUseCase.submitRequest(command));
    }

    private static final Pattern INVOICE_ID_PATTERN =
            Pattern.compile("^INV-([0-9A-Fa-f]{8})-(\\d{1,5})$");

    @GetMapping
    @SecuredEndpoint(obj = "penalty-waivers", act = "read")
    @Operation(summary = "List waiver requests (paginated, optional status filter)")
    public ResponseEntity<PagedWaiverRequestsResponse> list(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        PenaltyWaiverRequest.WaiverRequestStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = PenaltyWaiverRequest.WaiverRequestStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("COMMON.VALIDATION.FAILED",
                        "Invalid status: " + status + ". Expected one of PENDING, APPROVED, REJECTED");
            }
        }
        var result = requestRepository.findPaged(tenantId, statusFilter, page, size);
        return ResponseEntity.ok(new PagedWaiverRequestsResponse(
                result.content(), result.totalElements(), result.totalPages(), result.page(), result.size()));
    }

    public record PagedWaiverRequestsResponse(
            List<PenaltyWaiverRequest> content,
            long totalElements,
            int totalPages,
            int page,
            int size
    ) {}

    private InstallmentLookup lookupInstallmentByInvoice(UUID tenantId, String invoiceId) {
        if (invoiceId == null || invoiceId.isBlank()) {
            throw new BusinessException("COMMON.VALIDATION.FAILED", "invoiceId is required");
        }
        try {
            Object[] row = (Object[]) entityManager.createNativeQuery(
                            "SELECT i.id, i.loan_id " +
                                    "FROM installments i " +
                                    "JOIN repayment_schedules rs ON rs.id = i.schedule_id " +
                                    "WHERE i.tenant_id = ?1 AND rs.is_active = true AND i.invoice_id = ?2 " +
                                    "LIMIT 1")
                    .setParameter(1, tenantId)
                    .setParameter(2, invoiceId)
                    .getSingleResult();
            return new InstallmentLookup((UUID) row[0], (UUID) row[1]);
        } catch (jakarta.persistence.NoResultException ignored) {
            // fall through to regex-based resolution
        }

        Matcher matcher = INVOICE_ID_PATTERN.matcher(invoiceId);
        if (!matcher.matches()) {
            throw new BusinessException("COMMON.VALIDATION.FAILED",
                    "Invalid invoiceId format. Expected: INV-XXXXXXXX-NNN");
        }
        String loanPrefix = matcher.group(1).toLowerCase();
        int installmentNumber = Integer.parseInt(matcher.group(2));

        try {
            Object[] row = (Object[]) entityManager.createNativeQuery(
                            "SELECT i.id, i.loan_id " +
                                    "FROM installments i " +
                                    "JOIN repayment_schedules rs ON rs.id = i.schedule_id " +
                                    "WHERE i.tenant_id = ?1 AND rs.is_active = true " +
                                    "  AND LOWER(CAST(i.loan_id AS text)) LIKE ?2 " +
                                    "  AND i.installment_number = ?3 " +
                                    "LIMIT 1")
                    .setParameter(1, tenantId)
                    .setParameter(2, loanPrefix + "%")
                    .setParameter(3, installmentNumber)
                    .getSingleResult();
            return new InstallmentLookup((UUID) row[0], (UUID) row[1]);
        } catch (jakarta.persistence.NoResultException ex) {
            // Schedule resolved? If yes → installment_number is bad. If no → schedule not seeded.
            Long scheduleCount = ((Number) entityManager.createNativeQuery(
                            "SELECT COUNT(1) FROM repayment_schedules " +
                                    "WHERE tenant_id = ?1 AND is_active = true " +
                                    "  AND LOWER(CAST(loan_id AS text)) LIKE ?2")
                    .setParameter(1, tenantId)
                    .setParameter(2, loanPrefix + "%")
                    .getSingleResult()).longValue();

            if (scheduleCount == 0) {
                throw new BusinessException(
                        ErrorCodes.Collections.SCHEDULE_NOT_SEEDED,
                        "Repayment schedule not yet seeded for invoice " + invoiceId
                                + ". The loan exists in lending-service but its repayment schedule has not propagated to collections-service yet. "
                                + "Hit any payment endpoint or POST /api/v1/admin/repayment-schedules to seed it.",
                        invoiceId);
            }
            throw NotFoundException.forEntity("Installment",
                    "invoice=" + invoiceId + " (schedule found but installment #" + installmentNumber + " missing)");
        }
    }

    private record InstallmentLookup(UUID installmentId, UUID loanId) {}

    @GetMapping("/loan/{loanId}")
    @Operation(summary = "List waiver requests for a loan")
    public ResponseEntity<List<PenaltyWaiverRequest>> listByLoan(
            @PathVariable UUID loanId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(requestRepository.findByLoanId(tenantId, loanId));
    }

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "List waiver requests for an application")
    public ResponseEntity<List<PenaltyWaiverRequest>> listByApplication(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(requestRepository.findByApplicationId(tenantId, applicationId));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List waiver request history for a customer")
    public ResponseEntity<List<PenaltyWaiverRequest>> listByCustomer(
            @PathVariable UUID customerId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(requestRepository.findByRequestedBy(tenantId, customerId));
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get a waiver request by ID")
    public ResponseEntity<PenaltyWaiverRequest> get(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return requestRepository.findById(tenantId, requestId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> NotFoundException.forEntity("PenaltyWaiverRequest", requestId.toString()));
    }

    @PostMapping("/{requestId}/approve")
    @Operation(summary = "Approve a penalty waiver request (Admin). "
            + "Optional `amount` allows admin to approve a smaller amount than requested; "
            + "must be > 0 and <= requestedAmount.")
    public ResponseEntity<Void> approve(
            @PathVariable UUID requestId,
            @RequestBody(required = false) ApproveWaiverRequest body,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID adminId = UUID.fromString(jwt.getSubject());
        BigDecimal amount = body != null ? body.amount() : null;
        approveUseCase.approve(tenantId, requestId, amount, adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invoice/{invoiceId}/approve")
    @Operation(summary = "Approve a penalty waiver request by invoice ID (Admin). "
            + "Optional `amount` allows partial approval; must be <= requestedAmount.")
    public ResponseEntity<Void> approveByInvoice(
            @PathVariable String invoiceId,
            @RequestBody(required = false) ApproveWaiverRequest body,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID adminId = UUID.fromString(jwt.getSubject());

        PenaltyWaiverRequest request = requestRepository.findByInvoiceId(tenantId, invoiceId).stream()
                .filter(r -> r.getStatus() == PenaltyWaiverRequest.WaiverRequestStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending waiver request found for invoice: " + invoiceId));

        BigDecimal amount = body != null ? body.amount() : null;
        approveUseCase.approve(tenantId, request.getId(), amount, adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{requestId}/reject")
    @Operation(summary = "Reject a penalty waiver request (Admin)")
    public ResponseEntity<Void> reject(
            @PathVariable UUID requestId,
            @Valid @RequestBody RejectWaiverRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID adminId = UUID.fromString(jwt.getSubject());
        approveUseCase.reject(tenantId, requestId, request.reason(), adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invoice/{invoiceId}/reject")
    @Operation(summary = "Reject a penalty waiver request by invoice ID (Admin)")
    public ResponseEntity<Void> rejectByInvoice(
            @PathVariable String invoiceId,
            @Valid @RequestBody RejectWaiverRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID adminId = UUID.fromString(jwt.getSubject());

        PenaltyWaiverRequest waiverRequest = requestRepository.findByInvoiceId(tenantId, invoiceId).stream()
                .filter(r -> r.getStatus() == PenaltyWaiverRequest.WaiverRequestStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending waiver request found for invoice: " + invoiceId));

        approveUseCase.reject(tenantId, waiverRequest.getId(), request.reason(), adminId);
        return ResponseEntity.ok().build();
    }

    public record SubmitWaiverRequest(
            UUID applicationId,
            String invoiceId,
            String reason
    ) {}

    public record RejectWaiverRequest(
            String reason
    ) {}

    public record ApproveWaiverRequest(
            BigDecimal amount
    ) {}

    private UUID extractTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null) {
            throw new BusinessException("COMMON.AUTH.ACCESS_DENIED", "No tenant_id claim found");
        }
        return UUID.fromString(tenantId);
    }
}
