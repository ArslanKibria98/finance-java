package com.ksa.financing.lending.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root for loan applications. Matches the mobile app UI stepper:
 * Basic Information → Add Bank Account → Checking Eligibility → Accept Offer → Sign Contract
 *
 * Enforces all business invariants and emits domain events on state changes.
 * Zero framework imports.
 */
public class LoanApplicationAggregate {

    private final LoanApplicationId id;
    private final UUID tenantId;
    private final String applicationNumber;
    private final UUID customerId;
    private final String nationalId;

    // Pre-qualification data (from "Check Eligibility" screen)
    private BigDecimal monthlyIncome;
    private BigDecimal totalExpenses;
    private BigDecimal existingLiabilities;
    private int adultDependents;
    private int childDependents;

    // Individual expense categories (BRD Section 3.3)
    private BigDecimal foodGroceries;
    private BigDecimal utilities;
    private BigDecimal healthcare;
    private BigDecimal communication;
    private BigDecimal housingRent;
    private BigDecimal clothingEssentials;
    private BigDecimal education;
    private BigDecimal transportation;

    // Step 1: Basic Information
    private UUID productId;
    private String productCode;
    private String productName;
    private ShariaStructure shariaStructure;
    private BigDecimal requestedAmount;
    private int requestedTenureMonths;
    private String purposeOfFinance;
    private String purposeOfFinanceOther;
    private BigDecimal profitRate;
    private BigDecimal processingFeePercent;
    private BigDecimal processingFeeAmount; // Re-used for Step 1 storage
    private BigDecimal adminFeeAmount;      // Re-used for Step 1 storage
    private BigDecimal apr;
    private UUID partnerId;
    private UUID leadId;

    // Step 2: Bank Account
    private String disbursementBankCode;
    private String disbursementBankName;
    private String disbursementIban;
    private String disbursementAccountHolder;
    private boolean ibanVerified;

    // SafeWatch AML screening (BRD Phase 1)
    private String safeWatchSessionId;
    private String safeWatchStatus;

    // Masdar employment verification (BRD Phase 5)
    private String employerName;
    private String employmentSector;
    private String employmentStatus;
    private BigDecimal basicSalary;
    private BigDecimal totalSalary;
    private String employmentStartDate;

    // AML declaration (BRD Phase 6)
    private boolean amlDeclarationCompleted;
    private LocalDateTime amlDeclarationAt;

    // Step 3: SIMAH / Eligibility
    private boolean simahConsent;
    private LocalDateTime simahConsentAt;
    private int creditScore;
    private String simahReferenceId;
    private BigDecimal verifiedSalary;
    private BigDecimal dbrBefore;
    private BigDecimal dbrAfter;
    private BigDecimal maxEligibleAmount;

    // Step 4: Offer
    private BigDecimal offeredAmount;
    private BigDecimal offeredMonthlyInstallment;
    private BigDecimal offeredTotalProfit;
    private BigDecimal offeredTotalPayable;
    private BigDecimal processingFee;
    private BigDecimal adminFee;
    private BigDecimal acceptedAmount;

    // Step 5: Contract
    private LocalDateTime contractExpiresAt;
    private boolean authorizeDigitalSignature;
    private boolean authorizeSellCommodity;
    private boolean wantPhysicalDelivery;
    private String commodityTradeId;
    private boolean otpVerified;
    private int otpAttempts;
    private boolean ivrVerified;
    private int ivrAttempts;

    // NABA notification (BRD Phase 8)
    private boolean nabaNotificationSent;

    // PaymentGuard fraud check
    private String paymentGuardSessionId;
    private String paymentGuardStatus;

    // Status & Workflow
    private ApplicationStatus status;
    private String workflowId;
    private String currentStage;

    // Timing
    private LocalDateTime submittedAt;
    private LocalDateTime expiresAt;

    // Idempotency
    private String idempotencyKey;

    // Disbursement delay (snapshot from product at apply time, 0 = immediate)
    private int disbursementDurationHours;
    private LocalDateTime disbursementScheduledAt;

