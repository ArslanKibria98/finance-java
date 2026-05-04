package com.ksa.financing.lending.adapter.rest.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import io.temporal.client.WorkflowNotFoundException;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.lending.infrastructure.persistence.entity.LoanRescheduleJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanRescheduleRepository;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanRepository;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaRescheduleConfigRepository;
import com.ksa.islamic.orchestration.activity.lending.LoanRescheduleWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * REST controller for loan rescheduling operations.
 *
 * Starts a Temporal {@link LoanRescheduleWorkflow} for each reschedule request.
 * The workflow handles: eligibility validation → approval (if needed) → new schedule
 * → GL entries (RESTRUCTURING only via ledger-service) → Fineract sync (via ledger-service proxy).
 *
 * Endpoints (all keyed by applicationId — the loan application UUID):
 *   POST /api/v1/loans/{applicationId}/reschedules                        — Request reschedule (returns rescheduleId)
 *   GET  /api/v1/loans/{applicationId}/reschedules                        — List all reschedules for the loan
 *   GET  /api/v1/loans/{applicationId}/reschedules/{rescheduleId}/status  — Query status by rescheduleId
 *   POST /api/v1/loans/{applicationId}/reschedules/{rescheduleId}/approve — Approve (ops_head / credit_committee)
 *   POST /api/v1/loans/{applicationId}/reschedules/{rescheduleId}/reject  — Reject  (ops_head / credit_committee)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/loans/{applicationId}/reschedules")
@RequiredArgsConstructor
@Tag(name = "Loan Rescheduling", description = "Loan reschedule operations per Blueprint 17")
public class LoanRescheduleController {

    private final WorkflowClient workflowClient;
    private final ObjectMapper objectMapper;
    private final JpaLoanRescheduleRepository rescheduleRepository;
    private final JpaLoanRepository loanRepository;
    private final JpaRescheduleConfigRepository configRepository;

    @Value("${temporal.task-queue:loan-application-queue}")
    private String taskQueue;

    // ══════════════════════════════════════════════════════════════
    // REQUEST RESCHEDULE
    // ══════════════════════════════════════════════════════════════

