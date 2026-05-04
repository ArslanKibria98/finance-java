package com.ksa.financing.lending.adapter.rest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.lending.infrastructure.persistence.entity.LoanApplicationJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.entity.LoanJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.entity.LoanRescheduleJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanApplicationRepository;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanRepository;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanRescheduleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate API: Returns all data needed for the Financing Application detail screen.
 *
 * GET /api/v1/loan-applications/{applicationId}/full-detail
 *
 * Tabs covered:
 *   personalInformation   → customer-service Customer360 personalInfo + addressInfo
 *   loanInformation       → LoanApplication (amount, product, offer) + Loan (post-disbursement)
 *   employmentSalary      → Customer360 employments + LoanApp masdar employment
 *   documents             → LoanApplication (IBAN, contract flags)
 *   simahCheck            → LoanApp (creditScore, simahConsent, simahReferenceId) + Customer360 kycInfo
 *   openBankingCheck      → LoanApp (ibanVerified, bankCode) + Customer360 bankAccounts
 *   complianceCheck       → Customer360 (kycInfo, complianceQuestionHistory, kycWeightageData)
 *   creditCheck           → Customer360 (riskCalculation, riskHistory, kycWeightageData)
 *   approval              → LoanApp (status, currentStage, offeredAmount)
 *   reschedulingRequest   → LoanReschedule history
 *   stepper               → LoanApp (currentStage, stepperIndex, stepperLabel)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/loan-applications")
@RequiredArgsConstructor
@Tag(name = "Application Detail", description = "Aggregate application detail API for frontend tabs")
public class ApplicationDetailController {