    // Real wall-clock moment the application entered AWAIT_DISBURSED. Persisted so that
    // any later mutation (which would move updatedAt) cannot wipe the transition timestamp.
    private LocalDateTime awaitDisbursedAt;

    // Audit
    private final LocalDateTime createdAt;
    private final UUID createdBy;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
    private int version;

    // Domain events
    private final List<Object> uncommittedEvents = new ArrayList<>();

    private LoanApplicationAggregate(LoanApplicationId id, UUID tenantId, String applicationNumber,
                                      UUID customerId, String nationalId, UUID createdBy,
                                      LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.applicationNumber = applicationNumber;
        this.customerId = customerId;
        this.nationalId = nationalId;
        this.status = ApplicationStatus.DRAFT;
        this.createdAt = createdAt;
        this.updatedAt = this.createdAt;
        this.createdBy = createdBy;
        this.version = 1;
    }

    // ==================== FACTORY METHOD ====================

    public static LoanApplicationAggregate create(UUID tenantId, String applicationNumber,
                                                   UUID customerId, String nationalId,
                                                   BigDecimal monthlyIncome, BigDecimal totalExpenses,
                                                   BigDecimal existingLiabilities,
                                                   int adultDependents, int childDependents,
                                                   BigDecimal foodGroceries, BigDecimal utilities,
                                                   BigDecimal healthcare, BigDecimal communication,
                                                   BigDecimal housingRent, BigDecimal clothingEssentials,
                                                   BigDecimal education, BigDecimal transportation,
                                                   UUID productId, BigDecimal requestedAmount,
                                                   Integer requestedTenureMonths,
                                                   String purposeOfFinance, String purposeOfFinanceOther,
                                                   UUID createdBy) {
        if (tenantId == null) throw new IllegalArgumentException("Tenant ID cannot be null");
        if (applicationNumber == null || applicationNumber.isBlank())
            throw new IllegalArgumentException("Application number cannot be empty");
        if (customerId == null) throw new IllegalArgumentException("Customer ID cannot be null");

        var aggregate = new LoanApplicationAggregate(
                LoanApplicationId.generate(), tenantId, applicationNumber,
                customerId, nationalId, createdBy, LocalDateTime.now()
        );

        aggregate.monthlyIncome = monthlyIncome;
        aggregate.totalExpenses = totalExpenses;
        aggregate.existingLiabilities = existingLiabilities;
        aggregate.adultDependents = adultDependents;
        aggregate.childDependents = childDependents;
        aggregate.foodGroceries = foodGroceries;
        aggregate.utilities = utilities;
        aggregate.healthcare = healthcare;
        aggregate.communication = communication;
        aggregate.housingRent = housingRent;
        aggregate.clothingEssentials = clothingEssentials;
        aggregate.education = education;
        aggregate.transportation = transportation;

        // Pre-populate intent fields from initiate so reads (e.g. /latest-application)
        // can surface product/amount/tenure even before the Step 1 basic-info signal lands.
        aggregate.productId = productId;
        aggregate.requestedAmount = requestedAmount;
        aggregate.requestedTenureMonths = requestedTenureMonths != null ? requestedTenureMonths : 0;
        aggregate.purposeOfFinance = purposeOfFinance;
        aggregate.purposeOfFinanceOther = purposeOfFinanceOther;

        aggregate.registerEvent(new LoanApplicationCreated(
                aggregate.id, tenantId, customerId, applicationNumber
        ));

        return aggregate;
    }

    // ==================== RECONSTITUTION ====================

