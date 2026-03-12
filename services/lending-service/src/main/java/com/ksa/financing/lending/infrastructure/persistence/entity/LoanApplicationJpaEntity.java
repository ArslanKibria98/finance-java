package com.ksa.financing.lending.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class LoanApplicationJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "application_number", nullable = false, unique = true)
    private String applicationNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "national_id")
    private String nationalId;

    // ══════════ Pre-qualification ══════════

    @Column(name = "monthly_income", precision = 20, scale = 6)
    private BigDecimal monthlyIncome;

    @Column(name = "total_expenses", precision = 20, scale = 6)
    private BigDecimal totalExpenses;

    @Column(name = "existing_liabilities", precision = 20, scale = 6)
    private BigDecimal existingLiabilities;

    @Column(name = "adult_dependents")
    private Integer adultDependents;

    @Column(name = "child_dependents")
    private Integer childDependents;

    // ══════════ Individual Expense Categories ══════════

    @Column(name = "food_groceries", precision = 20, scale = 6)
    private BigDecimal foodGroceries;

    @Column(name = "utilities", precision = 20, scale = 6)
    private BigDecimal utilities;

    @Column(name = "healthcare", precision = 20, scale = 6)
    private BigDecimal healthcare;

    @Column(name = "communication", precision = 20, scale = 6)
    private BigDecimal communication;

    @Column(name = "housing_rent", precision = 20, scale = 6)
    private BigDecimal housingRent;

    @Column(name = "clothing_essentials", precision = 20, scale = 6)
    private BigDecimal clothingEssentials;

    @Column(name = "education", precision = 20, scale = 6)
    private BigDecimal education;

    @Column(name = "transportation", precision = 20, scale = 6)
    private BigDecimal transportation;

    // ══════════ Step 1: Basic Information ══════════

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "sharia_structure")
    private String shariaStructure;

    @Column(name = "requested_amount", precision = 18, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "requested_tenure_months")
    private Integer requestedTenureMonths;

    @Column(name = "purpose_of_finance")
    private String purposeOfFinance;

    @Column(name = "purpose_of_finance_other")
    private String purposeOfFinanceOther;

    @Column(name = "profit_rate", precision = 10, scale = 8)
    private BigDecimal profitRate;

    @Column(name = "apr", precision = 10, scale = 8)
    private BigDecimal apr;

    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(name = "lead_id")
    private UUID leadId;

    // ══════════ SafeWatch AML Screening ══════════

    @Column(name = "safe_watch_session_id")
    private String safeWatchSessionId;

    @Column(name = "safe_watch_status")
    private String safeWatchStatus;

    // ══════════ Masdar Employment Verification ══════════

    @Column(name = "employer_name")
    private String employerName;

    @Column(name = "employment_sector")
    private String employmentSector;

    @Column(name = "employment_status")
    private String employmentStatus;

    @Column(name = "basic_salary", precision = 20, scale = 6)
    private BigDecimal basicSalary;

    @Column(name = "total_salary", precision = 20, scale = 6)
    private BigDecimal totalSalary;

    @Column(name = "employment_start_date")
    private String employmentStartDate;

    // ══════════ AML Declaration ══════════

    @Column(name = "aml_declaration_completed")
    private Boolean amlDeclarationCompleted;

    @Column(name = "aml_declaration_at")
    private LocalDateTime amlDeclarationAt;

    // ══════════ Step 2: Bank Account ══════════

    @Column(name = "disbursement_bank_code")
    private String disbursementBankCode;

    @Column(name = "disbursement_bank_name")
    private String disbursementBankName;

    @Column(name = "disbursement_iban")
    private String disbursementIban;

    @Column(name = "disbursement_account_holder")
    private String disbursementAccountHolder;

    @Column(name = "iban_verified")
    private Boolean ibanVerified;

    // ══════════ Step 3: SIMAH / Eligibility ══════════

    @Column(name = "simah_consent")
    private Boolean simahConsent;

    @Column(name = "simah_consent_at")
    private LocalDateTime simahConsentAt;

    @Column(name = "credit_score")
    private Integer creditScore;

    @Column(name = "simah_reference_id")
    private String simahReferenceId;

    @Column(name = "verified_salary", precision = 20, scale = 6)
    private BigDecimal verifiedSalary;

    @Column(name = "max_eligible_amount", precision = 20, scale = 6)
    private BigDecimal maxEligibleAmount;

    // ══════════ Step 4: Offer ══════════

    @Column(name = "offered_amount", precision = 20, scale = 6)
    private BigDecimal offeredAmount;

    @Column(name = "offered_monthly_installment", precision = 20, scale = 6)
    private BigDecimal offeredMonthlyInstallment;

    @Column(name = "offered_total_profit", precision = 20, scale = 6)
    private BigDecimal offeredTotalProfit;

    @Column(name = "offered_total_payable", precision = 20, scale = 6)
    private BigDecimal offeredTotalPayable;

    @Column(name = "processing_fee", precision = 20, scale = 6)
    private BigDecimal processingFee;

    @Column(name = "admin_fee", precision = 20, scale = 6)
    private BigDecimal adminFee;

    @Column(name = "accepted_amount", precision = 20, scale = 6)
    private BigDecimal acceptedAmount;

    // ══════════ Step 5: Contract ══════════

    @Column(name = "contract_expires_at")
    private LocalDateTime contractExpiresAt;

    @Column(name = "authorize_digital_signature")
    private Boolean authorizeDigitalSignature;

    @Column(name = "authorize_sell_commodity")
    private Boolean authorizeSellCommodity;

    @Column(name = "want_physical_delivery")
    private Boolean wantPhysicalDelivery;

    @Column(name = "commodity_trade_id")
    private String commodityTradeId;

    @Column(name = "otp_verified")
    private Boolean otpVerified;

    @Column(name = "otp_attempts")
    private Integer otpAttempts;

    @Column(name = "ivr_verified")
    private Boolean ivrVerified;

    @Column(name = "ivr_attempts")
    private Integer ivrAttempts;

    // ══════════ NABA Notification ══════════

    @Column(name = "naba_notification_sent")
    private Boolean nabaNotificationSent;

    // ══════════ PaymentGuard ══════════

    @Column(name = "payment_guard_session_id")
    private String paymentGuardSessionId;

    @Column(name = "payment_guard_status")
    private String paymentGuardStatus;

    // ══════════ Status & Workflow ══════════

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "workflow_id")
    private String workflowId;

    @Column(name = "current_stage")
    private String currentStage;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    // ══════════ Audit ══════════

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
