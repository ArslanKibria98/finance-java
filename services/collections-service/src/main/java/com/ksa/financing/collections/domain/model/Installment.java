package com.ksa.financing.collections.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Installment entity — child of RepaymentScheduleAggregate.
 * Tracks scheduled vs paid amounts, late-penalty accrual + waivers,
 * write-off eligibility + execution. Zero framework imports.
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
    private BigDecimal latePenaltyAmount;   // accrued via DelinquencyEngine (LATE_PAYMENT rule)
    private BigDecimal waivedPenaltyAmount; // cumulative waived from latePenaltyAmount
    private BigDecimal totalAmount;         // principal + profit + fee + latePenalty (refreshed on penalty tick)

    // Paid amounts (mutable)
    private BigDecimal paidPrincipal;
    private BigDecimal paidProfit;
    private BigDecimal paidFee;
    private BigDecimal paidTotal;

    // Status
    private InstallmentStatus status;
    private int dpd;
    private LocalDate paidDate;

    // Write-off eligibility + execution
    private boolean isEligibleForWriteOff;
    private LocalDateTime eligibilityEvaluatedAt;
    private BigDecimal writtenOffPrincipal;
    private BigDecimal writtenOffProfit;
    private BigDecimal writtenOffFee;
    private BigDecimal writtenOffPenalty;
    private LocalDate writeOffDate;
    private String writeOffReason;
    private UUID writtenOffBy;

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
        this.waivedPenaltyAmount = BigDecimal.ZERO;
        this.totalAmount = principalAmount.add(profitAmount).add(feeAmount);
        this.paidPrincipal = BigDecimal.ZERO;
        this.paidProfit = BigDecimal.ZERO;
        this.paidFee = BigDecimal.ZERO;
        this.paidTotal = BigDecimal.ZERO;
        this.status = InstallmentStatus.SCHEDULED;
        this.dpd = 0;
        this.writtenOffPrincipal = BigDecimal.ZERO;
        this.writtenOffProfit = BigDecimal.ZERO;
        this.writtenOffFee = BigDecimal.ZERO;
        this.writtenOffPenalty = BigDecimal.ZERO;
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

    /** Hydrates the write-off + waiver fields. Called from the persistence mapper. */
    public void hydrateWriteOffState(boolean eligible, LocalDateTime evaluatedAt,
                                     BigDecimal waivedPenalty,
                                     BigDecimal woPrincipal, BigDecimal woProfit,
                                     BigDecimal woFee, BigDecimal woPenalty,
                                     LocalDate writeOffDate, String reason, UUID writtenOffBy) {
        this.isEligibleForWriteOff = eligible;
        this.eligibilityEvaluatedAt = evaluatedAt;
        this.waivedPenaltyAmount = waivedPenalty != null ? waivedPenalty : BigDecimal.ZERO;
        this.writtenOffPrincipal = woPrincipal != null ? woPrincipal : BigDecimal.ZERO;
        this.writtenOffProfit = woProfit != null ? woProfit : BigDecimal.ZERO;
        this.writtenOffFee = woFee != null ? woFee : BigDecimal.ZERO;
        this.writtenOffPenalty = woPenalty != null ? woPenalty : BigDecimal.ZERO;
        this.writeOffDate = writeOffDate;
        this.writeOffReason = reason;
        this.writtenOffBy = writtenOffBy;
    }

    /**
     * Sets the late-payment penalty from the LATE_PAYMENT DelinquencyRule.
     * Idempotent — overwrites the existing penalty with the new computed value.
     * Penalty routes to the charity fund on collection (Sharia).
     */
    public void applyLatePenalty(BigDecimal penalty) {
        if (status == InstallmentStatus.PAID
                || status == InstallmentStatus.WAIVED
                || status == InstallmentStatus.WRITTEN_OFF) {
            return;
        }
        BigDecimal next = penalty != null ? penalty : BigDecimal.ZERO;
        if (next.compareTo(this.latePenaltyAmount) == 0) {
            return;
        }
        BigDecimal diff = next.subtract(this.latePenaltyAmount);
        this.totalAmount = this.totalAmount.add(diff);
        this.latePenaltyAmount = next;
    }

    /**
     * Applies a payment allocation to this installment.
     * Uses waterfall: fees first, then profit, then principal.
     */
    public BigDecimal applyAllocation(BigDecimal feeAlloc, BigDecimal profitAlloc, BigDecimal principalAlloc) {
        if (status == InstallmentStatus.PAID
                || status == InstallmentStatus.WAIVED
                || status == InstallmentStatus.WRITTEN_OFF)
            throw new IllegalStateException("Cannot apply payment to a fully paid, waived, or written-off installment");

        this.paidFee = this.paidFee.add(feeAlloc);
        this.paidProfit = this.paidProfit.add(profitAlloc);
        this.paidPrincipal = this.paidPrincipal.add(principalAlloc);
        BigDecimal applied = feeAlloc.add(profitAlloc).add(principalAlloc);
        this.paidTotal = this.paidTotal.add(applied);

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
     * Applies a penalty allocation to this installment.
     */
    public BigDecimal applyPenaltyAllocation(BigDecimal penaltyAlloc) {
        if (status == InstallmentStatus.PAID
                || status == InstallmentStatus.WAIVED
                || status == InstallmentStatus.WRITTEN_OFF)
            throw new IllegalStateException("Cannot apply payment to a fully paid, waived, or written-off installment");

        this.paidTotal = this.paidTotal.add(penaltyAlloc);

        BigDecimal outstanding = this.totalAmount.subtract(this.paidTotal);
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = InstallmentStatus.PAID;
            this.paidDate = LocalDate.now();
        } else if (this.paidTotal.compareTo(BigDecimal.ZERO) > 0) {
            this.status = InstallmentStatus.PARTIALLY_PAID;
        }

        return penaltyAlloc;
    }

    public void changeDueDate(LocalDate newDueDate) {
        if (newDueDate == null) {
            throw new IllegalArgumentException("due date cannot be null");
        }
        if (status == InstallmentStatus.PAID
                || status == InstallmentStatus.WAIVED
                || status == InstallmentStatus.WRITTEN_OFF) {
            throw new IllegalStateException("Cannot change due date on a PAID, WAIVED, or WRITTEN_OFF installment");
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
        if (status != InstallmentStatus.PAID
                && status != InstallmentStatus.WAIVED
                && status != InstallmentStatus.WRITTEN_OFF) {
            this.status = InstallmentStatus.OVERDUE;
            this.dpd = dpd;
        }
    }

    public void waive() {
        if (status == InstallmentStatus.PAID)
            throw new IllegalStateException("Cannot waive an already paid installment");
        if (status == InstallmentStatus.WRITTEN_OFF)
            throw new IllegalStateException("Cannot waive a written-off installment");
        this.status = InstallmentStatus.WAIVED;
        this.paidDate = LocalDate.now();
    }

    public void defer() {
        if (status == InstallmentStatus.PAID
                || status == InstallmentStatus.WAIVED
                || status == InstallmentStatus.WRITTEN_OFF)
            throw new IllegalStateException("Cannot defer a paid, waived, or written-off installment");
        this.status = InstallmentStatus.DEFERRED;
    }

    // ================== WRITE-OFF ELIGIBILITY ==================

    /**
     * Toggle the write-off eligibility flag. Called by {@code WriteOffEligibilityService}
     * after evaluating the installment's DPD against the WRITE_OFFS DelinquencyRule.
     */
    public void setEligibleForWriteOff(boolean eligible) {
        if (status == InstallmentStatus.PAID
                || status == InstallmentStatus.WAIVED
                || status == InstallmentStatus.WRITTEN_OFF) {
            this.isEligibleForWriteOff = false;
            return;
        }
        this.isEligibleForWriteOff = eligible;
        this.eligibilityEvaluatedAt = LocalDateTime.now();
    }

    // ================== PENALTY WAIVER ==================

    /**
     * Waives part or all of the accrued late penalty. Returns the amount actually waived.
     * Safeguards:
     *   - Cannot waive if status is PAID / WAIVED / WRITTEN_OFF
     *   - Cannot waive more than the currently remaining penalty
     */
    public BigDecimal waivePenalty(BigDecimal amount) {
        if (status == InstallmentStatus.PAID
                || status == InstallmentStatus.WAIVED
                || status == InstallmentStatus.WRITTEN_OFF) {
            throw new IllegalStateException("Cannot waive penalty on a finalized installment");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Waiver amount must be positive");
        }
        BigDecimal remainingPenalty = getRemainingPenalty();
        if (remainingPenalty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No penalty remaining to waive");
        }
        BigDecimal actual = amount.min(remainingPenalty);
        this.waivedPenaltyAmount = this.waivedPenaltyAmount.add(actual);
        return actual;
    }

    public BigDecimal getRemainingPenalty() {
        return latePenaltyAmount.subtract(waivedPenaltyAmount).max(BigDecimal.ZERO);
    }

    // ================== WRITE-OFF EXECUTION ==================

    /**
     * Marks the installment as WRITTEN_OFF and records the written-off amount
     * breakdown. Safeguards:
     *   - Must be in an overdue-compatible state (OVERDUE / PARTIALLY_PAID / DUE / GRACE_PERIOD / DEFERRED)
     *   - Must be flagged as eligible by the evaluator (unless override=true)
     *   - Idempotent: silently returns if already written-off
     */
    public WriteOffAmounts writeOff(String reason, UUID actorId, LocalDate asOf, boolean override) {
        if (status == InstallmentStatus.WRITTEN_OFF) {
            return new WriteOffAmounts(writtenOffPrincipal, writtenOffProfit,
                    writtenOffFee, writtenOffPenalty);
        }
        if (status == InstallmentStatus.PAID || status == InstallmentStatus.WAIVED) {
            throw new IllegalStateException("Cannot write off a PAID or WAIVED installment");
        }
        if (!override && !isEligibleForWriteOff) {
            throw new IllegalStateException(
                    "Installment is not eligible for write-off (delinquency threshold not met)");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Write-off reason is required");
        }
        if (actorId == null) {
            throw new IllegalArgumentException("Actor id is required");
        }

        BigDecimal woPrincipal = getOutstandingPrincipal();
        BigDecimal woProfit    = getOutstandingProfit();
        BigDecimal woFee       = getOutstandingFee();
        BigDecimal woPenalty   = getRemainingPenalty();

        this.writtenOffPrincipal = woPrincipal;
        this.writtenOffProfit    = woProfit;
        this.writtenOffFee       = woFee;
        this.writtenOffPenalty   = woPenalty;
        this.writeOffDate        = asOf != null ? asOf : LocalDate.now();
        this.writeOffReason      = reason;
        this.writtenOffBy        = actorId;
        this.status              = InstallmentStatus.WRITTEN_OFF;
        this.isEligibleForWriteOff = false;

        return new WriteOffAmounts(woPrincipal, woProfit, woFee, woPenalty);
    }

    /** Reverses a prior write-off (e.g., late recovery). Returns installment to OVERDUE. */
    public void reverseWriteOff(int dpdAsOfReversal) {
        if (status != InstallmentStatus.WRITTEN_OFF) {
            throw new IllegalStateException("Can only reverse a WRITTEN_OFF installment");
        }
        this.writtenOffPrincipal = BigDecimal.ZERO;
        this.writtenOffProfit    = BigDecimal.ZERO;
        this.writtenOffFee       = BigDecimal.ZERO;
        this.writtenOffPenalty   = BigDecimal.ZERO;
        this.writeOffDate        = null;
        this.writeOffReason      = null;
        this.writtenOffBy        = null;
        this.status              = InstallmentStatus.OVERDUE;
        this.dpd                 = Math.max(0, dpdAsOfReversal);
    }

    public BigDecimal getOutstandingAmount() {
        return totalAmount.subtract(paidTotal);
    }

    public BigDecimal getOutstandingFee() {
        return feeAmount.subtract(paidFee);
    }

    public BigDecimal getOutstandingLatePenalty() {
        return getRemainingPenalty();
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

    public boolean isWrittenOff() {
        return status == InstallmentStatus.WRITTEN_OFF;
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

    public boolean isEligibleForWriteOff() { return isEligibleForWriteOff; }
    public LocalDateTime getEligibilityEvaluatedAt() { return eligibilityEvaluatedAt; }
    public BigDecimal getWaivedPenaltyAmount() { return waivedPenaltyAmount; }
    public BigDecimal getWrittenOffPrincipal() { return writtenOffPrincipal; }
    public BigDecimal getWrittenOffProfit() { return writtenOffProfit; }
    public BigDecimal getWrittenOffFee() { return writtenOffFee; }
    public BigDecimal getWrittenOffPenalty() { return writtenOffPenalty; }
    public LocalDate getWriteOffDate() { return writeOffDate; }
    public String getWriteOffReason() { return writeOffReason; }
    public UUID getWrittenOffBy() { return writtenOffBy; }

    /** Amounts written off in a single write-off transaction. */
    public record WriteOffAmounts(
            BigDecimal principal,
            BigDecimal profit,
            BigDecimal fee,
            BigDecimal penalty
    ) {
        public BigDecimal total() {
            return principal.add(profit).add(fee).add(penalty);
        }
    }
}