    public static LoanApplicationAggregate reconstitute(
            LoanApplicationId id, UUID tenantId, String applicationNumber,
            UUID customerId, String nationalId,
            BigDecimal monthlyIncome, BigDecimal totalExpenses, BigDecimal existingLiabilities,
            int adultDependents, int childDependents,
            BigDecimal foodGroceries, BigDecimal utilities, BigDecimal healthcare,
            BigDecimal communication, BigDecimal housingRent, BigDecimal clothingEssentials,
            BigDecimal education, BigDecimal transportation,
            UUID productId, String productCode, String productName,
            ShariaStructure shariaStructure, BigDecimal requestedAmount, int requestedTenureMonths,
            String purposeOfFinance, String purposeOfFinanceOther,
            BigDecimal profitRate, BigDecimal processingFeePercent, BigDecimal processingFeeAmount,
            BigDecimal adminFeeAmount, BigDecimal apr, UUID partnerId, UUID leadId,
            String safeWatchSessionId, String safeWatchStatus,
            String employerName, String employmentSector, String employmentStatus,
            BigDecimal basicSalary, BigDecimal totalSalary, String employmentStartDate,
            boolean amlDeclarationCompleted, LocalDateTime amlDeclarationAt,
            String disbursementBankCode, String disbursementBankName,
            String disbursementIban, String disbursementAccountHolder, boolean ibanVerified,
            boolean simahConsent, LocalDateTime simahConsentAt, int creditScore,
            String simahReferenceId, BigDecimal verifiedSalary, BigDecimal dbrBefore, BigDecimal dbrAfter,
            BigDecimal maxEligibleAmount,
            BigDecimal offeredAmount, BigDecimal offeredMonthlyInstallment,
            BigDecimal offeredTotalProfit, BigDecimal offeredTotalPayable,
            BigDecimal processingFee, BigDecimal adminFee, BigDecimal acceptedAmount,
            LocalDateTime contractExpiresAt, boolean authorizeDigitalSignature,
            boolean authorizeSellCommodity, boolean wantPhysicalDelivery,
            String commodityTradeId, boolean otpVerified, int otpAttempts,
            boolean ivrVerified, int ivrAttempts,
            boolean nabaNotificationSent, String paymentGuardSessionId, String paymentGuardStatus,
            ApplicationStatus status, String workflowId, String currentStage,
            LocalDateTime submittedAt, LocalDateTime expiresAt, String idempotencyKey,
            UUID createdBy, LocalDateTime createdAt, UUID updatedBy, LocalDateTime updatedAt, int version) {

        var agg = new LoanApplicationAggregate(id, tenantId, applicationNumber, customerId, nationalId, createdBy, createdAt);

        agg.monthlyIncome = monthlyIncome;
        agg.totalExpenses = totalExpenses;
        agg.existingLiabilities = existingLiabilities;
        agg.adultDependents = adultDependents;
        agg.childDependents = childDependents;
        agg.foodGroceries = foodGroceries;
        agg.utilities = utilities;
        agg.healthcare = healthcare;
        agg.communication = communication;
        agg.housingRent = housingRent;
        agg.clothingEssentials = clothingEssentials;
        agg.education = education;
        agg.transportation = transportation;
        agg.productId = productId;
        agg.productCode = productCode;
        agg.productName = productName;
        agg.shariaStructure = shariaStructure;
        agg.requestedAmount = requestedAmount;
        agg.requestedTenureMonths = requestedTenureMonths;
        agg.purposeOfFinance = purposeOfFinance;
        agg.purposeOfFinanceOther = purposeOfFinanceOther;
        agg.profitRate = profitRate;
        agg.processingFeePercent = processingFeePercent;
        agg.processingFeeAmount = processingFeeAmount;
        agg.adminFeeAmount = adminFeeAmount;
        agg.apr = apr;
        agg.partnerId = partnerId;
        agg.leadId = leadId;
        agg.safeWatchSessionId = safeWatchSessionId;
        agg.safeWatchStatus = safeWatchStatus;
        agg.employerName = employerName;
        agg.employmentSector = employmentSector;
        agg.employmentStatus = employmentStatus;
        agg.basicSalary = basicSalary;
        agg.totalSalary = totalSalary;
        agg.employmentStartDate = employmentStartDate;
        agg.amlDeclarationCompleted = amlDeclarationCompleted;
        agg.amlDeclarationAt = amlDeclarationAt;
        agg.disbursementBankCode = disbursementBankCode;
        agg.disbursementBankName = disbursementBankName;
        agg.disbursementIban = disbursementIban;
        agg.disbursementAccountHolder = disbursementAccountHolder;
        agg.ibanVerified = ibanVerified;
        agg.simahConsent = simahConsent;
        agg.simahConsentAt = simahConsentAt;
        agg.creditScore = creditScore;
        agg.simahReferenceId = simahReferenceId;
        agg.verifiedSalary = verifiedSalary;
        agg.dbrBefore = dbrBefore;
        agg.dbrAfter = dbrAfter;
        agg.maxEligibleAmount = maxEligibleAmount;
        agg.offeredAmount = offeredAmount;
        agg.offeredMonthlyInstallment = offeredMonthlyInstallment;
        agg.offeredTotalProfit = offeredTotalProfit;
        agg.offeredTotalPayable = offeredTotalPayable;
        agg.processingFee = processingFee;
        agg.adminFee = adminFee;
        agg.acceptedAmount = acceptedAmount;
        agg.contractExpiresAt = contractExpiresAt;
        agg.authorizeDigitalSignature = authorizeDigitalSignature;
        agg.authorizeSellCommodity = authorizeSellCommodity;
        agg.wantPhysicalDelivery = wantPhysicalDelivery;
        agg.commodityTradeId = commodityTradeId;
        agg.otpVerified = otpVerified;
        agg.otpAttempts = otpAttempts;
        agg.ivrVerified = ivrVerified;
        agg.ivrAttempts = ivrAttempts;
        agg.nabaNotificationSent = nabaNotificationSent;
        agg.paymentGuardSessionId = paymentGuardSessionId;
        agg.paymentGuardStatus = paymentGuardStatus;
        agg.status = status;
        agg.workflowId = workflowId;
        agg.currentStage = currentStage;
        agg.submittedAt = submittedAt;
        agg.expiresAt = expiresAt;
        agg.idempotencyKey = idempotencyKey;
        agg.updatedBy = updatedBy;
        agg.updatedAt = updatedAt;
        agg.version = version;
        return agg;
    }

