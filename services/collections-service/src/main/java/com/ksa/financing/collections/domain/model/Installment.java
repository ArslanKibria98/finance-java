package com.ksa.financing.collections.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Installment entity — child of RepaymentScheduleAggregate.
 * Tracks scheduled vs paid amounts. Zero framework imports.
 */
public class Installment {

    private final UUID id;
    private final UUID tenantId;
    private final UUID scheduleId;
    private final UUID loanId;
    private final int installmentNumber;
    private LocalDate dueDate;  // mutable via changeDueDate() for test-only backdating

    // Scheduled amounts
    private final BigDecimal principalAmount;
    private final BigDecimal profitAmount;
    private final BigDecimal feeAmount;
    private BigDecimal latePenaltyAmount;  // accrued via DelinquencyEngine (LATE_PAYMENT rule, type=3)
    private BigDecimal totalAmount;        // principal + profit + fee + latePenalty (refreshed on penalty tick)

    // Paid amounts (mutable)
    private BigDecimal paidPrincipal;
    private BigDecimal paidProfit;
    private BigDecimal paidFee;
    private BigDecimal paidTotal;

    // Status
    private InstallmentStatus status;
    private int dpd;
    private LocalDate paidDate;

    private Installment(UUID id, UUID tenantId, UUID scheduleId, UUID loanId,
                        int installmentNumber, LocalDate dueDate,
                        BigDecimal principalAmount, BigDecimal profitAmount,
                        BigDecimal feeAmount) {
        this.id = id;
        this.tenantId = tenantId;
        this.scheduleId = scheduleId;
        this.loanId = loanId;
        this.installmentNumber = installmentNumber;
        this.dueDate = dueDate;
        this.principalAmount = principalAmount;
        this.profitAmount = profitAmount;
        this.feeAmount = feeAmount;
        this.latePenaltyAmount = BigDecimal.ZERO;
        this.totalAmount = principalAmount.add(profitAmount).add(feeAmount);
        this.paidPrincipal = BigDecimal.ZERO;
        this.paidProfit = BigDecimal.ZERO;
        this.paidFee = BigDecimal.ZERO;
        this.paidTotal = BigDecimal.ZERO;
        this.status = InstallmentStatus.SCHEDULED;
        this.dpd = 0;
    }