    @PostMapping
    @SecuredEndpoint(obj = "loan.reschedules", act = "create")
    @Operation(summary = "Request a loan reschedule (starts Temporal workflow, returns rescheduleId)")
    public ResponseEntity<JsonNode> requestReschedule(
            @PathVariable UUID applicationId,
            @RequestBody RescheduleRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        String requestedBy = jwt.getSubject();

        // Resolve applicationId → loanId
        var loan = loanRepository.findByTenantIdAndApplicationId(UUID.fromString(tenantId), applicationId)
                .orElseThrow(() -> NotFoundException.forEntity("Loan", applicationId.toString()));
        UUID loanId = loan.getId();

        String idempotencyKey = request.idempotencyKey() != null
                ? request.idempotencyKey()
                : applicationId + "-" + request.rescheduleType() + "-" + System.currentTimeMillis();
        String workflowId = "reschedule-" + loanId + "-" + idempotencyKey;

        log.info("Reschedule requested: applicationId={} loanId={} type={} tenantId={}", applicationId, loanId, request.rescheduleType(), tenantId);

        // Idempotency check — return existing rescheduleId if already submitted with same key
        var existing = rescheduleRepository.findByTenantIdAndIdempotencyKey(
                UUID.fromString(tenantId), idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent: returning existing reschedule id={}", existing.get().getId());
            var resp = buildSubmitResponse(existing.get().getId().toString(), applicationId.toString(),
                    loanId.toString(), request.rescheduleType(), existing.get().getStatus(), "Reschedule already in progress.");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
        }

        // Block new request if ANY reschedule (any type) is still active for this loan
        List<String> activeStatuses = List.of("SUBMITTED", "PENDING", "AWAITING_APPROVAL", "PROCESSING");
        boolean hasAnyPending = rescheduleRepository.existsByTenantIdAndLoanIdAndStatusIn(
                UUID.fromString(tenantId), loanId, activeStatuses);
        if (hasAnyPending) {
            // Find the active request to include its type and status in the error
            var activeRequest = rescheduleRepository
                    .findByTenantIdAndLoanId(UUID.fromString(tenantId), loanId)
                    .stream()
                    .filter(r -> activeStatuses.contains(r.getStatus()))
                    .findFirst();
            String detail = activeRequest
                    .map(r -> "A " + r.getRescheduleType() + " request is already " + r.getStatus()
                            + " (ID: " + r.getId() + "). Wait for it to be approved or rejected before submitting a new one.")
                    .orElse("A reschedule request is already pending for this loan.");
            throw new BusinessException(ErrorCodes.CONFLICT, detail);
        }

        // Pre-save PENDING record so we can return rescheduleId immediately (workflow is async)
        var entity = LoanRescheduleJpaEntity.builder()
                .tenantId(UUID.fromString(tenantId))
                .loanId(loanId)
                .loanNumber(request.loanNumber() != null ? request.loanNumber() : loan.getLoanNumber())
                .rescheduleType(request.rescheduleType())
                .status("SUBMITTED")
                .requestedBy(parseUuidSafely(requestedBy))
                .justification(request.justification())
                .requestedSkipMonth(request.skipMonth())
                .extensionMonths(request.extensionMonths())
                .holidayMonths(request.holidayMonths())
                .newProfitRate(request.newProfitRate())
                .writeOffAmount(request.writeOffAmount())
                .profitWaiverAmount(request.profitWaiverAmount())
                .attachmentUrl(request.attachment())
                .workflowId(workflowId)
                .idempotencyKey(idempotencyKey)
                .createdBy(parseUuidSafely(requestedBy))
                .build();
        var saved = rescheduleRepository.save(entity);
        String rescheduleId = saved.getId().toString();

        log.info("Reschedule pre-saved: rescheduleId={} workflowId={}", rescheduleId, workflowId);

        var workflowInput = new LoanRescheduleWorkflow.RescheduleRequest(
                tenantId,
                loanId.toString(),
                loan.getLoanNumber(),
                request.rescheduleType(),
                requestedBy,
                request.justification(),
                request.skipMonth() != null ? request.skipMonth().toString() : null,
                request.extensionMonths(),
                request.holidayMonths(),
                request.newProfitRate(),
                request.writeOffAmount(),
                request.profitWaiverAmount(),
                request.outstandingPrincipal(),
                request.attachment(),
                idempotencyKey,
                requestedBy
        );

        try {
            var workflow = workflowClient.newWorkflowStub(
                    LoanRescheduleWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId(workflowId)
                            .setTaskQueue(taskQueue)
                            .build()
            );

            // Start async — don't wait for completion (approval may take days)
            WorkflowClient.start(workflow::execute, workflowInput);

            log.info("Reschedule workflow started: workflowId={}", workflowId);

            var response = buildSubmitResponse(rescheduleId, applicationId.toString(),
                    loanId.toString(), request.rescheduleType(), "PENDING",
                    "Reschedule request submitted. "
                            + (requiresApproval(request.rescheduleType())
                            ? "Awaiting manager approval."
                            : "Processing automatically."));

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (WorkflowExecutionAlreadyStarted e) {
            log.info("Idempotent: workflow already started: workflowId={}", workflowId);
            var response = buildSubmitResponse(rescheduleId, applicationId.toString(),
                    loanId.toString(), request.rescheduleType(), "PENDING", "Reschedule already in progress.");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // RESCHEDULE OPTIONS  (mobile-ready: types + fields + validations)
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/options")
    @SecuredEndpoint(obj = "loan.reschedules", act = "read")
    @Operation(summary = "Get available reschedule types for this loan with per-type field definitions and validation rules (mobile-ready)")
    public ResponseEntity<JsonNode> getRescheduleOptions(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        var loan = loanRepository.findByTenantIdAndApplicationId(UUID.fromString(tenantId), applicationId)
                .orElseThrow(() -> NotFoundException.forEntity("Loan", applicationId.toString()));

        if (!"DISBURSED".equals(loan.getStatus()) && !"ACTIVE".equals(loan.getStatus())) {
            throw new BusinessException(ErrorCodes.CONFLICT,
                    "Reschedule options only available for active loans. Current status: " + loan.getStatus());
        }

        // ── Loan state ─────────────────────────────────────────────
        LocalDate disbursementDate = loan.getDisbursementDate();
        long loanAgeMonths = disbursementDate != null
                ? ChronoUnit.MONTHS.between(disbursementDate, LocalDate.now()) : 0;
        int currentDpd     = loan.getCurrentDpd()     != null ? loan.getCurrentDpd()     : 0;
        int currentTenure  = loan.getTenureMonths()   != null ? loan.getTenureMonths()   : 0;

        // ── Existing reschedules ────────────────────────────────────
        var existing = rescheduleRepository.findByTenantIdAndLoanId(UUID.fromString(tenantId), loan.getId());
        var activeStatuses = List.of("PENDING", "SUBMITTED", "PROCESSING", "AWAITING_APPROVAL");

        // ── Fetch Global Configs ────────────────────────────────────
        var globalConfigs = configRepository.findByTenantIdAndActiveTrue(UUID.fromString(tenantId));

        // ── Build root response ─────────────────────────────────────
        var root = objectMapper.createObjectNode();
        root.put("applicationId", applicationId.toString());
        root.put("loanId", loan.getId().toString());
        root.put("loanNumber", loan.getLoanNumber());

        var summary = root.putObject("loanSummary");
        summary.put("outstandingPrincipal",
                loan.getOutstandingPrincipal() != null ? loan.getOutstandingPrincipal().toPlainString() : "0");
        summary.put("currentInstallment",
                loan.getInstallmentAmount() != null ? loan.getInstallmentAmount().toPlainString() : "0");
        summary.put("tenureMonths", currentTenure);
        summary.put("currentDpd", currentDpd);
        summary.put("loanAgeMonths", (int) loanAgeMonths);
        summary.put("disbursementDate", disbursementDate != null ? disbursementDate.toString() : null);
        summary.put("maturityDate", loan.getMaturityDate() != null ? loan.getMaturityDate().toString() : null);
        summary.put("profitRate", loan.getProfitRate() != null ? loan.getProfitRate().toPlainString() : null);

        var options = root.putArray("rescheduleOptions");

        for (var config : globalConfigs) {
            var opt = options.addObject();
            opt.put("type",            config.getRescheduleType());
            opt.put("label",           config.getLabelEn());
            opt.put("labelAr",         config.getLabelAr());
            opt.put("description",     config.getDescriptionEn());
            opt.put("descriptionAr",   config.getDescriptionAr());
            opt.put("requiresApproval", config.isRequiresApproval());
            if (config.getApproverRole() != null) {
                opt.put("approver", config.getApproverRole());
            }

            // Availability Logic
            var unavailableReasons = new ArrayList<String>();
            boolean hasActive = existing.stream().anyMatch(r ->
                    config.getRescheduleType().equals(r.getRescheduleType()) && activeStatuses.contains(r.getStatus()));
            if (hasActive) {
                unavailableReasons.add("A request of this type is already in progress");
            }

            // Parse Fields from Config
            try {
                if (config.getFieldsConfig() != null) {
                    var fields = (ArrayNode) objectMapper.readTree(config.getFieldsConfig());
                    
                    // Inject dynamic validation context into fields
                    injectDynamicContextIntoFields(fields, config.getRescheduleType(), loan);
                    
                    opt.set("fields", fields);
                }
            } catch (Exception e) {
                log.error("Failed to parse fieldsConfig for {}: {}", config.getRescheduleType(), e.getMessage());
                opt.putArray("fields");
            }

            opt.put("available", unavailableReasons.isEmpty());
            if (!unavailableReasons.isEmpty()) {
                var reasons = opt.putArray("unavailableReasons");
                unavailableReasons.forEach(reasons::add);
            }
        }

        return ResponseEntity.ok(root);
    }

    private void injectDynamicContextIntoFields(ArrayNode fields, String type, com.ksa.financing.lending.infrastructure.persistence.entity.LoanJpaEntity loan) {
        for (var field : fields) {
            if (field.isObject()) {
                var obj = (ObjectNode) field;
                var name = obj.path("name").asText();
                var validation = obj.has("validation") ? (ObjectNode) obj.get("validation") : null;

                if ("SKIP_PAYMENT".equals(type) || "PAYMENT_HOLIDAY".equals(type)) {
                    if ("skipmonth".equals(name) && validation != null) {
                        LocalDate nextDue = (loan.getFirstDueDate() != null)
                                ? loan.getFirstDueDate().plusMonths(
                                        Math.max(0, ChronoUnit.MONTHS.between(loan.getFirstDueDate(), LocalDate.now()) + 1))
                                : LocalDate.now().plusMonths(1).withDayOfMonth(1);
                        validation.put("minDate", nextDue.withDayOfMonth(1).toString());
                        validation.put("maxDate", loan.getMaturityDate() != null
                                ? loan.getMaturityDate().minusMonths(1).withDayOfMonth(1).toString() : null);
                    }
                }

                if ("TENURE_EXTENSION".equals(type) || "RESTRUCTURING".equals(type)) {
                    if ("extensionMonths".equals(name) && validation != null) {
                        int currentTenure = loan.getTenureMonths() != null ? loan.getTenureMonths() : 0;
                        int maxExtension = Math.min(12, Math.max(0, 72 - currentTenure));
                        validation.put("max", maxExtension);
                        validation.put("currentTenureMonths", currentTenure);
                    }
                }
            }
        }
    }

    @GetMapping
    @SecuredEndpoint(obj = "loan.reschedules", act = "read")
    @Operation(summary = "List all reschedule requests for a loan application")
    public ResponseEntity<JsonNode> listReschedules(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        var loan = loanRepository.findByTenantIdAndApplicationId(UUID.fromString(tenantId), applicationId)
                .orElseThrow(() -> NotFoundException.forEntity("Loan", applicationId.toString()));

        var list = rescheduleRepository.findByTenantIdAndLoanId(UUID.fromString(tenantId), loan.getId());

        var arrayNode = objectMapper.createArrayNode();
        for (var r : list) {
            var node = objectMapper.createObjectNode()
                    .put("rescheduleId", r.getId().toString())
                    .put("applicationId", applicationId.toString())
                    .put("loanId", r.getLoanId().toString())
                    .put("rescheduleType", r.getRescheduleType())
                    .put("status", r.getStatus())
                    .put("justification", r.getJustification())
                    .put("requestedSkipMonth", r.getRequestedSkipMonth() != null ? r.getRequestedSkipMonth().toString() : null)
                    .put("extensionMonths", r.getExtensionMonths())
                    .put("holidayMonths", r.getHolidayMonths())
                    .put("newProfitRate", r.getNewProfitRate() != null ? r.getNewProfitRate().toPlainString() : null)
                    .put("writeOffAmount", r.getWriteOffAmount() != null ? r.getWriteOffAmount().toPlainString() : null)
                    .put("profitWaiverAmount", r.getProfitWaiverAmount() != null ? r.getProfitWaiverAmount().toPlainString() : null)
                    .put("newTenureMonths", r.getNewTenureMonths())
                    .put("newInstallmentAmount", r.getNewInstallmentAmount() != null ? r.getNewInstallmentAmount().toPlainString() : null)
                    .put("newMaturityDate", r.getNewMaturityDate() != null ? r.getNewMaturityDate().toString() : null)
                    .put("attachmentUrl", r.getAttachmentUrl())
                    .put("details", generateRescheduleDetails(r));

            // Add 'before' block for consistency
            ObjectNode before = objectMapper.createObjectNode();
            before.put("tenureMonths", r.getOldTenureMonths());
            before.put("installmentAmount", r.getOldInstallmentAmount() != null ? r.getOldInstallmentAmount().toPlainString() : null);
            before.put("maturityDate", r.getOldMaturityDate() != null ? r.getOldMaturityDate().toString() : null);
            node.set("before", before);

            node.put("approverRole", r.getApproverRole())
                    .put("approvalNotes", r.getApprovalNotes())
                    .put("rejectionReason", r.getRejectionReason())
                    .put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                    .put("appliedAt", r.getAppliedAt() != null ? r.getAppliedAt().toString() : null)
                    .put("rejectedAt", r.getRejectedAt() != null ? r.getRejectedAt().toString() : null);
            arrayNode.add(node);
        }

        var response = objectMapper.createObjectNode();
        response.put("applicationId", applicationId.toString());
        response.put("loanId", loan.getId().toString());
        response.put("total", list.size());
        response.set("reschedules", arrayNode);
        return ResponseEntity.ok(response);
    }

    // ══════════════════════════════════════════════════════════════
    // APPROVE / REJECT (signal the running workflow)
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/{rescheduleId}/approve")
    @SecuredEndpoint(obj = "loan.reschedules", act = "update")
    @Operation(summary = "Approve a pending reschedule request (ops_head or credit_committee)")
    public ResponseEntity<JsonNode> approve(
            @PathVariable UUID applicationId,
            @PathVariable UUID rescheduleId,
            @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        var record = findReschedule(tenantId, applicationId, rescheduleId);

        // Guard: can only approve PENDING / AWAITING_APPROVAL reschedules
        if (!"PENDING".equals(record.getStatus()) && !"SUBMITTED".equals(record.getStatus())
                && !"AWAITING_APPROVAL".equals(record.getStatus())) {
            throw new BusinessException(ErrorCodes.CONFLICT,
                    "Cannot approve reschedule in status: " + record.getStatus()
                    + ". Only PENDING or AWAITING_APPROVAL reschedules can be approved.");
        }

        log.info("Approval signal: rescheduleId={} workflowId={} by={}", rescheduleId, record.getWorkflowId(), jwt.getSubject());

        // Persist approver info (but do NOT overwrite status yet — workflow may already be APPROVED)
        record.setApproverId(parseUuidSafely(jwt.getSubject()));
        record.setApproverRole(request.approverRole());
        record.setApprovalNotes(request.approvalNotes());
        record.setApprovedAt(java.time.OffsetDateTime.now());
        // Only set AWAITING_APPROVAL if status is PENDING/SUBMITTED — don't downgrade APPROVED
        if ("PENDING".equals(record.getStatus()) || "SUBMITTED".equals(record.getStatus())) {
            record.setStatus("AWAITING_APPROVAL");
        }
        rescheduleRepository.save(record);

        boolean signalSent = false;
        try {
            var workflow = workflowClient.newWorkflowStub(LoanRescheduleWorkflow.class, record.getWorkflowId());
            workflow.approve(new LoanRescheduleWorkflow.ApprovalSignal(
                    jwt.getSubject(),
                    request.approverRole(),
                    request.approvalNotes()
            ));
            signalSent = true;
        } catch (WorkflowNotFoundException e) {
            // Workflow already completed — this is OK, just read the result from DB
            log.info("Workflow already completed for reschedule {} — reading result from DB", rescheduleId);
        } catch (Exception e) {
            // Check if it's a "workflow already completed" error (Temporal sends this as a generic exception sometimes)
            if (e.getMessage() != null && e.getMessage().contains("already completed")) {
                log.info("Workflow already completed for reschedule {} — reading result from DB", rescheduleId);
            } else {
                log.error("Failed to send approval signal for reschedule {}: {}", rescheduleId, e.getMessage());
                throw new BusinessException(ErrorCodes.INTERNAL_ERROR,
                        "Failed to send approval signal: " + e.getMessage());
            }
        }

        // Poll DB until workflow completes (APPROVED/REJECTED) or timeout (45s)
        String finalStatus = "PROCESSING";
        String newTenureMonths = null;
        String newInstallmentAmount = null;
        String newMaturityDate = null;

        // If signal was sent, poll for workflow completion; if not, check DB immediately
        int maxPolls = signalSent ? 90 : 2;
        for (int i = 0; i < maxPolls; i++) {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            var updated = rescheduleRepository.findById(rescheduleId).orElse(null);
            if (updated != null && ("APPROVED".equals(updated.getStatus()) || "REJECTED".equals(updated.getStatus()) || "FAILED".equals(updated.getStatus()))) {
                finalStatus = updated.getStatus();
                if (updated.getNewTenureMonths() != null) newTenureMonths = updated.getNewTenureMonths().toString();
                if (updated.getNewInstallmentAmount() != null) newInstallmentAmount = updated.getNewInstallmentAmount().toPlainString();
                if (updated.getNewMaturityDate() != null) newMaturityDate = updated.getNewMaturityDate().toString();
                break;
            }
        }

        // Fallback: still not APPROVED/REJECTED — sync from Temporal workflow result directly
        if (!"APPROVED".equals(finalStatus) && !"REJECTED".equals(finalStatus)) {
            log.warn("DB not yet updated for rescheduleId={} — attempting to sync from Temporal result", rescheduleId);
            try {
                var completedWorkflow = workflowClient.newUntypedWorkflowStub(record.getWorkflowId());
                var result = completedWorkflow.getResult(5, java.util.concurrent.TimeUnit.SECONDS, LoanRescheduleWorkflow.RescheduleResult.class);
                if (result != null) {
                    finalStatus = result.status() != null ? result.status() : "PROCESSING";
                    if (result.newTenureMonths() > 0) newTenureMonths = String.valueOf(result.newTenureMonths());
                    if (result.newInstallmentAmount() != null) newInstallmentAmount = result.newInstallmentAmount().toPlainString();
                    if (result.newMaturityDate() != null) newMaturityDate = result.newMaturityDate();

                    // Sync the stale DB record with Temporal's authoritative result
                    if ("APPROVED".equals(finalStatus)) {
                        var stale = rescheduleRepository.findById(rescheduleId).orElse(null);
                        if (stale != null && !"APPROVED".equals(stale.getStatus())) {
                            stale.setStatus("APPROVED");
                            if (result.newTenureMonths() > 0) stale.setNewTenureMonths(result.newTenureMonths());
                            if (result.newInstallmentAmount() != null) stale.setNewInstallmentAmount(result.newInstallmentAmount());
                            if (result.newMaturityDate() != null) stale.setNewMaturityDate(java.time.LocalDate.parse(result.newMaturityDate()));
                            stale.setAppliedAt(java.time.OffsetDateTime.now());
                            rescheduleRepository.save(stale);
                            log.info("Synced stale DB record from Temporal result: rescheduleId={}", rescheduleId);
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not sync from Temporal result for rescheduleId={}: {}", rescheduleId, ex.getMessage());
                // Last resort: just return what DB has
                var latest = rescheduleRepository.findById(rescheduleId).orElse(null);
                if (latest != null) {
                    finalStatus = latest.getStatus();
                    if (latest.getNewTenureMonths() != null) newTenureMonths = latest.getNewTenureMonths().toString();
                    if (latest.getNewInstallmentAmount() != null) newInstallmentAmount = latest.getNewInstallmentAmount().toPlainString();
                    if (latest.getNewMaturityDate() != null) newMaturityDate = latest.getNewMaturityDate().toString();
                }
            }
        }

        var response = objectMapper.createObjectNode()
                .put("rescheduleId", rescheduleId.toString())
                .put("applicationId", applicationId.toString())
                .put("status", finalStatus)
                .put("newTenureMonths", newTenureMonths)
                .put("newInstallmentAmount", newInstallmentAmount)
                .put("newMaturityDate", newMaturityDate)
                .put("message", "APPROVED".equals(finalStatus)
                        ? "Reschedule approved and schedule updated successfully."
                        : "PROCESSING".equals(finalStatus) || "AWAITING_APPROVAL".equals(finalStatus)
                            ? "Approval signal sent. Workflow is processing — use status endpoint to poll."
                            : "Current status: " + finalStatus);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{rescheduleId}/reject")
    @SecuredEndpoint(obj = "loan.reschedules", act = "update")
    @Operation(summary = "Reject a pending reschedule request (ops_head or credit_committee)")
    public ResponseEntity<JsonNode> reject(
            @PathVariable UUID applicationId,
            @PathVariable UUID rescheduleId,
            @RequestBody RejectionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        var record = findReschedule(tenantId, applicationId, rescheduleId);

        // Guard: can only reject PENDING reschedules
        if (!"PENDING".equals(record.getStatus()) && !"SUBMITTED".equals(record.getStatus())) {
            throw new BusinessException(ErrorCodes.CONFLICT,
                    "Cannot reject reschedule in status: " + record.getStatus()
                    + ". Only PENDING reschedules can be rejected.");
        }

        log.info("Rejection signal: rescheduleId={} workflowId={} by={}", rescheduleId, record.getWorkflowId(), jwt.getSubject());

        try {
            var workflow = workflowClient.newWorkflowStub(LoanRescheduleWorkflow.class, record.getWorkflowId());
            workflow.reject(new LoanRescheduleWorkflow.RejectionSignal(
                    jwt.getSubject(),
                    request.rejectionReason()
            ));
        } catch (WorkflowNotFoundException e) {
            log.warn("Workflow not found for reschedule {}: {}", rescheduleId, e.getMessage());
            throw new BusinessException(ErrorCodes.CONFLICT,
                    "Reschedule workflow has already completed or does not exist. Current status: " + record.getStatus());
        } catch (Exception e) {
            log.error("Failed to send rejection signal for reschedule {}: {}", rescheduleId, e.getMessage());
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR,
                    "Failed to send rejection signal: " + e.getMessage());
        }

        // Poll DB until workflow reflects final status or timeout (15s)
        String finalStatus = "REJECTED";
        for (int i = 0; i < 30; i++) {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            var updated = rescheduleRepository.findById(rescheduleId).orElse(null);
            if (updated != null && ("APPROVED".equals(updated.getStatus()) || "REJECTED".equals(updated.getStatus()) || "FAILED".equals(updated.getStatus()))) {
                finalStatus = updated.getStatus();
                break;
            }
        }

        var response = objectMapper.createObjectNode()
                .put("rescheduleId", rescheduleId.toString())
                .put("applicationId", applicationId.toString())
                .put("status", finalStatus)
                .put("message", "REJECTED".equals(finalStatus)
                        ? "Reschedule rejected successfully."
                        : "Rejection signal sent. Current status: " + finalStatus);

        return ResponseEntity.ok(response);
    }

    // ══════════════════════════════════════════════════════════════
    // STATUS QUERY
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/{rescheduleId}/status")
    @SecuredEndpoint(obj = "loan.reschedules", act = "read")
    @Operation(summary = "Query reschedule status by rescheduleId (DB UUID)")
    public ResponseEntity<JsonNode> getStatus(
            @PathVariable UUID applicationId,
            @PathVariable UUID rescheduleId,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        var record = findReschedule(tenantId, applicationId, rescheduleId);

        log.info("Status query: rescheduleId={} workflowId={} applicationId={}", rescheduleId, record.getWorkflowId(), applicationId);

        var response = objectMapper.createObjectNode()
                .put("rescheduleId", rescheduleId.toString())
                .put("applicationId", applicationId.toString())
                .put("loanId", record.getLoanId().toString())
                .put("rescheduleType", record.getRescheduleType())
                .put("status", record.getStatus())
                .put("justification", record.getJustification())
                .put("requestedSkipMonth", record.getRequestedSkipMonth() != null ? record.getRequestedSkipMonth().toString() : null)
                .put("extensionMonths", record.getExtensionMonths())
                .put("holidayMonths", record.getHolidayMonths())
                .put("newProfitRate", record.getNewProfitRate() != null ? record.getNewProfitRate().toPlainString() : null)
                .put("writeOffAmount", record.getWriteOffAmount() != null ? record.getWriteOffAmount().toPlainString() : null)
                .put("profitWaiverAmount", record.getProfitWaiverAmount() != null ? record.getProfitWaiverAmount().toPlainString() : null)
                .put("newTenureMonths", record.getNewTenureMonths())
                .put("newInstallmentAmount", record.getNewInstallmentAmount() != null ? record.getNewInstallmentAmount().toPlainString() : null)
                .put("newMaturityDate", record.getNewMaturityDate() != null ? record.getNewMaturityDate().toString() : null)
                .put("details", generateRescheduleDetails(record));

        // Add 'before' block
        ObjectNode before = objectMapper.createObjectNode();
        before.put("tenureMonths", record.getOldTenureMonths());
        before.put("installmentAmount", record.getOldInstallmentAmount() != null ? record.getOldInstallmentAmount().toPlainString() : null);
        before.put("maturityDate", record.getOldMaturityDate() != null ? record.getOldMaturityDate().toString() : null);
        response.set("before", before);

        response.put("approverRole", record.getApproverRole())
                .put("approvalNotes", record.getApprovalNotes())
                .put("rejectionReason", record.getRejectionReason())
                .put("createdAt", record.getCreatedAt() != null ? record.getCreatedAt().toString() : null)
                .put("updatedAt", record.getUpdatedAt() != null ? record.getUpdatedAt().toString() : null)
                .put("appliedAt", record.getAppliedAt() != null ? record.getAppliedAt().toString() : null)
                .put("rejectedAt", record.getRejectedAt() != null ? record.getRejectedAt().toString() : null);

        // Also try Temporal query for live status (in-flight workflows only)
        try {
            var workflow = workflowClient.newWorkflowStub(LoanRescheduleWorkflow.class, record.getWorkflowId());
            String temporalStatus = workflow.getStatus();
            response.put("temporalStatus", temporalStatus);
        } catch (Exception e) {
            log.debug("Temporal status query skipped (workflow may be completed): {}", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    // ══════════════════════════════════════════════════════════════
    // Request / Response Records
    // ══════════════════════════════════════════════════════════════

    public record RescheduleRequest(
            String rescheduleType,           // SKIP_PAYMENT | TENURE_EXTENSION | PAYMENT_HOLIDAY | RESTRUCTURING
            String loanNumber,
            String justification,
            @JsonProperty("skipmonth") LocalDate skipMonth,
            Integer extensionMonths,         // TENURE_EXTENSION only
            Integer holidayMonths,           // PAYMENT_HOLIDAY only
            BigDecimal newProfitRate,        // RESTRUCTURING only
            BigDecimal writeOffAmount,       // RESTRUCTURING only
            BigDecimal profitWaiverAmount,   // RESTRUCTURING only
            BigDecimal outstandingPrincipal,
            @JsonProperty("attachment") String attachment,
            String idempotencyKey
    ) {}

    public record ApprovalRequest(
            String approverRole,
            String approvalNotes
    ) {}

    public record RejectionRequest(
            String rejectionReason
    ) {}

    // ══════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════

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

    private String extractTenantId(Jwt jwt) {
        String tenantId = jwt != null ? jwt.getClaimAsString("tenant_id") : null;
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return tenantId;
    }

    private LoanRescheduleJpaEntity findReschedule(String tenantId, UUID applicationId, UUID rescheduleId) {
        var record = rescheduleRepository.findById(rescheduleId)
                .orElseThrow(() -> NotFoundException.forEntity("LoanReschedule", rescheduleId.toString()));
        // Tenant check
        if (!record.getTenantId().toString().equals(tenantId)) {
            throw new BusinessException(ErrorCodes.ACCESS_DENIED,
                    "Reschedule " + rescheduleId + " not accessible");
        }
        // Verify reschedule belongs to the loan linked to this applicationId
        var loan = loanRepository.findByTenantIdAndApplicationId(UUID.fromString(tenantId), applicationId)
                .orElseThrow(() -> NotFoundException.forEntity("Loan", applicationId.toString()));
        if (!record.getLoanId().equals(loan.getId())) {
            throw new BusinessException(ErrorCodes.ACCESS_DENIED,
                    "Reschedule " + rescheduleId + " does not belong to applicationId " + applicationId);
        }
        if (record.getWorkflowId() == null) {
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR,
                    "Reschedule " + rescheduleId + " has no associated workflow");
        }
        return record;
    }

    private JsonNode buildSubmitResponse(String rescheduleId, String applicationId,
                                          String loanId, String rescheduleType,
                                          String status, String message) {
        return objectMapper.createObjectNode()
                .put("rescheduleId", rescheduleId)
                .put("applicationId", applicationId)
                .put("loanId", loanId)
                .put("rescheduleType", rescheduleType)
                .put("status", status)
                .put("message", message);
    }

    private UUID parseUuidSafely(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean requiresApproval(String type) {
        return "TENURE_EXTENSION".equals(type) || "PAYMENT_HOLIDAY".equals(type) || "RESTRUCTURING".equals(type);
    }
}