    // ==================== STEP 1: BASIC INFORMATION ====================

    public void submitBasicInfo(UUID productId, String productCode, String productName,
                                 ShariaStructure shariaStructure, BigDecimal requestedAmount,
                                 int requestedTenureMonths, String purposeOfFinance,
                                 String purposeOfFinanceOther,
                                 BigDecimal profitRate, 
                                 BigDecimal processingFeePercent, BigDecimal processingFeeAmount,
                                 BigDecimal adminFeeAmount, BigDecimal apr,
                                 UUID partnerId, UUID leadId, UUID updatedBy) {
        assertTransition(ApplicationStatus.BASIC_INFO_SUBMITTED);
        if (productId == null && (productCode == null || productCode.isBlank()))
            throw new IllegalArgumentException("Either Product ID or Product Code must be provided");
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Requested amount must be positive");
        if (requestedTenureMonths <= 0)
            throw new IllegalArgumentException("Requested tenure must be positive");

        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.shariaStructure = shariaStructure;
        this.requestedAmount = requestedAmount;
        this.requestedTenureMonths = requestedTenureMonths;
        this.purposeOfFinance = purposeOfFinance;
        this.purposeOfFinanceOther = purposeOfFinanceOther;
        this.profitRate = profitRate;
        this.processingFeePercent = processingFeePercent;
        this.processingFeeAmount = processingFeeAmount;
        this.adminFeeAmount = adminFeeAmount;
        this.apr = apr;
        this.partnerId = partnerId;
        this.leadId = leadId;
        this.status = ApplicationStatus.BASIC_INFO_SUBMITTED;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new BasicInfoSubmitted(id, tenantId, customerId, productId, requestedAmount));
    }

    // ==================== STEP 2: BANK ACCOUNT ====================

    public void moveToBankAccountPending(UUID updatedBy) {
        assertTransition(ApplicationStatus.BANK_ACCOUNT_PENDING);
        this.status = ApplicationStatus.BANK_ACCOUNT_PENDING;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void saveBankAccount(String bankCode, String bankName, String iban,
                                 String accountHolder, boolean verified, UUID updatedBy) {
        this.disbursementBankCode = bankCode;
        this.disbursementBankName = bankName;
        this.disbursementIban = iban;
        this.disbursementAccountHolder = accountHolder;
        this.ibanVerified = verified;

        if (verified) {
            assertTransition(ApplicationStatus.BANK_ACCOUNT_VERIFIED);
            this.status = ApplicationStatus.BANK_ACCOUNT_VERIFIED;
        }
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== STEP 3: ELIGIBILITY ====================

    public void recordSimahConsent(boolean consent, UUID updatedBy) {
        assertTransition(ApplicationStatus.SIMAH_CONSENT_GIVEN);
        this.simahConsent = consent;
        this.simahConsentAt = LocalDateTime.now();
        this.status = ApplicationStatus.SIMAH_CONSENT_GIVEN;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void moveToEligibilityChecking(UUID updatedBy) {
        assertTransition(ApplicationStatus.ELIGIBILITY_CHECKING);
        this.status = ApplicationStatus.ELIGIBILITY_CHECKING;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void saveEligibilityResult(boolean eligible, int creditScore, String simahReferenceId,
                                       BigDecimal verifiedSalary, BigDecimal dbrBefore, BigDecimal dbrAfter,
                                       BigDecimal maxEligibleAmount, UUID updatedBy) {
        this.creditScore = creditScore;
        this.simahReferenceId = simahReferenceId;
        this.verifiedSalary = verifiedSalary;
        this.dbrBefore = dbrBefore;
        this.dbrAfter = dbrAfter;
        this.maxEligibleAmount = maxEligibleAmount;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        if (eligible) {
            assertTransition(ApplicationStatus.ELIGIBILITY_PASSED);
            this.status = ApplicationStatus.ELIGIBILITY_PASSED;
            registerEvent(new EligibilityPassed(id, tenantId, customerId, maxEligibleAmount, creditScore));
        } else {
            assertTransition(ApplicationStatus.REJECTED);
            this.status = ApplicationStatus.REJECTED;
            registerEvent(new LoanApplicationRejected(id, tenantId, customerId, "Eligibility check failed"));
        }
    }

    // ==================== STEP 4: OFFER ====================

    public void presentOffer(BigDecimal offeredAmount, BigDecimal monthlyInstallment,
                              BigDecimal totalProfit, BigDecimal totalPayable,
                              BigDecimal processingFee, BigDecimal adminFee, UUID updatedBy) {
        assertTransition(ApplicationStatus.OFFER_PRESENTED);
        this.offeredAmount = offeredAmount;
        this.offeredMonthlyInstallment = monthlyInstallment;
        this.offeredTotalProfit = totalProfit;
        this.offeredTotalPayable = totalPayable;
        this.processingFee = processingFee;
        this.adminFee = adminFee;
        this.status = ApplicationStatus.OFFER_PRESENTED;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void acceptOffer(BigDecimal acceptedAmount, BigDecimal monthlyInstallment,
                             BigDecimal totalPayable, BigDecimal totalProfit, UUID updatedBy) {
        assertTransition(ApplicationStatus.OFFER_ACCEPTED);
        this.acceptedAmount = acceptedAmount;
        this.offeredMonthlyInstallment = monthlyInstallment;
        this.offeredTotalPayable = totalPayable;
        this.offeredTotalProfit = totalProfit;
        this.status = ApplicationStatus.OFFER_ACCEPTED;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new OfferAccepted(id, tenantId, customerId, acceptedAmount));
    }

    // ==================== STEP 5: CONTRACT ====================

    public void moveToContractPending(LocalDateTime contractExpiresAt, UUID updatedBy) {
        assertTransition(ApplicationStatus.CONTRACT_PENDING);
        this.contractExpiresAt = contractExpiresAt;
        this.status = ApplicationStatus.CONTRACT_PENDING;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void recordContractConsent(boolean authorizeDigitalSignature,
                                       boolean authorizeSellCommodity,
                                       boolean wantPhysicalDelivery, UUID updatedBy) {
        assertTransition(ApplicationStatus.CONTRACT_SIGNING);
        this.authorizeDigitalSignature = authorizeDigitalSignature;
        this.authorizeSellCommodity = authorizeSellCommodity;
        this.wantPhysicalDelivery = wantPhysicalDelivery;
        this.status = ApplicationStatus.CONTRACT_SIGNING;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void moveToOtpVerification(UUID updatedBy) {
        assertTransition(ApplicationStatus.OTP_VERIFICATION);
        this.status = ApplicationStatus.OTP_VERIFICATION;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void recordOtpVerified(UUID updatedBy) {
        this.otpVerified = true;
        assertTransition(ApplicationStatus.IVR_VERIFICATION);
        this.status = ApplicationStatus.IVR_VERIFICATION;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void recordOtpAttempt() {
        this.otpAttempts++;
    }

    public void recordIvrAttempt() {
        this.ivrAttempts++;
    }

    public void recordIvrVerified(UUID updatedBy) {
        this.ivrVerified = true;
        assertTransition(ApplicationStatus.CONTRACT_SIGNED);
        this.status = ApplicationStatus.CONTRACT_SIGNED;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new ContractSigned(id, tenantId, customerId));
    }

    // ==================== SAFEWATCH AML SCREENING ====================

    public void recordSafeWatchResult(String sessionId, String safeWatchStatus, UUID updatedBy) {
        this.safeWatchSessionId = sessionId;
        this.safeWatchStatus = safeWatchStatus;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== MASDAR EMPLOYMENT VERIFICATION ====================

    public void recordMasdarResult(String employerName, String employmentSector,
                                    String employmentStatus, BigDecimal basicSalary,
                                    BigDecimal totalSalary, String employmentStartDate,
                                    UUID updatedBy) {
        this.employerName = employerName;
        this.employmentSector = employmentSector;
        this.employmentStatus = employmentStatus;
        this.basicSalary = basicSalary;
        this.totalSalary = totalSalary;
        this.employmentStartDate = employmentStartDate;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== AML DECLARATION ====================

    public void recordAmlDeclaration(UUID updatedBy) {
        this.amlDeclarationCompleted = true;
        this.amlDeclarationAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== NABA NOTIFICATION ====================

    public void recordNabaNotificationSent(UUID updatedBy) {
        this.nabaNotificationSent = true;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== PAYMENT GUARD ====================

    public void recordPaymentGuardResult(String sessionId, String pgStatus, UUID updatedBy) {
        this.paymentGuardSessionId = sessionId;
        this.paymentGuardStatus = pgStatus;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== STEP 6: LOAN CREATION ====================

    public void moveToAwaitDisbursed(UUID updatedBy) {
        assertTransition(ApplicationStatus.AWAIT_DISBURSED);
        var now = LocalDateTime.now();
        this.status = ApplicationStatus.AWAIT_DISBURSED;
        this.awaitDisbursedAt = now;
        this.updatedBy = updatedBy;
        this.updatedAt = now;
    }

    public void moveToLoanCreating(UUID updatedBy) {
        assertTransition(ApplicationStatus.LOAN_CREATING);
        this.status = ApplicationStatus.LOAN_CREATING;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void moveToDisbursing(UUID updatedBy) {
        assertTransition(ApplicationStatus.DISBURSING);
        this.status = ApplicationStatus.DISBURSING;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void approve(UUID updatedBy) {
        assertTransition(ApplicationStatus.APPROVED);
        this.status = ApplicationStatus.APPROVED;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new LoanApplicationApproved(id, tenantId, customerId, productId, acceptedAmount));
    }

    public void markDisbursed(UUID updatedBy) {
        assertTransition(ApplicationStatus.DISBURSED);
        this.status = ApplicationStatus.DISBURSED;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(String reason, UUID rejectedBy) {
        assertTransition(ApplicationStatus.REJECTED);
        this.status = ApplicationStatus.REJECTED;
        this.updatedBy = rejectedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new LoanApplicationRejected(id, tenantId, customerId, reason));
    }

    public void cancel(UUID cancelledBy) {
        assertTransition(ApplicationStatus.CANCELLED);
        this.status = ApplicationStatus.CANCELLED;
        this.updatedBy = cancelledBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void expire() {
        assertTransition(ApplicationStatus.EXPIRED);
        this.status = ApplicationStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    public void setCommodityTradeId(String tradeId) {
        this.commodityTradeId = tradeId;
    }

    public void assignWorkflow(String workflowId) {
        this.workflowId = workflowId;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public void setDisbursementDurationHours(int hours) {
        this.disbursementDurationHours = Math.max(0, hours);
    }

    public void setDisbursementScheduledAt(LocalDateTime scheduledAt) {
        this.disbursementScheduledAt = scheduledAt;
    }

    public void setAwaitDisbursedAt(LocalDateTime awaitDisbursedAt) {
        this.awaitDisbursedAt = awaitDisbursedAt;
    }

    // ==================== EVENT MANAGEMENT ====================

    private void registerEvent(Object event) {
        uncommittedEvents.add(event);
    }

    public List<Object> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    // ==================== ASSERTIONS ====================

    private void assertTransition(ApplicationStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Cannot transition from " + status + " to " + target);
        }
    }

    // ==================== GETTERS ====================

    public LoanApplicationId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getApplicationNumber() { return applicationNumber; }
    public UUID getCustomerId() { return customerId; }
    public String getNationalId() { return nationalId; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public BigDecimal getExistingLiabilities() { return existingLiabilities; }
    public int getAdultDependents() { return adultDependents; }
    public int getChildDependents() { return childDependents; }
    public BigDecimal getFoodGroceries() { return foodGroceries; }
    public BigDecimal getUtilities() { return utilities; }
    public BigDecimal getHealthcare() { return healthcare; }
    public BigDecimal getCommunication() { return communication; }
    public BigDecimal getHousingRent() { return housingRent; }
    public BigDecimal getClothingEssentials() { return clothingEssentials; }
    public BigDecimal getEducation() { return education; }
    public BigDecimal getTransportation() { return transportation; }
    public UUID getProductId() { return productId; }
    public String getProductCode() { return productCode; }
    public String getProductName() { return productName; }
    public ShariaStructure getShariaStructure() { return shariaStructure; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public int getRequestedTenureMonths() { return requestedTenureMonths; }
    public String getPurposeOfFinance() { return purposeOfFinance; }
    public String getPurposeOfFinanceOther() { return purposeOfFinanceOther; }
    public BigDecimal getProfitRate() { return profitRate; }
    public BigDecimal getProcessingFeePercent() { return processingFeePercent; }
    public BigDecimal getProcessingFeeAmount() { return processingFeeAmount; }
    public BigDecimal getAdminFeeAmount() { return adminFeeAmount; }
    public BigDecimal getApr() { return apr; }
    public UUID getPartnerId() { return partnerId; }
    public UUID getLeadId() { return leadId; }
    public String getDisbursementBankCode() { return disbursementBankCode; }
    public String getDisbursementBankName() { return disbursementBankName; }
    public String getDisbursementIban() { return disbursementIban; }
    public String getDisbursementAccountHolder() { return disbursementAccountHolder; }
    public boolean isIbanVerified() { return ibanVerified; }
    public String getSafeWatchSessionId() { return safeWatchSessionId; }
    public String getSafeWatchStatus() { return safeWatchStatus; }
    public String getEmployerName() { return employerName; }
    public String getEmploymentSector() { return employmentSector; }
    public String getEmploymentStatus() { return employmentStatus; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public BigDecimal getTotalSalary() { return totalSalary; }
    public String getEmploymentStartDate() { return employmentStartDate; }
    public boolean isAmlDeclarationCompleted() { return amlDeclarationCompleted; }
    public LocalDateTime getAmlDeclarationAt() { return amlDeclarationAt; }
    public boolean isSimahConsent() { return simahConsent; }
    public LocalDateTime getSimahConsentAt() { return simahConsentAt; }
    public int getCreditScore() { return creditScore; }
    public String getSimahReferenceId() { return simahReferenceId; }
    public BigDecimal getVerifiedSalary() { return verifiedSalary; }
    public BigDecimal getDbrBefore() { return dbrBefore; }
    public BigDecimal getDbrAfter() { return dbrAfter; }
    public BigDecimal getMaxEligibleAmount() { return maxEligibleAmount; }
    public BigDecimal getOfferedAmount() { return offeredAmount; }
    public BigDecimal getOfferedMonthlyInstallment() { return offeredMonthlyInstallment; }
    public BigDecimal getOfferedTotalProfit() { return offeredTotalProfit; }
    public BigDecimal getOfferedTotalPayable() { return offeredTotalPayable; }
    public BigDecimal getProcessingFee() { return processingFee; }
    public BigDecimal getAdminFee() { return adminFee; }
    public BigDecimal getAcceptedAmount() { return acceptedAmount; }
    public LocalDateTime getContractExpiresAt() { return contractExpiresAt; }
    public boolean isAuthorizeDigitalSignature() { return authorizeDigitalSignature; }
    public boolean isAuthorizeSellCommodity() { return authorizeSellCommodity; }
    public boolean isWantPhysicalDelivery() { return wantPhysicalDelivery; }
    public String getCommodityTradeId() { return commodityTradeId; }
    public boolean isOtpVerified() { return otpVerified; }
    public int getOtpAttempts() { return otpAttempts; }
    public boolean isIvrVerified() { return ivrVerified; }
    public int getIvrAttempts() { return ivrAttempts; }
    public boolean isNabaNotificationSent() { return nabaNotificationSent; }
    public String getPaymentGuardSessionId() { return paymentGuardSessionId; }
    public String getPaymentGuardStatus() { return paymentGuardStatus; }
    public ApplicationStatus getStatus() { return status; }
    public String getWorkflowId() { return workflowId; }
    public String getCurrentStage() { return currentStage; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public int getDisbursementDurationHours() { return disbursementDurationHours; }
    public LocalDateTime getDisbursementScheduledAt() { return disbursementScheduledAt; }
    public LocalDateTime getAwaitDisbursedAt() { return awaitDisbursedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public int getVersion() { return version; }

    // ==================== DOMAIN EVENTS ====================

    public record LoanApplicationCreated(
            LoanApplicationId applicationId, UUID tenantId, UUID customerId,
            String applicationNumber
    ) {}

    public record BasicInfoSubmitted(
            LoanApplicationId applicationId, UUID tenantId, UUID customerId,
            UUID productId, BigDecimal requestedAmount
    ) {}

    public record EligibilityPassed(
            LoanApplicationId applicationId, UUID tenantId, UUID customerId,
            BigDecimal maxEligibleAmount, int creditScore
    ) {}

    public record OfferAccepted(
            LoanApplicationId applicationId, UUID tenantId, UUID customerId,
            BigDecimal acceptedAmount
    ) {}

    public record ContractSigned(
            LoanApplicationId applicationId, UUID tenantId, UUID customerId
    ) {}

    public record LoanApplicationApproved(
            LoanApplicationId applicationId, UUID tenantId, UUID customerId,
            UUID productId, BigDecimal approvedAmount
    ) {}

    public record LoanApplicationRejected(
            LoanApplicationId applicationId, UUID tenantId, UUID customerId,
            String reason
    ) {}
}