    private final JpaLoanApplicationRepository applicationRepository;
    private final JpaLoanRepository loanRepository;
    private final JpaLoanRescheduleRepository rescheduleRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.services.customer-service-url}")
    private String customerServiceUrl;

    @Value("${app.services.collections-service-url:${COLLECTIONS_SERVICE_URL:http://collections-service:8099}}")
    private String collectionsServiceUrl;

    // ══════════════════════════════════════════════════════════════
    // MAIN AGGREGATE ENDPOINT
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/{applicationId}/full-detail")
    @SecuredEndpoint(obj = "loan.applications", act = "read")
    @Operation(summary = "Full application detail — all tabs data in one response")
    public ResponseEntity<JsonNode> getFullDetail(
            @PathVariable("applicationId") UUID applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        UUID tenantUuid = UUID.fromString(tenantId);

        // ── 1. Load loan application ────────────────────────────────
        LoanApplicationJpaEntity app = applicationRepository
                .findByTenantIdAndId(tenantUuid, applicationId)
                .orElseThrow(() -> NotFoundException.forEntity("LoanApplication", applicationId.toString()));

        // ── 2. Load loan (if disbursed) ─────────────────────────────
        LoanJpaEntity loan = loanRepository
                .findByTenantIdAndApplicationId(tenantUuid, applicationId)
                .orElse(null);

        // ── 3. Load reschedule history ──────────────────────────────
        List<LoanRescheduleJpaEntity> reschedules = loan != null
                ? rescheduleRepository.findByTenantIdAndLoanId(tenantUuid, loan.getId())
                : List.of();

        // ── 4. Load Customer 360 from customer-service ──────────────
        JsonNode customer360 = fetchCustomer360(app.getCustomerId().toString(), jwt.getTokenValue());

        // ── 5. Build aggregate response ─────────────────────────────
        ObjectNode root = objectMapper.createObjectNode();

        buildStepper(root, app, loan, jwt);
        buildPersonalInformation(root, customer360);
        buildLoanInformation(root, app, loan);
        buildEmploymentSalary(root, app, customer360);
        buildDocuments(root, app);
        buildSimahCheck(root, app, customer360);
        buildOpenBankingCheck(root, app, customer360);
        buildComplianceCheck(root, customer360);
        buildCreditCheck(root, customer360);
        buildApproval(root, app, loan);
        buildReschedulingRequest(root, reschedules);

        log.info("Full detail fetched: applicationId={} customerId={}", applicationId, app.getCustomerId());
        return ResponseEntity.ok(root);
    }

    // ══════════════════════════════════════════════════════════════
    // TAB BUILDERS
    // ══════════════════════════════════════════════════════════════

    /** Stepper: Finance → Verification → Simmah Consent → Counter → Contract → OTP → IVR → DISBURSED → PAID */
    private void buildStepper(ObjectNode root, LoanApplicationJpaEntity app, LoanJpaEntity loan, Jwt jwt) {
        ObjectNode stepper = root.putObject("stepper");
        stepper.put("currentStage",  nvl(app.getCurrentStage()));
        stepper.put("status",        nvl(app.getStatus()));
        stepper.put("applicationId", app.getId().toString());
        stepper.put("applicationNumber", nvl(app.getApplicationNumber()));
        stepper.put("workflowId",    nvl(app.getWorkflowId()));

        // Map currentStage → stepperIndex (1-based)
        // If currentStage is null, derive from status + loan data
        int idx;
        if (app.getCurrentStage() != null && !app.getCurrentStage().isBlank()) {
            idx = switch (app.getCurrentStage()) {
                case "INITIATED", "PRE_QUALIFICATION"           -> 1;
                case "BASIC_INFO", "EMPLOYMENT_VERIFICATION"    -> 2;
                case "SIMAH_CONSENT", "CREDIT_CHECK"            -> 3;
                case "OFFER_GENERATED", "COUNTER_OFFER"         -> 4;
                case "CONTRACT_GENERATION", "CONTRACT_SIGNING"  -> 5;
                case "OTP_VERIFICATION"                         -> 6;
                case "IVR_VERIFICATION"                         -> 7;
                case "DISBURSED", "PENDING_DISBURSEMENT"        -> 8;
                case "ACTIVE", "CLOSED", "PAID"                 -> 9;
                default -> 1;
            };
        } else {
            // Derive from signals when currentStage is not tracked
            String appStatus = app.getStatus() != null ? app.getStatus() : "";
            String loanStatus = loan != null && loan.getStatus() != null ? loan.getStatus() : "";
            boolean otpDone = Boolean.TRUE.equals(app.getOtpVerified());
            boolean ivrDone = Boolean.TRUE.equals(app.getIvrVerified());
            boolean contractSet = app.getContractExpiresAt() != null;
            boolean offerSet = app.getOfferedAmount() != null;
            boolean simahDone = Boolean.TRUE.equals(app.getSimahConsent());
            boolean employmentDone = app.getEmployerName() != null;

            // Collections-side check first — if all installments are settled there,
            // the loan is effectively fully paid even if lending's loan.status row
            // hasn't been flipped to PAID yet (no ScheduleFullyPaid listener wired).
            boolean fullyPaidInCollections = loan != null
                    && isFullyPaidInCollections(loan.getId(), jwt);

            if (fullyPaidInCollections)                                          idx = 9;
            else if ("PAID".equals(loanStatus))                                  idx = 9;
            else if (loan != null && ("ACTIVE".equals(loanStatus)
                    || "PENDING_DISBURSEMENT".equals(loanStatus)))               idx = 8;
            else if ("APPROVED".equals(appStatus) || "DISBURSED".equals(appStatus)) idx = 8;
            else if ("REJECTED".equals(appStatus) || "CANCELLED".equals(appStatus)) idx = resolveRejectedStep(app);
            else if (ivrDone)                                                    idx = 7;
            else if (otpDone)                                                    idx = 6;
            else if (contractSet)                                                idx = 5;
            else if (offerSet)                                                   idx = 4;
            else if (simahDone)                                                  idx = 3;
            else if (employmentDone)                                             idx = 2;
            else                                                                 idx = 1;
        }
        stepper.put("stepperIndex", idx);

        ArrayNode steps = stepper.putArray("steps");
        addStep(steps, 1, "Finance",         "INITIATED",       idx);
        addStep(steps, 2, "Verification",    "BASIC_INFO",      idx);
        addStep(steps, 3, "Simmah Consent",  "SIMAH_CONSENT",   idx);
        addStep(steps, 4, "Counter",         "COUNTER_OFFER",   idx);
        addStep(steps, 5, "Contract",        "CONTRACT_SIGNING",idx);
        addStep(steps, 6, "OTP",             "OTP_VERIFICATION",idx);
        addStep(steps, 7, "IVR",             "IVR_VERIFICATION",idx);
        addStep(steps, 8, "DISBURSED",       "DISBURSED",       idx);
        addStep(steps, 9, "PAID",            "PAID",            idx);
    }

    /** For rejected/cancelled apps, infer the last reached step from available data */
    private int resolveRejectedStep(LoanApplicationJpaEntity app) {
        if (Boolean.TRUE.equals(app.getIvrVerified()))    return 7;
        if (Boolean.TRUE.equals(app.getOtpVerified()))    return 6;
        if (app.getContractExpiresAt() != null)           return 5;
        if (app.getOfferedAmount() != null)               return 4;
        if (Boolean.TRUE.equals(app.getSimahConsent()))   return 3;
        if (app.getEmployerName() != null)                return 2;
        return 1;
    }

    /**
     * Calls collections-service to check if every installment is settled. Used as a
     * fallback for the stepper's PAID step when lending's loan.status row hasn't
     * been flipped to PAID yet. Returns false on any HTTP / parse failure.
     */
    private boolean isFullyPaidInCollections(UUID loanId, Jwt jwt) {
        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            var response = restTemplate.exchange(
                    collectionsServiceUrl + "/api/v1/repayment-schedules/by-loan/" + loanId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return false;
            }
            var data = objectMapper.readTree(response.getBody()).path("data");
            var fullyPaid = data.path("fullyPaid");
            if (fullyPaid.isBoolean()) {
                return fullyPaid.asBoolean();
            }
            var installments = data.path("installments");
            if (!installments.isArray() || installments.isEmpty()) return false;
            for (var i : installments) {
                var st = i.path("status").asText("");
                if (!"PAID".equalsIgnoreCase(st) && !"WAIVED".equalsIgnoreCase(st)) return false;
            }
            return true;
        } catch (Exception ex) {
            log.debug("Collections fully-paid check failed for loanId={}: {}", loanId, ex.getMessage());
            return false;
        }
    }

    private void addStep(ArrayNode steps, int num, String label, String stage, int currentIdx) {
        ObjectNode s = steps.addObject();
        s.put("step",   num);
        s.put("label",  label);
        s.put("stage",  stage);
        String stepStatus;
        if (num < currentIdx)      stepStatus = "COMPLETED";
        else if (num == currentIdx) stepStatus = "ACTIVE";
        else                        stepStatus = "PENDING";
        s.put("status", stepStatus);
    }

    /** Tab 1: Personal Information */
    private void buildPersonalInformation(ObjectNode root, JsonNode c360) {
        ObjectNode tab = root.putObject("personalInformation");

        // personalInfo comes from PII vault — may be null if vault is down.
        // Fallback: use customer base record (always present in Customer360).
        JsonNode pi  = safeGet(c360, "personalInfo");
        JsonNode cus = safeGet(c360, "customer");   // CustomerResponse — always populated

        ObjectNode personal = tab.putObject("personalInfo");
        personal.put("nationalId",           coalesce(text(pi, "nationalId"),           text(cus, "nationalId")));
        personal.put("nationalIdType",       coalesce(text(pi, "nationalIdType"),       text(cus, "nationalIdType")));
        personal.put("fullNameEn",           coalesce(text(pi, "fullNameEn"),           text(cus, "fullName")));
        personal.put("fullNameAr",           text(pi, "fullNameAr"));
        personal.put("firstName",            coalesce(text(pi, "firstName"),            text(cus, "firstName")));
        personal.put("lastName",             coalesce(text(pi, "lastName"),             text(cus, "lastName")));
        personal.put("firstNameAr",          coalesce(text(pi, "firstNameAr"),          text(cus, "firstNameAr")));
        personal.put("lastNameAr",           coalesce(text(pi, "lastNameAr"),           text(cus, "lastNameAr")));
        personal.put("dateOfBirthGregorian", coalesce(text(pi, "dateOfBirthGregorian"), text(cus, "dateOfBirth")));
        personal.put("dateOfBirthHijri",     text(pi, "dateOfBirthHijri"));
        personal.put("gender",               coalesce(text(pi, "gender"),               text(cus, "gender")));
        personal.put("nationality",          coalesce(text(pi, "nationality"),          text(cus, "nationality")));
        personal.put("mobile",               coalesce(text(pi, "mobile"),               text(cus, "mobileNumber")));
        personal.put("email",                coalesce(text(pi, "email"),                text(cus, "email")));
        personal.put("iqamaNumber",          text(pi, "iqamaNumber"));
        personal.put("iqamaExpiryDate",      text(pi, "iqamaExpiryDate"));
        personal.put("verificationDate",     text(pi, "verificationDate"));

        // Address — from addressInfo, fallback to customer base fields
        JsonNode ai = safeGet(c360, "addressInfo");
        ObjectNode address = tab.putObject("addressInfo");
        address.put("city",           coalesce(text(ai, "city"),           text(cus, "city")));
        address.put("regionName",     coalesce(text(ai, "regionName"),     text(cus, "region")));
        address.put("district",       text(ai, "district"));
        address.put("streetName",     coalesce(text(ai, "streetName"),     text(cus, "addressLine1")));
        address.put("buildingNumber", text(ai, "buildingNumber"));
        address.put("postCode",       coalesce(text(ai, "postCode"),       text(cus, "postalCode")));
        address.put("shortAddress",   text(ai, "shortAddress"));
    }

    /** Tab 2: Loan Information */
    private void buildLoanInformation(ObjectNode root, LoanApplicationJpaEntity app, LoanJpaEntity loan) {
        ObjectNode tab = root.putObject("loanInformation");

        // Loan amount info (request)
        ObjectNode amountInfo = tab.putObject("loanAmountInfo");
        amountInfo.put("requestedAmount",      str(app.getRequestedAmount()));
        amountInfo.put("requestedTenureMonths",app.getRequestedTenureMonths() != null ? app.getRequestedTenureMonths() : 0);
        amountInfo.put("offeredAmount",        str(app.getOfferedAmount()));
        amountInfo.put("offeredTenureMonths",  app.getRequestedTenureMonths() != null ? app.getRequestedTenureMonths() : 0);
        amountInfo.put("acceptedAmount",       str(app.getAcceptedAmount()));
        amountInfo.put("maxEligibleAmount",    str(app.getMaxEligibleAmount()));
        amountInfo.put("monthlyInstallment",   str(app.getOfferedMonthlyInstallment()));
        amountInfo.put("totalPayable",         str(app.getOfferedTotalPayable()));
        amountInfo.put("totalProfit",          str(app.getOfferedTotalProfit()));
        amountInfo.put("profitRate",           str(app.getProfitRate()));
        amountInfo.put("apr",                  str(app.getApr()));
        amountInfo.put("processingFee",        str(app.getProcessingFee()));
        amountInfo.put("adminFee",             str(app.getAdminFee()));

        // Loan application info
        ObjectNode appInfo = tab.putObject("loanApplicationInfo");
        appInfo.put("applicationId",     app.getId().toString());
        appInfo.put("applicationNumber", nvl(app.getApplicationNumber()));
        appInfo.put("productCode",       nvl(app.getProductCode()));
        appInfo.put("productName",       nvl(app.getProductName()));
        appInfo.put("shariaStructure",   nvl(app.getShariaStructure()));
        appInfo.put("purposeOfFinance",  nvl(app.getPurposeOfFinance()));
        appInfo.put("status",            nvl(app.getStatus()));
        appInfo.put("currentStage",      nvl(app.getCurrentStage()));
        appInfo.put("submittedAt",       str(app.getSubmittedAt()));
        appInfo.put("createdAt",         str(app.getCreatedAt()));

        // Post-disbursement loan data
        if (loan != null) {
            ObjectNode loanInfo = tab.putObject("activeLoan");
            loanInfo.put("loanId",              loan.getId().toString());
            loanInfo.put("loanNumber",          nvl(loan.getLoanNumber()));
            loanInfo.put("loanStatus",          nvl(loan.getStatus()));
            loanInfo.put("principalAmount",     str(loan.getPrincipalAmount()));
            loanInfo.put("totalAmount",         str(loan.getTotalAmount()));
            loanInfo.put("installmentAmount",   str(loan.getInstallmentAmount()));
            loanInfo.put("outstandingPrincipal",str(loan.getOutstandingPrincipal()));
            loanInfo.put("totalOutstanding",    str(loan.getTotalOutstanding()));
            loanInfo.put("tenureMonths",        loan.getTenureMonths() != null ? loan.getTenureMonths() : 0);
            loanInfo.put("currentDpd",          loan.getCurrentDpd() != null ? loan.getCurrentDpd() : 0);
            loanInfo.put("disbursementDate",    str(loan.getDisbursementDate()));
            loanInfo.put("maturityDate",        str(loan.getMaturityDate()));
            loanInfo.put("bookingDate",         str(loan.getBookingDate()));
        } else {
            tab.putNull("activeLoan");
        }
    }

    /** Tab 3: Employment & Salary Details */
    private void buildEmploymentSalary(ObjectNode root, LoanApplicationJpaEntity app, JsonNode c360) {
        ObjectNode tab = root.putObject("employmentSalary");

        // Masdar/GOSI verified data from loan application
        ObjectNode verified = tab.putObject("verifiedEmployment");
        verified.put("employerName",       nvl(app.getEmployerName()));
        verified.put("employmentSector",   nvl(app.getEmploymentSector()));
        verified.put("employmentStatus",   nvl(app.getEmploymentStatus()));
        verified.put("basicSalary",        str(app.getBasicSalary()));
        verified.put("totalSalary",        str(app.getTotalSalary()));
        verified.put("verifiedSalary",     str(app.getVerifiedSalary()));
        verified.put("employmentStartDate",nvl(app.getEmploymentStartDate()));
        verified.put("source",             "GOSI/MASDAR");

        // Customer employment history from Customer360
        JsonNode emps = safeGetArray(c360, "employments");
        tab.set("employmentHistory", emps);

        // Expense breakdown
        ObjectNode expenses = tab.putObject("monthlyExpenses");
        expenses.put("monthlyIncome",      str(app.getMonthlyIncome()));
        expenses.put("totalExpenses",      str(app.getTotalExpenses()));
        expenses.put("existingLiabilities",str(app.getExistingLiabilities()));
        expenses.put("foodGroceries",      str(app.getFoodGroceries()));
        expenses.put("utilities",          str(app.getUtilities()));
        expenses.put("healthcare",         str(app.getHealthcare()));
        expenses.put("communication",      str(app.getCommunication()));
        expenses.put("housingRent",        str(app.getHousingRent()));
        expenses.put("clothingEssentials", str(app.getClothingEssentials()));
        expenses.put("education",          str(app.getEducation()));
        expenses.put("transportation",     str(app.getTransportation()));
        expenses.put("adultDependents",    app.getAdultDependents() != null ? app.getAdultDependents() : 0);
        expenses.put("childDependents",    app.getChildDependents() != null ? app.getChildDependents() : 0);
    }

    /** Tab 4: Documents */
    private void buildDocuments(ObjectNode root, LoanApplicationJpaEntity app) {
        ObjectNode tab = root.putObject("documents");

        // Contract & signature status
        ObjectNode contract = tab.putObject("contractInfo");
        contract.put("authorizeDigitalSignature", app.getAuthorizeDigitalSignature() != null && app.getAuthorizeDigitalSignature());
        contract.put("authorizeSellCommodity",    app.getAuthorizeSellCommodity() != null && app.getAuthorizeSellCommodity());
        contract.put("wantPhysicalDelivery",      app.getWantPhysicalDelivery() != null && app.getWantPhysicalDelivery());
        contract.put("commodityTradeId",          nvl(app.getCommodityTradeId()));
        contract.put("contractExpiresAt",         str(app.getContractExpiresAt()));

        // AML declaration
        ObjectNode aml = tab.putObject("amlDeclaration");
        aml.put("completed",   app.getAmlDeclarationCompleted() != null && app.getAmlDeclarationCompleted());
        aml.put("completedAt", str(app.getAmlDeclarationAt()));

        // SafeWatch screening
        ObjectNode safewatch = tab.putObject("safewatchScreening");
        safewatch.put("sessionId", nvl(app.getSafeWatchSessionId()));
        safewatch.put("status",    nvl(app.getSafeWatchStatus()));
    }

    /** Tab 5: Simah Check */
    private void buildSimahCheck(ObjectNode root, LoanApplicationJpaEntity app, JsonNode c360) {
        ObjectNode tab = root.putObject("simahCheck");

        // Consumer inquiry data
        ObjectNode inquiry = tab.putObject("consumerInquiry");
        inquiry.put("simahConsent",      app.getSimahConsent() != null && app.getSimahConsent());
        inquiry.put("simahConsentAt",    str(app.getSimahConsentAt()));
        inquiry.put("creditScore",       app.getCreditScore() != null ? app.getCreditScore() : 0);
        inquiry.put("simahReferenceId",  nvl(app.getSimahReferenceId()));
        inquiry.put("maxEligibleAmount", str(app.getMaxEligibleAmount()));
        inquiry.put("verifiedSalary",    str(app.getVerifiedSalary()));

        // KYC status from Customer360
        JsonNode kyc = safeGet(c360, "kycInfo");
        ObjectNode kycNode = tab.putObject("kycInfo");
        kycNode.put("kycStatus",        text(kyc, "kycStatus"));
        kycNode.put("riskLevel",        text(kyc, "riskLevel"));
        kycNode.put("riskScore",        text(kyc, "riskScore"));
        kycNode.put("isPep",            kyc != null && !kyc.isNull() && kyc.path("isPep").asBoolean(false));
        kycNode.put("complianceStatus", text(kyc, "complianceStatus"));

        // KYC steps
        tab.set("kycSteps", safeGetArray(c360, "kycSteps"));
    }

    /** Tab 6: Open Banking Check */
    private void buildOpenBankingCheck(ObjectNode root, LoanApplicationJpaEntity app, JsonNode c360) {
        ObjectNode tab = root.putObject("openBankingCheck");

        // Disbursement bank account from loan application
        ObjectNode bankStatement = tab.putObject("disbursementAccount");
        bankStatement.put("bankCode",          nvl(app.getDisbursementBankCode()));
        bankStatement.put("bankName",          nvl(app.getDisbursementBankName()));
        bankStatement.put("iban",              nvl(app.getDisbursementIban()));
        bankStatement.put("accountHolder",     nvl(app.getDisbursementAccountHolder()));
        bankStatement.put("ibanVerified",      app.getIbanVerified() != null && app.getIbanVerified());
        bankStatement.put("paymentGuardStatus",nvl(app.getPaymentGuardStatus()));

        // All bank accounts from Customer360
        tab.set("bankAccounts", safeGetArray(c360, "bankAccounts"));
    }

    /** Tab 7: Compliance Check */
    private void buildComplianceCheck(ObjectNode root, JsonNode c360) {
        ObjectNode tab = root.putObject("complianceCheck");

        JsonNode kyc = safeGet(c360, "kycInfo");
        tab.set("kycInfo", kyc != null ? kyc : objectMapper.createObjectNode());
        tab.set("kycWeightageData",         safeGetArray(c360, "kycWeightageData"));
        tab.set("complianceQuestionHistory", safeGetArray(c360, "complianceQuestionHistory"));
    }

    /** Tab 8: Credit Check */
    private void buildCreditCheck(ObjectNode root, JsonNode c360) {
        ObjectNode tab = root.putObject("creditCheck");

        // Risk calculation (score analysis, formula, components)
        JsonNode rc = safeGet(c360, "riskCalculation");
        tab.set("riskCalculation", rc != null ? rc : objectMapper.createObjectNode());

        // KYC weightage breakdown
        tab.set("kycWeightageData", safeGetArray(c360, "kycWeightageData"));

        // Risk history
        tab.set("riskHistory", safeGetArray(c360, "riskHistory"));

        // Risk info summary
        JsonNode ri = safeGet(c360, "riskInfo");
        tab.set("riskInfo", ri != null ? ri : objectMapper.createObjectNode());
    }

    /** Tab 9: Approval */
    private void buildApproval(ObjectNode root, LoanApplicationJpaEntity app, LoanJpaEntity loan) {
        ObjectNode tab = root.putObject("approval");

        tab.put("applicationStatus", nvl(app.getStatus()));
        tab.put("currentStage",      nvl(app.getCurrentStage()));
        tab.put("applicationNumber", nvl(app.getApplicationNumber()));
        tab.put("offeredAmount",     str(app.getOfferedAmount()));
        tab.put("acceptedAmount",    str(app.getAcceptedAmount()));
        tab.put("monthlyInstallment",str(app.getOfferedMonthlyInstallment()));
        tab.put("totalPayable",      str(app.getOfferedTotalPayable()));
        tab.put("profitRate",        str(app.getProfitRate()));
        tab.put("tenureMonths",      app.getRequestedTenureMonths() != null ? app.getRequestedTenureMonths() : 0);
        tab.put("submittedAt",       str(app.getSubmittedAt()));
        tab.put("otpVerified",       app.getOtpVerified() != null && app.getOtpVerified());
        tab.put("ivrVerified",       app.getIvrVerified() != null && app.getIvrVerified());

        if (loan != null) {
            tab.put("loanStatus",       nvl(loan.getStatus()));
            tab.put("loanNumber",       nvl(loan.getLoanNumber()));
            tab.put("disbursementDate", str(loan.getDisbursementDate()));
        } else {
            tab.putNull("loanStatus");
            tab.putNull("loanNumber");
            tab.putNull("disbursementDate");
        }
    }

    /** Tab 10: Rescheduling Request */
    private void buildReschedulingRequest(ObjectNode root, List<LoanRescheduleJpaEntity> reschedules) {
        ObjectNode tab = root.putObject("reschedulingRequest");
        tab.put("total", reschedules.size());

        ArrayNode list = tab.putArray("requests");
        for (LoanRescheduleJpaEntity r : reschedules) {
            ObjectNode item = list.addObject();
            item.put("rescheduleId",     r.getId().toString());
            item.put("rescheduleType",   nvl(r.getRescheduleType()));
            item.put("status",           nvl(r.getStatus()));
            item.put("details",          generateRescheduleDetails(r));
            item.put("justification",    nvl(r.getJustification()));
            item.put("extensionMonths",  r.getExtensionMonths() != null ? r.getExtensionMonths() : 0);
            item.put("holidayMonths",    r.getHolidayMonths() != null ? r.getHolidayMonths() : 0);
            item.put("requestedSkipMonth", r.getRequestedSkipMonth() != null ? r.getRequestedSkipMonth().toString() : null);
            item.put("newProfitRate",    str(r.getNewProfitRate()));
            item.put("writeOffAmount",   str(r.getWriteOffAmount()));
            item.put("oldInstallment",   str(r.getOldInstallmentAmount()));
            item.put("newInstallment",   str(r.getNewInstallmentAmount()));
            item.put("oldTenureMonths",  r.getOldTenureMonths() != null ? r.getOldTenureMonths() : 0);
            item.put("newTenureMonths",  r.getNewTenureMonths() != null ? r.getNewTenureMonths() : 0);
            item.put("oldMaturityDate",  r.getOldMaturityDate() != null ? r.getOldMaturityDate().toString() : null);
            item.put("newMaturityDate",  r.getNewMaturityDate() != null ? r.getNewMaturityDate().toString() : null);
            item.put("approverRole",     nvl(r.getApproverRole()));
            item.put("approvalNotes",    nvl(r.getApprovalNotes()));
            item.put("rejectionReason",  nvl(r.getRejectionReason()));
            item.put("approvedAt",       r.getApprovedAt() != null ? r.getApprovedAt().toString() : null);
            item.put("createdAt",        r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    private JsonNode fetchCustomer360(String customerId, String bearerToken) {
        try {
            String url = customerServiceUrl + "/api/v1/customers/" + customerId + "/360";
            log.debug("Fetching Customer360: url={}", url);
            var headers = new HttpHeaders();
            headers.setBearerAuth(bearerToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode node = objectMapper.readTree(response.getBody());
                // Customer service wraps response in { "data": {...} } — unwrap if needed
                if (node.has("data") && !node.get("data").isNull()) {
                    log.debug("Customer360 fetched OK (unwrapped data): customerId={}", customerId);
                    return node.get("data");
                }
                log.debug("Customer360 fetched OK: customerId={}", customerId);
                return node;
            }
            log.warn("Customer360 non-2xx response: status={} customerId={}", response.getStatusCode(), customerId);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Customer360 not found: customerId={}", customerId);
        } catch (HttpClientErrorException e) {
            log.warn("Customer360 HTTP error: status={} customerId={} body={}", e.getStatusCode(), customerId, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("Customer360 fetch failed: customerId={} error={}", customerId, e.getMessage());
        }
        return objectMapper.createObjectNode();
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

    private String extractTenantId(Jwt jwt) {
        String tenantId = jwt != null ? jwt.getClaimAsString("tenant_id") : null;
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "No tenant_id claim found in JWT token");
        }
        return tenantId;
    }

    private JsonNode safeGet(JsonNode node, String field) {
        if (node == null || node.isNull()) return null;
        JsonNode val = node.path(field);
        return (val.isMissingNode() || val.isNull()) ? null : val;
    }

    private JsonNode safeGetArray(JsonNode node, String field) {
        if (node == null || node.isNull()) return objectMapper.createArrayNode();
        JsonNode val = node.path(field);
        return (val.isMissingNode() || val.isNull()) ? objectMapper.createArrayNode() : val;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isNull()) return null;
        JsonNode val = node.path(field);
        return (val.isMissingNode() || val.isNull()) ? null : val.asText(null);
    }

    private String nvl(String value) {
        return value != null ? value : null;
    }

    /** Return first non-null, non-blank value */
    private String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private String str(Object value) {
        return value != null ? value.toString() : null;
    }
}