    public static Installment create(UUID tenantId, UUID scheduleId, UUID loanId,
                                     int installmentNumber, LocalDate dueDate,
                                     BigDecimal principalAmount, BigDecimal profitAmount,
                                     BigDecimal feeAmount) {
        if (principalAmount == null || principalAmount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Principal amount cannot be negative");
        if (profitAmount == null || profitAmount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Profit amount cannot be negative");
        if (installmentNumber <= 0)
            throw new IllegalArgumentException("Installment number must be positive");
        if (dueDate == null)
            throw new IllegalArgumentException("Due date cannot be null");

        return new Installment(UUID.randomUUID(), tenantId, scheduleId, loanId,
                installmentNumber, dueDate, principalAmount, profitAmount,
                feeAmount != null ? feeAmount : BigDecimal.ZERO);
    }

    public static Installment reconstitute(UUID id, UUID tenantId, UUID scheduleId, UUID loanId,
                                           int installmentNumber, LocalDate dueDate,
                                           BigDecimal principalAmount, BigDecimal profitAmount,
                                           BigDecimal feeAmount, BigDecimal latePenaltyAmount,
                                           BigDecimal totalAmount,
                                           BigDecimal paidPrincipal, BigDecimal paidProfit,
                                           BigDecimal paidFee, BigDecimal paidTotal,
                                           InstallmentStatus status, int dpd, LocalDate paidDate) {
        var inst = new Installment(id, tenantId, scheduleId, loanId,
                installmentNumber, dueDate, principalAmount, profitAmount, feeAmount);
        inst.latePenaltyAmount = latePenaltyAmount != null ? latePenaltyAmount : BigDecimal.ZERO;
        inst.totalAmount = totalAmount != null ? totalAmount
                : principalAmount.add(profitAmount).add(feeAmount).add(inst.latePenaltyAmount);
        inst.paidPrincipal = paidPrincipal;
        inst.paidProfit = paidProfit;
        inst.paidFee = paidFee;
        inst.paidTotal = paidTotal;
        inst.status = status;
        inst.dpd = dpd;
        inst.paidDate = paidDate;
        return inst;
    }

    /**
     * Sets the late-payment penalty from the LATE_PAYMENT DelinquencyRule.
     * Idempotent — overwrites the existing penalty with the new computed value.
     * The penalty is tracked SEPARATELY from {@code totalAmount} so the scheduled
     * installment total (principal+profit+fee) remains unchanged when a penalty
     * accrues. The penalty appears only in the {@code latePenaltyAmount} field.
     * Sharia: the penalty routes to the charity fund on collection (see
     * DelinquencyRule.charityFundAccount).
     */
    public void applyLatePenalty(BigDecimal penalty) {
        if (status == InstallmentStatus.PAID || status == InstallmentStatus.WAIVED) {
            return;
        }
        BigDecimal next = penalty != null ? penalty : BigDecimal.ZERO;
        if (next.compareTo(this.latePenaltyAmount) == 0) {
            return;
        }
        this.latePenaltyAmount = next;
    }

    /**
     * Applies a payment allocation to this installment.
     * Uses waterfall: fees first, then profit, then principal.
     * Returns actual amount applied.
     */
    public BigDecimal applyAllocation(BigDecimal feeAlloc, BigDecimal profitAlloc, BigDecimal principalAlloc) {
        if (status == InstallmentStatus.PAID || status == InstallmentStatus.WAIVED)
            throw new IllegalStateException("Cannot apply payment to a fully paid or waived installment");

        this.paidFee = this.paidFee.add(feeAlloc);
        this.paidProfit = this.paidProfit.add(profitAlloc);
        this.paidPrincipal = this.paidPrincipal.add(principalAlloc);
        BigDecimal applied = feeAlloc.add(profitAlloc).add(principalAlloc);
        this.paidTotal = this.paidTotal.add(applied);

        // Update status
        BigDecimal outstanding = this.totalAmount.subtract(this.paidTotal);
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = InstallmentStatus.PAID;
            this.paidDate = LocalDate.now();
        } else if (this.paidTotal.compareTo(BigDecimal.ZERO) > 0) {
            this.status = InstallmentStatus.PARTIALLY_PAID;
        }

        return applied;
    }

    /**
     * Test-only hook to shift an installment's due date. Used by the admin test-support
     * endpoint to backdate an installment and exercise the LATE_PAYMENT delinquency rule.
     * Resets status to SCHEDULED so the engine can re-transition to DUE/OVERDUE based on
     * the new date.
     */
    public void changeDueDate(LocalDate newDueDate) {
        if (newDueDate == null) {
            throw new IllegalArgumentException("due date cannot be null");
        }
        if (status == InstallmentStatus.PAID || status == InstallmentStatus.WAIVED) {
            throw new IllegalStateException("Cannot change due date on a PAID or WAIVED installment");
        }
        this.dueDate = newDueDate;
        this.dpd = 0;
        this.status = InstallmentStatus.SCHEDULED;
    }

    public void markDue() {
        if (status == InstallmentStatus.SCHEDULED) {
            this.status = InstallmentStatus.DUE;
        }
    }

    public void markGracePeriod() {
        if (status == InstallmentStatus.DUE) {
            this.status = InstallmentStatus.GRACE_PERIOD;
        }
    }

    public void markOverdue(int dpd) {
        if (status != InstallmentStatus.PAID && status != InstallmentStatus.WAIVED) {
            this.status = InstallmentStatus.OVERDUE;
            this.dpd = dpd;
        }
    }

    public void waive() {
        if (status == InstallmentStatus.PAID)
            throw new IllegalStateException("Cannot waive an already paid installment");
        this.status = InstallmentStatus.WAIVED;
        this.paidDate = LocalDate.now();
    }

    public void defer() {
        if (status == InstallmentStatus.PAID || status == InstallmentStatus.WAIVED)
            throw new IllegalStateException("Cannot defer a paid or waived installment");
        this.status = InstallmentStatus.DEFERRED;
    }

    public BigDecimal getOutstandingAmount() {
        return totalAmount.subtract(paidTotal);
    }

    public BigDecimal getOutstandingFee() {
        return feeAmount.subtract(paidFee);
    }

    public BigDecimal getOutstandingLatePenalty() {
        return latePenaltyAmount;
    }

    public BigDecimal getLatePenaltyAmount() {
        return latePenaltyAmount;
    }

    public BigDecimal getOutstandingProfit() {
        return profitAmount.subtract(paidProfit);
    }

    public BigDecimal getOutstandingPrincipal() {
        return principalAmount.subtract(paidPrincipal);
    }

    public boolean isFullyPaid() {
        return status == InstallmentStatus.PAID;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getScheduleId() { return scheduleId; }
    public UUID getLoanId() { return loanId; }
    public int getInstallmentNumber() { return installmentNumber; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPaidPrincipal() { return paidPrincipal; }
    public BigDecimal getPaidProfit() { return paidProfit; }
    public BigDecimal getPaidFee() { return paidFee; }
    public BigDecimal getPaidTotal() { return paidTotal; }
    public InstallmentStatus getStatus() { return status; }
    public int getDpd() { return dpd; }
    public LocalDate getPaidDate() { return paidDate; }
}
