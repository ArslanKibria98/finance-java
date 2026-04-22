package com.ksa.financing.lending.adapter.rest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import io.temporal.client.WorkflowNotFoundException;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.lending.infrastructure.persistence.entity.LoanRescheduleJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanRescheduleRepository;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanRepository;
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
                .requestedSkipMonth(request.requestedSkipMonth())
                .extensionMonths(request.extensionMonths())
                .holidayMonths(request.holidayMonths())
                .newProfitRate(request.newProfitRate())
                .writeOffAmount(request.writeOffAmount())
                .profitWaiverAmount(request.profitWaiverAmount())
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
                request.requestedSkipMonth() != null ? request.requestedSkipMonth().toString() : null,
                request.extensionMonths(),
                request.holidayMonths(),
                request.newProfitRate(),
                request.writeOffAmount(),
                request.profitWaiverAmount(),
                request.outstandingPrincipal(),
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
        int ifrs9Stage     = loan.getIfrs9Stage()     != null ? loan.getIfrs9Stage()     : 1;

        // ── Existing reschedules ────────────────────────────────────
        var existing = rescheduleRepository.findByTenantIdAndLoanId(UUID.fromString(tenantId), loan.getId());
        var activeStatuses = List.of("PENDING", "SUBMITTED", "PROCESSING");

        var approvedSkips = existing.stream()
                .filter(r -> "SKIP_PAYMENT".equals(r.getRescheduleType()) && "APPROVED".equals(r.getStatus()))
                .toList();
        int skipCount = approvedSkips.size();
        LocalDate lastSkipMonth = approvedSkips.stream()
                .map(LoanRescheduleJpaEntity::getRequestedSkipMonth)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        long monthsSinceLastSkip = lastSkipMonth != null
                ? ChronoUnit.MONTHS.between(lastSkipMonth, LocalDate.now()) : 999;

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

        // ══ 1. SKIP PAYMENT ══════════════════════════════════════════
        boolean hasActiveSkip = existing.stream().anyMatch(r ->
                "SKIP_PAYMENT".equals(r.getRescheduleType()) && activeStatuses.contains(r.getStatus()));

        var skipReasons = new ArrayList<String>();
        // Availability checks disabled — always show as available for frontend testing
        // if (loanAgeMonths < 3)          skipReasons.add("Loan must be at least 3 months old (current: " + loanAgeMonths + " months)");
        // if (currentDpd > 0)             skipReasons.add("No arrears allowed (current DPD: " + currentDpd + " days)");
        // if (skipCount >= 2)             skipReasons.add("Maximum 2 skips already used (" + skipCount + "/2)");
        // if (monthsSinceLastSkip < 6)    skipReasons.add("Must wait 6 months between skips (" + (6 - monthsSinceLastSkip) + " months remaining)");
        if (hasActiveSkip)              skipReasons.add("A skip payment request is already in progress");

        LocalDate nextDue = (loan.getFirstDueDate() != null)
                ? loan.getFirstDueDate().plusMonths(
                        Math.max(0, ChronoUnit.MONTHS.between(loan.getFirstDueDate(), LocalDate.now()) + 1))
                : LocalDate.now().plusMonths(1).withDayOfMonth(1);

        var skipOpt = options.addObject();
        skipOpt.put("type",            "SKIP_PAYMENT");
        skipOpt.put("label",           "Skip Payment");
        skipOpt.put("labelAr",         "تأجيل قسط");
        skipOpt.put("description",     "Move one installment to end of tenure. No extra profit charged (Sharia-compliant).");
        skipOpt.put("descriptionAr",   "تأجيل قسط واحد إلى نهاية مدة التمويل دون أي رسوم إضافية.");
        skipOpt.put("available",       skipReasons.isEmpty());
        skipOpt.put("requiresApproval", false);
        skipOpt.put("skipsUsed",       skipCount);
        skipOpt.put("skipsRemaining",  Math.max(0, 2 - skipCount));
        if (!skipReasons.isEmpty()) {
            var reasons = skipOpt.putArray("unavailableReasons");
            skipReasons.forEach(reasons::add);
        }
        var skipFields = skipOpt.putArray("fields");
        var skipMonthField = skipFields.addObject();
        skipMonthField.put("name",     "requestedSkipMonth");
        skipMonthField.put("type",     "DATE");
        skipMonthField.put("label",    "Month to Skip");
        skipMonthField.put("labelAr",  "الشهر المراد تأجيله");
        skipMonthField.put("required", true);
        skipMonthField.put("hint",     "Select the upcoming installment month you want to skip.");
        var skipVal = skipMonthField.putObject("validation");
        skipVal.put("minDate",   nextDue.withDayOfMonth(1).toString());
        skipVal.put("maxDate",   loan.getMaturityDate() != null
                ? loan.getMaturityDate().minusMonths(1).withDayOfMonth(1).toString() : null);
        skipVal.put("format",    "YYYY-MM-01");
        skipVal.put("note",      "Must be a future installment month. Use first day of month.");

        var skipJustField = skipFields.addObject();
        skipJustField.put("name",        "justification");
        skipJustField.put("type",        "TEXT");
        skipJustField.put("label",       "Reason for Skip (Optional)");
        skipJustField.put("labelAr",     "سبب التأجيل (اختياري)");
        skipJustField.put("placeholder", "Briefly mention why you want to skip this installment...");
        skipJustField.put("required",    false);
        skipJustField.put("hint",        "Optional — helps us serve you better.");
        var skipJustVal = skipJustField.putObject("validation");
        skipJustVal.put("minLength", 0);
        skipJustVal.put("maxLength", 300);

        // ══ 2. TENURE EXTENSION ══════════════════════════════════════
        boolean hasActiveTenure = existing.stream().anyMatch(r ->
                "TENURE_EXTENSION".equals(r.getRescheduleType()) && activeStatuses.contains(r.getStatus()));
        int maxExtension = Math.min(12, Math.max(0, 72 - currentTenure));

        var tenureReasons = new ArrayList<String>();
        // Availability checks disabled — always show as available for frontend testing
        // if (loanAgeMonths < 6)    tenureReasons.add("Loan must be at least 6 months old (current: " + loanAgeMonths + " months)");
        // if (currentDpd > 30)      tenureReasons.add("DPD must be ≤ 30 days (current: " + currentDpd + " days)");
        // if (ifrs9Stage >= 3)      tenureReasons.add("Not available for high-risk (D/E grade) loans");
        // if (maxExtension <= 0)    tenureReasons.add("Maximum 72-month total tenure already reached");
        if (hasActiveTenure)      tenureReasons.add("A tenure extension request is already in progress");

        var tenureOpt = options.addObject();
        tenureOpt.put("type",            "TENURE_EXTENSION");
        tenureOpt.put("label",           "Tenure Extension");
        tenureOpt.put("labelAr",         "تمديد مدة التمويل");
        tenureOpt.put("description",     "Extend your loan tenure to reduce your monthly installment.");
        tenureOpt.put("descriptionAr",   "تمديد مدة التمويل لتقليل قيمة القسط الشهري.");
        tenureOpt.put("available",       tenureReasons.isEmpty());
        tenureOpt.put("requiresApproval", true);
        tenureOpt.put("approver",        "OPERATIONS_HEAD");
        tenureOpt.put("approvalLabel",   "Operations Head Approval");
        if (!tenureReasons.isEmpty()) {
            var reasons = tenureOpt.putArray("unavailableReasons");
            tenureReasons.forEach(reasons::add);
        }
        var tenureFields = tenureOpt.putArray("fields");

        var extField = tenureFields.addObject();
        extField.put("name",        "extensionMonths");
        extField.put("type",        "INTEGER");
        extField.put("label",       "Extension Duration");
        extField.put("labelAr",     "مدة التمديد");
        extField.put("placeholder", "Enter months (1–" + maxExtension + ")");
        extField.put("unit",        "months");
        extField.put("required",    true);
        extField.put("hint",        "Extending tenure reduces monthly installment but increases total profit paid.");
        var extVal = extField.putObject("validation");
        extVal.put("min",                   1);
        extVal.put("max",                   maxExtension);
        extVal.put("step",                  1);
        extVal.put("currentTenureMonths",   currentTenure);
        extVal.put("maxTotalTenureMonths",  72);
        extVal.put("note",                  "Total tenure cannot exceed 72 months");

        var tenureJustField = tenureFields.addObject();
        tenureJustField.put("name",        "justification");
        tenureJustField.put("type",        "TEXT");
        tenureJustField.put("label",       "Reason for Extension");
        tenureJustField.put("labelAr",     "سبب طلب التمديد");
        tenureJustField.put("placeholder", "Describe your financial hardship or reason for requesting tenure extension...");
        tenureJustField.put("required",    true);
        tenureJustField.put("hint",        "Reviewed by the operations team. Be specific about your financial situation.");
        var tenureJustVal = tenureJustField.putObject("validation");
        tenureJustVal.put("minLength", 20);
        tenureJustVal.put("maxLength", 500);

        var tenureHardshipField = tenureFields.addObject();
        tenureHardshipField.put("name",     "hardshipDeclaration");
        tenureHardshipField.put("type",     "BOOLEAN");
        tenureHardshipField.put("label",    "I declare that I am experiencing financial hardship");
        tenureHardshipField.put("labelAr",  "أقر بأنني أمر بظروف مالية صعبة");
        tenureHardshipField.put("required", true);
        tenureHardshipField.put("hint",     "Required by SAMA regulations for tenure extension approval.");

        // ══ 3. PAYMENT HOLIDAY ═══════════════════════════════════════
        boolean hasActiveHoliday = existing.stream().anyMatch(r ->
                "PAYMENT_HOLIDAY".equals(r.getRescheduleType()) && activeStatuses.contains(r.getStatus()));

        var holidayReasons = new ArrayList<String>();
        // Availability checks disabled — always show as available for frontend testing
        // if (loanAgeMonths < 3) holidayReasons.add("Loan must be at least 3 months old (current: " + loanAgeMonths + " months)");
        if (hasActiveHoliday)  holidayReasons.add("A payment holiday request is already in progress");

        var holidayOpt = options.addObject();
        holidayOpt.put("type",            "PAYMENT_HOLIDAY");
        holidayOpt.put("label",           "Payment Holiday");
        holidayOpt.put("labelAr",         "إجازة سداد");
        holidayOpt.put("description",     "Pause payments for up to 3 months. No extra profit accrues (Sharia-compliant).");
        holidayOpt.put("descriptionAr",   "أوقف أقساطك مؤقتاً لمدة تصل إلى 3 أشهر دون احتساب أرباح إضافية.");
        holidayOpt.put("available",       holidayReasons.isEmpty());
        holidayOpt.put("requiresApproval", true);
        holidayOpt.put("approver",        "OPERATIONS_HEAD");
        holidayOpt.put("approvalLabel",   "Operations Head Approval");
        if (!holidayReasons.isEmpty()) {
            var reasons = holidayOpt.putArray("unavailableReasons");
            holidayReasons.forEach(reasons::add);
        }
        var holidayFields = holidayOpt.putArray("fields");

        var holidayMonthsField = holidayFields.addObject();
        holidayMonthsField.put("name",        "holidayMonths");
        holidayMonthsField.put("type",        "INTEGER");
        holidayMonthsField.put("label",       "Holiday Duration");
        holidayMonthsField.put("labelAr",     "مدة الإجازة");
        holidayMonthsField.put("placeholder", "Enter months (1–3)");
        holidayMonthsField.put("unit",        "months");
        holidayMonthsField.put("required",    true);
        holidayMonthsField.put("hint",        "Tenure will be extended by the number of holiday months.");
        var holidayVal = holidayMonthsField.putObject("validation");
        holidayVal.put("min",  1);
        holidayVal.put("max",  3);
        holidayVal.put("step", 1);
        holidayVal.put("note", "Maximum 3 months payment holiday allowed per request");

        var holidayJustField = holidayFields.addObject();
        holidayJustField.put("name",        "justification");
        holidayJustField.put("type",        "TEXT");
        holidayJustField.put("label",       "Justification");
        holidayJustField.put("labelAr",     "المبرر");
        holidayJustField.put("placeholder", "Explain why you need a payment holiday...");
        holidayJustField.put("required",    true);
        holidayJustField.put("hint",        "Mandatory for operations head review and approval.");
        var holidayJustVal = holidayJustField.putObject("validation");
        holidayJustVal.put("minLength", 20);
        holidayJustVal.put("maxLength", 500);

        // ══ 4. FULL RESTRUCTURING ════════════════════════════════════
        boolean hasActiveRestructure = existing.stream().anyMatch(r ->
                "RESTRUCTURING".equals(r.getRescheduleType()) && activeStatuses.contains(r.getStatus()));

        var restructureReasons = new ArrayList<String>();
        // Availability checks disabled — always show as available for frontend testing
        // if (currentDpd <= 90)     restructureReasons.add("Full restructuring requires DPD > 90 days or documented financial distress (current DPD: " + currentDpd + " days)");
        if (hasActiveRestructure) restructureReasons.add("A restructuring request is already in progress");

        int maxRestExtension = Math.min(12, Math.max(0, 72 - currentTenure));
        String currentProfitRate = loan.getProfitRate() != null ? loan.getProfitRate().toPlainString() : "0";
        String maxReducedRate    = loan.getProfitRate() != null
                ? loan.getProfitRate().subtract(BigDecimal.ONE).max(BigDecimal.ONE).toPlainString() : "29.00";

        var restructureOpt = options.addObject();
        restructureOpt.put("type",            "RESTRUCTURING");
        restructureOpt.put("label",           "Full Restructuring");
        restructureOpt.put("labelAr",         "إعادة هيكلة كاملة");
        restructureOpt.put("description",     "Complete loan restructure for financial distress. Options: principal reduction, profit rate reduction, tenure extension, or combination.");
        restructureOpt.put("descriptionAr",   "إعادة هيكلة شاملة للتمويل في حالات الضائقة المالية: تخفيض الأصل أو الربح أو تمديد المدة أو مزيج.");
        restructureOpt.put("available",       restructureReasons.isEmpty());
        restructureOpt.put("requiresApproval", true);
        restructureOpt.put("approver",        "CREDIT_COMMITTEE");
        restructureOpt.put("approvalLabel",   "Credit Committee Approval");
        if (!restructureReasons.isEmpty()) {
            var reasons = restructureOpt.putArray("unavailableReasons");
            restructureReasons.forEach(reasons::add);
        }
        var restructureFields = restructureOpt.putArray("fields");

        // restructuringOption — SELECT
        var restTypeField = restructureFields.addObject();
        restTypeField.put("name",        "restructuringOption");
        restTypeField.put("type",        "SELECT");
        restTypeField.put("label",       "Restructuring Option");
        restTypeField.put("labelAr",     "خيار إعادة الهيكلة");
        restTypeField.put("required",    true);
        restTypeField.put("hint",        "Select the type of restructuring you need. Bank will assess feasibility.");
        var restOpts = restTypeField.putArray("options");
        restOpts.addObject().put("value", "REDUCE_PRINCIPAL").put("label", "Principal Reduction (Write-off)").put("labelAr", "تخفيض الأصل (شطب جزئي)");
        restOpts.addObject().put("value", "REDUCE_PROFIT_RATE").put("label", "Profit Rate Reduction").put("labelAr", "تخفيض معدل الربح");
        restOpts.addObject().put("value", "EXTEND_TENURE").put("label", "Tenure Extension").put("labelAr", "تمديد مدة التمويل");
        restOpts.addObject().put("value", "COMBINATION").put("label", "Combination (Multiple Options)").put("labelAr", "مزيج من الخيارات");

        // extensionMonths — conditional on EXTEND_TENURE / COMBINATION
        var restExtField = restructureFields.addObject();
        restExtField.put("name",        "extensionMonths");
        restExtField.put("type",        "INTEGER");
        restExtField.put("label",       "Extension Months");
        restExtField.put("labelAr",     "عدد أشهر التمديد");
        restExtField.put("unit",        "months");
        restExtField.put("required",    false);
        restExtField.put("conditional", true);
        restExtField.put("showWhen",    "restructuringOption IN [EXTEND_TENURE, COMBINATION]");
        restExtField.put("hint",        "Only fill if requesting tenure extension as part of restructure.");
        var restExtVal = restExtField.putObject("validation");
        restExtVal.put("min",                  1);
        restExtVal.put("max",                  maxRestExtension);
        restExtVal.put("step",                 1);
        restExtVal.put("maxTotalTenureMonths", 72);
        restExtVal.put("currentTenureMonths",  currentTenure);

        // newProfitRate — conditional on REDUCE_PROFIT_RATE / COMBINATION
        var newRateField = restructureFields.addObject();
        newRateField.put("name",        "newProfitRate");
        newRateField.put("type",        "DECIMAL");
        newRateField.put("label",       "Requested New Profit Rate (%)");
        newRateField.put("labelAr",     "معدل الربح المقترح (%)");
        newRateField.put("required",    false);
        newRateField.put("conditional", true);
        newRateField.put("showWhen",    "restructuringOption IN [REDUCE_PROFIT_RATE, COMBINATION]");
        newRateField.put("hint",        "Bank may offer a different rate. Current rate: " + currentProfitRate + "%");
        var rateVal = newRateField.putObject("validation");
        rateVal.put("min",          "1.00");
        rateVal.put("max",          maxReducedRate);
        rateVal.put("decimalPlaces", 2);
        rateVal.put("currentRate",  currentProfitRate);
        rateVal.put("note",         "Requested rate must be less than current rate (" + currentProfitRate + "%)");

        // justification
        var restJustField = restructureFields.addObject();
        restJustField.put("name",        "justification");
        restJustField.put("type",        "TEXT");
        restJustField.put("label",       "Financial Distress Explanation");
        restJustField.put("labelAr",     "شرح الوضع المالي");
        restJustField.put("placeholder", "Describe your situation in detail — income loss, medical condition, legal matter, etc.");
        restJustField.put("required",    true);
        restJustField.put("hint",        "Reviewed by credit committee. Be as detailed as possible.");
        var restJustVal = restJustField.putObject("validation");
        restJustVal.put("minLength", 50);
        restJustVal.put("maxLength", 1000);

        // hardshipDeclaration
        var restHardshipField = restructureFields.addObject();
        restHardshipField.put("name",     "hardshipDeclaration");
        restHardshipField.put("type",     "BOOLEAN");
        restHardshipField.put("label",    "I declare I am under documented financial distress or legal proceedings");
        restHardshipField.put("labelAr",  "أقر بأنني في ضائقة مالية موثقة أو إجراءات قانونية");
        restHardshipField.put("required", true);
        restHardshipField.put("hint",     "Required for credit committee review per SAMA guidelines.");

        return ResponseEntity.ok(root);
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
            arrayNode.add(objectMapper.createObjectNode()
                    .put("rescheduleId", r.getId().toString())
                    .put("applicationId", applicationId.toString())
                    .put("loanId", r.getLoanId().toString())
                    .put("rescheduleType", r.getRescheduleType())
                    .put("status", r.getStatus())
                    .put("requestedSkipMonth", r.getRequestedSkipMonth() != null ? r.getRequestedSkipMonth().toString() : null)
                    .put("extensionMonths", r.getExtensionMonths())
                    .put("holidayMonths", r.getHolidayMonths())
                    .put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null));
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

        // Guard: can only approve PENDING reschedules
        if (!"PENDING".equals(record.getStatus()) && !"SUBMITTED".equals(record.getStatus())) {
            throw new BusinessException(ErrorCodes.CONFLICT,
                    "Cannot approve reschedule in status: " + record.getStatus()
                    + ". Only PENDING reschedules can be approved.");
        }

        log.info("Approval signal: rescheduleId={} workflowId={} by={}", rescheduleId, record.getWorkflowId(), jwt.getSubject());

        try {
            var workflow = workflowClient.newWorkflowStub(LoanRescheduleWorkflow.class, record.getWorkflowId());
            workflow.approve(new LoanRescheduleWorkflow.ApprovalSignal(
                    jwt.getSubject(),
                    request.approverRole(),
                    request.approvalNotes()
            ));
        } catch (WorkflowNotFoundException e) {
            log.warn("Workflow not found for reschedule {}: {}", rescheduleId, e.getMessage());
            throw new BusinessException(ErrorCodes.CONFLICT,
                    "Reschedule workflow has already completed or does not exist. Current status: " + record.getStatus());
        } catch (Exception e) {
            log.error("Failed to send approval signal for reschedule {}: {}", rescheduleId, e.getMessage());
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR,
                    "Failed to send approval signal: " + e.getMessage());
        }

        // Poll DB until workflow completes (APPROVED/REJECTED) or timeout (15s)
        String finalStatus = "PROCESSING";
        String newTenureMonths = null;
        String newInstallmentAmount = null;
        String newMaturityDate = null;
        for (int i = 0; i < 30; i++) {
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

        var response = objectMapper.createObjectNode()
                .put("rescheduleId", rescheduleId.toString())
                .put("applicationId", applicationId.toString())
                .put("status", finalStatus)
                .put("newTenureMonths", newTenureMonths)
                .put("newInstallmentAmount", newInstallmentAmount)
                .put("newMaturityDate", newMaturityDate)
                .put("message", "APPROVED".equals(finalStatus)
                        ? "Reschedule approved successfully."
                        : "Approval signal sent. Current status: " + finalStatus);

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
                .put("requestedSkipMonth", record.getRequestedSkipMonth() != null ? record.getRequestedSkipMonth().toString() : null)
                .put("extensionMonths", record.getExtensionMonths())
                .put("holidayMonths", record.getHolidayMonths())
                .put("newTenureMonths", record.getNewTenureMonths())
                .put("newInstallmentAmount", record.getNewInstallmentAmount() != null ? record.getNewInstallmentAmount().toPlainString() : null)
                .put("newMaturityDate", record.getNewMaturityDate() != null ? record.getNewMaturityDate().toString() : null)
                .put("approverRole", record.getApproverRole())
                .put("approvalNotes", record.getApprovalNotes())
                .put("rejectionReason", record.getRejectionReason())
                .put("createdAt", record.getCreatedAt() != null ? record.getCreatedAt().toString() : null)
                .put("updatedAt", record.getUpdatedAt() != null ? record.getUpdatedAt().toString() : null);

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
            LocalDate requestedSkipMonth,    // SKIP_PAYMENT only
            Integer extensionMonths,         // TENURE_EXTENSION only
            Integer holidayMonths,           // PAYMENT_HOLIDAY only
            BigDecimal newProfitRate,        // RESTRUCTURING only
            BigDecimal writeOffAmount,       // RESTRUCTURING only
            BigDecimal profitWaiverAmount,   // RESTRUCTURING only
            BigDecimal outstandingPrincipal,
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
