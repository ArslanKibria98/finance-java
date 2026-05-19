package com.ksa.financing.lending.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root for active financing contracts. Created after application approval.
 * Manages loan lifecycle, balances, and DPD tracking. Zero framework imports.
 */
public class LoanAggregate {

    private final LoanId id;
    private final UUID tenantId;
    private final String loanNumber;
    private final LoanApplicationId applicationId;
    private final UUID customerId;
    private final UUID productId;
    private final String productCode;
    private final ShariaStructure shariaStructure;

    // Commodity reference (for Tawarruq)
    private UUID commodityTransactionId;

    // Principal and profit
    private final BigDecimal principalAmount;
    private final BigDecimal profitAmount;
    private final BigDecimal feeAmount;
    private final BigDecimal totalAmount;

    // Terms
    private final BigDecimal profitRate;
    private final int tenureMonths;
    private final BigDecimal installmentAmount;

    // Balances (mutable)
    private BigDecimal outstandingPrincipal;
    private BigDecimal outstandingProfit;
    private BigDecimal outstandingFees;
    private BigDecimal totalOutstanding;

    // Status
    private LoanStatus status;

    // Dates
    private final LocalDate bookingDate;
    private LocalDate disbursementDate;
    private LocalDate firstDueDate;
    private LocalDate maturityDate;
    private LocalDate settlementDate;

    // DPD tracking
    private int currentDpd;
    private int maxDpd;

    // IFRS9
    private int ifrs9Stage;

    // Fineract
    private Long fineractLoanId;

    // Audit
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    // Domain events
    private final List<Object> uncommittedEvents = new ArrayList<>();

    private LoanAggregate(LoanId id, UUID tenantId, String loanNumber,
                           LoanApplicationId applicationId, UUID customerId,
                           UUID productId, String productCode,
                           ShariaStructure shariaStructure,
                           BigDecimal principalAmount, BigDecimal profitAmount,
                           BigDecimal feeAmount,
                           BigDecimal profitRate, int tenureMonths,
                           BigDecimal installmentAmount) {
        this.id = id;
        this.tenantId = tenantId;
        this.loanNumber = loanNumber;
        this.applicationId = applicationId;
        this.customerId = customerId;
        this.productId = productId;
        this.productCode = productCode;
        this.shariaStructure = shariaStructure;
        this.principalAmount = principalAmount;
        this.profitAmount = profitAmount;
        this.feeAmount = feeAmount;
        this.totalAmount = principalAmount.add(profitAmount).add(feeAmount);
        this.profitRate = profitRate;
        this.tenureMonths = tenureMonths;
        this.installmentAmount = installmentAmount;
        this.outstandingPrincipal = principalAmount;
        this.outstandingProfit = profitAmount;
        this.outstandingFees = feeAmount;
        this.totalOutstanding = this.totalAmount;
        this.status = LoanStatus.PENDING_DISBURSEMENT;
        this.bookingDate = LocalDate.now();
        this.currentDpd = 0;
        this.maxDpd = 0;
        this.ifrs9Stage = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.version = 1;
    }

    // ==================== FACTORY METHOD ====================

    public static LoanAggregate create(UUID tenantId, String loanNumber,
                                        LoanApplicationId applicationId, UUID customerId,
                                        UUID productId, String productCode,
                                        ShariaStructure shariaStructure,
                                        BigDecimal principalAmount, BigDecimal profitAmount,
                                        BigDecimal feeAmount,
                                        BigDecimal profitRate, int tenureMonths,
                                        BigDecimal installmentAmount) {
        if (tenantId == null) throw new IllegalArgumentException("Tenant ID cannot be null");
        if (loanNumber == null || loanNumber.isBlank())
            throw new IllegalArgumentException("Loan number cannot be empty");
        if (principalAmount == null || principalAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Principal amount must be positive");
        if (profitAmount == null || profitAmount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Profit amount cannot be negative");

        var loan = new LoanAggregate(
                LoanId.generate(), tenantId, loanNumber, applicationId,
                customerId, productId, productCode, shariaStructure,
                principalAmount, profitAmount, feeAmount,
                profitRate, tenureMonths, installmentAmount
        );

        loan.registerEvent(new LoanCreated(
                loan.id, tenantId, applicationId, customerId,
                principalAmount, profitAmount, feeAmount,
                profitRate, tenureMonths, shariaStructure
        ));

        return loan;
    }

    // ==================== RECONSTITUTION (from persistence) ====================

    public static LoanAggregate reconstitute(
            LoanId id, UUID tenantId, String loanNumber,
            LoanApplicationId applicationId, UUID customerId,
            UUID productId, String productCode, ShariaStructure shariaStructure,
            UUID commodityTransactionId,
            BigDecimal principalAmount, BigDecimal profitAmount,
            BigDecimal feeAmount, BigDecimal totalAmount,
            BigDecimal profitRate, int tenureMonths, BigDecimal installmentAmount,
            BigDecimal outstandingPrincipal, BigDecimal outstandingProfit,
            BigDecimal outstandingFees, BigDecimal totalOutstanding,
            LoanStatus status, LocalDate bookingDate,
            LocalDate disbursementDate, LocalDate firstDueDate,
            LocalDate maturityDate, LocalDate settlementDate,
            int currentDpd, int maxDpd, int ifrs9Stage,
            Long fineractLoanId,
            LocalDateTime createdAt, LocalDateTime updatedAt, int version) {

        var loan = new LoanAggregate(
                id, tenantId, loanNumber, applicationId, customerId,
                productId, productCode, shariaStructure,
                principalAmount, profitAmount, feeAmount,
                profitRate, tenureMonths, installmentAmount);

        loan.commodityTransactionId = commodityTransactionId;
        loan.outstandingPrincipal = outstandingPrincipal;
        loan.outstandingProfit = outstandingProfit;
        loan.outstandingFees = outstandingFees;
        loan.totalOutstanding = totalOutstanding;
        loan.status = status;
        loan.disbursementDate = disbursementDate;
        loan.firstDueDate = firstDueDate;
        loan.maturityDate = maturityDate;
        loan.settlementDate = settlementDate;
        loan.currentDpd = currentDpd;
        loan.maxDpd = maxDpd;
        loan.ifrs9Stage = ifrs9Stage;
        loan.fineractLoanId = fineractLoanId;
        loan.updatedAt = updatedAt;
        loan.version = version;
        // Do NOT register any domain events on reconstitution
        return loan;
    }

    // ==================== BUSINESS OPERATIONS ====================

    public void disburse(LocalDate disbursementDate, LocalDate firstDueDate, LocalDate maturityDate) {
        if (this.status != LoanStatus.PENDING_DISBURSEMENT)
            throw new IllegalStateException("Can only disburse a loan in PENDING_DISBURSEMENT status");

        this.status = LoanStatus.ACTIVE;
        this.disbursementDate = disbursementDate;
        this.firstDueDate = firstDueDate;
        this.maturityDate = maturityDate;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new LoanDisbursed(
                id, tenantId, customerId, principalAmount, disbursementDate
        ));
    }

    public void markDelinquent(int dpd) {
        if (this.status != LoanStatus.ACTIVE)
            throw new IllegalStateException("Can only mark active loan as delinquent");
        this.status = LoanStatus.DELINQUENT;
        this.currentDpd = dpd;
        if (dpd > this.maxDpd) this.maxDpd = dpd;
        updateIfrs9Stage(dpd);
        this.updatedAt = LocalDateTime.now();
    }

    public void cureDelinquency() {
        if (this.status != LoanStatus.DELINQUENT)
            throw new IllegalStateException("Can only cure a delinquent loan");
        this.status = LoanStatus.ACTIVE;
        this.currentDpd = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public void settle(LocalDate settlementDate) {
        if (this.status != LoanStatus.ACTIVE && this.status != LoanStatus.DELINQUENT)
            throw new IllegalStateException("Can only settle an active or delinquent loan");
        this.status = LoanStatus.SETTLED;
        this.settlementDate = settlementDate;
        this.outstandingPrincipal = BigDecimal.ZERO;
        this.outstandingProfit = BigDecimal.ZERO;
        this.outstandingFees = BigDecimal.ZERO;
        this.totalOutstanding = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new LoanSettled(id, tenantId, customerId, settlementDate));
    }

    public void setCommodityTransactionId(UUID commodityTransactionId) {
        this.commodityTransactionId = commodityTransactionId;
    }

    public void setFineractLoanId(Long fineractLoanId) {
        this.fineractLoanId = fineractLoanId;
    }

    // ==================== PRIVATE HELPERS ====================

    private void updateIfrs9Stage(int dpd) {
        if (dpd > 90) this.ifrs9Stage = 3;
        else if (dpd > 30) this.ifrs9Stage = 2;
        else this.ifrs9Stage = 1;
    }

    private void registerEvent(Object event) {
        uncommittedEvents.add(event);
    }

    public List<Object> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    // ==================== GETTERS ====================

    public LoanId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getLoanNumber() { return loanNumber; }
    public LoanApplicationId getApplicationId() { return applicationId; }
    public UUID getCustomerId() { return customerId; }
    public UUID getProductId() { return productId; }
    public String getProductCode() { return productCode; }
    public ShariaStructure getShariaStructure() { return shariaStructure; }
    public UUID getCommodityTransactionId() { return commodityTransactionId; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getProfitRate() { return profitRate; }
    public int getTenureMonths() { return tenureMonths; }
    public void updateOutstandingBalance(BigDecimal newOutstanding) {
        this.totalOutstanding = newOutstanding;
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    public BigDecimal getInstallmentAmount() { return installmentAmount; }
    public BigDecimal getOutstandingPrincipal() { return outstandingPrincipal; }
    public BigDecimal getOutstandingProfit() { return outstandingProfit; }
    public BigDecimal getOutstandingFees() { return outstandingFees; }
    public BigDecimal getTotalOutstanding() { return totalOutstanding; }
    public LoanStatus getStatus() { return status; }
    public LocalDate getBookingDate() { return bookingDate; }
    public LocalDate getDisbursementDate() { return disbursementDate; }
    public LocalDate getFirstDueDate() { return firstDueDate; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public int getCurrentDpd() { return currentDpd; }
    public int getMaxDpd() { return maxDpd; }
    public int getIfrs9Stage() { return ifrs9Stage; }
    public Long getFineractLoanId() { return fineractLoanId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }

    // ==================== DOMAIN EVENTS ====================

    public record LoanCreated(
            LoanId loanId, UUID tenantId, LoanApplicationId applicationId,
            UUID customerId, BigDecimal principalAmount, BigDecimal profitAmount,
            BigDecimal feeAmount, BigDecimal profitRate,
            int tenureMonths, ShariaStructure shariaStructure
    ) {}

    public record LoanDisbursed(
            LoanId loanId, UUID tenantId, UUID customerId,
            BigDecimal amount, LocalDate disbursementDate
    ) {}

    public record LoanSettled(
            LoanId loanId, UUID tenantId, UUID customerId,
            LocalDate settlementDate
    ) {}

    public record LoanRescheduled(
            LoanId loanId, UUID tenantId, UUID customerId,
            int oldTenureMonths, int newTenureMonths,
            BigDecimal newMonthlyInstallment, String reason,
            LocalDate effectiveDate
    ) {}
}
