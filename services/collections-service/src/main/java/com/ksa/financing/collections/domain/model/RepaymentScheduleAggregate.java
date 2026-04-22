package com.ksa.financing.collections.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root for a loan repayment schedule.
 * Manages installments and applies payment waterfall allocation.
 * Zero framework imports.
 */
public class RepaymentScheduleAggregate {

    private final RepaymentScheduleId id;
    private final UUID tenantId;
    private final String scheduleNumber;
    private final UUID loanId;
    private UUID productId;  // nullable — resolves per-product DelinquencyRule lookups

    private int version;
    private boolean active;

    private final int totalInstallments;
    private final BigDecimal totalPrincipal;
    private final BigDecimal totalProfit;
    private final BigDecimal totalAmount;

    private final LocalDate firstDueDate;
    private final LocalDate lastDueDate;

    private final List<Installment> installments;
    private final List<Object> uncommittedEvents = new ArrayList<>();

    private final LocalDateTime createdAt;
    private UUID createdBy;

    private RepaymentScheduleAggregate(RepaymentScheduleId id, UUID tenantId,
                                        String scheduleNumber, UUID loanId,
                                        BigDecimal totalPrincipal, BigDecimal totalProfit,
                                        LocalDate firstDueDate, LocalDate lastDueDate,
                                        List<Installment> installments) {
        this.id = id;
        this.tenantId = tenantId;
        this.scheduleNumber = scheduleNumber;
        this.loanId = loanId;
        this.totalPrincipal = totalPrincipal;
        this.totalProfit = totalProfit;
        this.totalAmount = totalPrincipal.add(totalProfit);
        this.firstDueDate = firstDueDate;
        this.lastDueDate = lastDueDate;
        this.installments = new ArrayList<>(installments);
        this.totalInstallments = installments.size();
        this.active = true;
        this.version = 1;
        this.createdAt = LocalDateTime.now();
    }

    // ==================== FACTORY METHOD ====================

    public static RepaymentScheduleAggregate create(UUID tenantId, String scheduleNumber,
                                                     UUID loanId, UUID productId,
                                                     BigDecimal totalPrincipal,
                                                     BigDecimal totalProfit,
                                                     LocalDate firstDueDate,
                                                     LocalDate lastDueDate,
                                                     List<Installment> installments,
                                                     UUID createdBy) {
        if (tenantId == null) throw new IllegalArgumentException("Tenant ID cannot be null");
        if (loanId == null) throw new IllegalArgumentException("Loan ID cannot be null");
        if (scheduleNumber == null || scheduleNumber.isBlank())
            throw new IllegalArgumentException("Schedule number cannot be blank");
        if (installments == null || installments.isEmpty())
            throw new IllegalArgumentException("Installments cannot be empty");
        if (totalPrincipal == null || totalPrincipal.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Total principal must be positive");

        var schedule = new RepaymentScheduleAggregate(
                RepaymentScheduleId.generate(), tenantId, scheduleNumber, loanId,
                totalPrincipal, totalProfit, firstDueDate, lastDueDate, installments);
        schedule.productId = productId;
        schedule.createdBy = createdBy;

        schedule.registerEvent(new ScheduleCreated(
                schedule.id, tenantId, loanId, installments.size(), totalPrincipal, totalProfit));

        return schedule;
    }

    public static RepaymentScheduleAggregate reconstitute(RepaymentScheduleId id, UUID tenantId,
                                                           String scheduleNumber, UUID loanId,
                                                           UUID productId,
                                                           int version, boolean active,
                                                           BigDecimal totalPrincipal,
                                                           BigDecimal totalProfit,
                                                           LocalDate firstDueDate,
                                                           LocalDate lastDueDate,
                                                           List<Installment> installments,
                                                           LocalDateTime createdAt,
                                                           UUID createdBy) {
        var schedule = new RepaymentScheduleAggregate(id, tenantId, scheduleNumber, loanId,
                totalPrincipal, totalProfit, firstDueDate, lastDueDate, installments);
        schedule.productId = productId;
        schedule.version = version;
        schedule.active = active;
        schedule.createdBy = createdBy;
        return schedule;
    }

    // ==================== BUSINESS OPERATIONS ====================

    /**
     * Applies a payment using waterfall allocation:
     * 1. Fees (oldest first)
     * 2. Profit (oldest first)
     * 3. Principal (oldest first)
     *
     * Returns list of payment allocations applied.
     */
    public List<PaymentAllocation> applyPayment(UUID paymentId, BigDecimal paymentAmount) {
        if (!active)
            throw new IllegalStateException("Cannot apply payment to inactive schedule");
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Payment amount must be positive");

        List<PaymentAllocation> allocations = new ArrayList<>();
        BigDecimal remaining = paymentAmount;

        // Get unpaid installments sorted by due date (oldest first)
        List<Installment> unpaid = installments.stream()
                .filter(i -> i.getStatus() != InstallmentStatus.PAID
                        && i.getStatus() != InstallmentStatus.WAIVED)
                .sorted(Comparator.comparing(Installment::getDueDate))
                .toList();

        // Phase 1: Fees (oldest first)
        for (Installment inst : unpaid) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal feeOutstanding = inst.getOutstandingFee();
            if (feeOutstanding.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal feeApply = remaining.min(feeOutstanding).setScale(6, RoundingMode.HALF_UP);
            if (feeApply.compareTo(BigDecimal.ZERO) > 0) {
                inst.applyAllocation(feeApply, BigDecimal.ZERO, BigDecimal.ZERO);
                allocations.add(new PaymentAllocation(paymentId, inst.getId(), allocations.size() + 1,
                        BigDecimal.ZERO, BigDecimal.ZERO, feeApply, feeApply));
                remaining = remaining.subtract(feeApply);
            }
        }

        // Phase 2: Profit (oldest first)
        for (Installment inst : unpaid) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal profitOutstanding = inst.getOutstandingProfit();
            if (profitOutstanding.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal profitApply = remaining.min(profitOutstanding).setScale(6, RoundingMode.HALF_UP);
            if (profitApply.compareTo(BigDecimal.ZERO) > 0) {
                inst.applyAllocation(BigDecimal.ZERO, profitApply, BigDecimal.ZERO);
                // Merge with existing fee allocation for same installment if any
                allocations.add(new PaymentAllocation(paymentId, inst.getId(), allocations.size() + 1,
                        BigDecimal.ZERO, profitApply, BigDecimal.ZERO, profitApply));
                remaining = remaining.subtract(profitApply);
            }
        }

        // Phase 3: Principal (oldest first)
        for (Installment inst : unpaid) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal principalOutstanding = inst.getOutstandingPrincipal();
            if (principalOutstanding.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal principalApply = remaining.min(principalOutstanding).setScale(6, RoundingMode.HALF_UP);
            if (principalApply.compareTo(BigDecimal.ZERO) > 0) {
                inst.applyAllocation(BigDecimal.ZERO, BigDecimal.ZERO, principalApply);
                allocations.add(new PaymentAllocation(paymentId, inst.getId(), allocations.size() + 1,
                        principalApply, BigDecimal.ZERO, BigDecimal.ZERO, principalApply));
                remaining = remaining.subtract(principalApply);
            }
        }

        BigDecimal totalApplied = paymentAmount.subtract(remaining);
        registerEvent(new PaymentApplied(id, tenantId, loanId, paymentId, totalApplied, remaining));

        // Check if fully settled
        boolean allPaid = installments.stream()
                .allMatch(i -> i.getStatus() == InstallmentStatus.PAID
                        || i.getStatus() == InstallmentStatus.WAIVED);
        if (allPaid) {
            registerEvent(new ScheduleFullyPaid(id, tenantId, loanId));
        }

        return allocations;
    }

    /**
     * Applies payment to a specific installment (invoice-targeted payment flow).
     */
    public List<PaymentAllocation> applyPaymentToInstallment(
            UUID paymentId, UUID installmentId, BigDecimal paymentAmount) {
        if (!active)
            throw new IllegalStateException("Cannot apply payment to inactive schedule");
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Payment amount must be positive");

        var installment = installments.stream()
                .filter(i -> i.getId().equals(installmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Installment not found: " + installmentId));

        BigDecimal remaining = paymentAmount;
        List<PaymentAllocation> allocations = new ArrayList<>();

        BigDecimal feeApply = remaining.min(installment.getOutstandingFee()).setScale(6, RoundingMode.HALF_UP);
        if (feeApply.compareTo(BigDecimal.ZERO) > 0) {
            installment.applyAllocation(feeApply, BigDecimal.ZERO, BigDecimal.ZERO);
            allocations.add(new PaymentAllocation(paymentId, installmentId, allocations.size() + 1,
                    BigDecimal.ZERO, BigDecimal.ZERO, feeApply, feeApply));
            remaining = remaining.subtract(feeApply);
        }

        BigDecimal profitApply = remaining.min(installment.getOutstandingProfit()).setScale(6, RoundingMode.HALF_UP);
        if (profitApply.compareTo(BigDecimal.ZERO) > 0) {
            installment.applyAllocation(BigDecimal.ZERO, profitApply, BigDecimal.ZERO);
            allocations.add(new PaymentAllocation(paymentId, installmentId, allocations.size() + 1,
                    BigDecimal.ZERO, profitApply, BigDecimal.ZERO, profitApply));
            remaining = remaining.subtract(profitApply);
        }

        BigDecimal principalApply = remaining.min(installment.getOutstandingPrincipal()).setScale(6, RoundingMode.HALF_UP);
        if (principalApply.compareTo(BigDecimal.ZERO) > 0) {
            installment.applyAllocation(BigDecimal.ZERO, BigDecimal.ZERO, principalApply);
            allocations.add(new PaymentAllocation(paymentId, installmentId, allocations.size() + 1,
                    principalApply, BigDecimal.ZERO, BigDecimal.ZERO, principalApply));
            remaining = remaining.subtract(principalApply);
        }

        BigDecimal totalApplied = paymentAmount.subtract(remaining);
        registerEvent(new PaymentApplied(id, tenantId, loanId, paymentId, totalApplied, remaining));

        boolean allPaid = installments.stream()
                .allMatch(i -> i.getStatus() == InstallmentStatus.PAID
                        || i.getStatus() == InstallmentStatus.WAIVED);
        if (allPaid) {
            registerEvent(new ScheduleFullyPaid(id, tenantId, loanId));
        }

        return allocations;
    }

    public void markInstallmentsDue(LocalDate asOf) {
        installments.stream()
                .filter(i -> i.getStatus() == InstallmentStatus.SCHEDULED
                        && !i.getDueDate().isAfter(asOf))
                .forEach(Installment::markDue);
    }

    public void markInstallmentsOverdue(LocalDate asOf, int graceDays) {
        installments.stream()
                .filter(i -> (i.getStatus() == InstallmentStatus.DUE
                        || i.getStatus() == InstallmentStatus.GRACE_PERIOD)
                        && i.getDueDate().plusDays(graceDays).isBefore(asOf))
                .forEach(i -> {
                    int dpd = (int) (asOf.toEpochDay() - i.getDueDate().toEpochDay());
                    i.markOverdue(dpd);
                });
    }

    public void deactivate() {
        this.active = false;
    }

    // ==================== QUERIES ====================

    public BigDecimal getTotalOutstanding() {
        return installments.stream()
                .map(Installment::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getMaxDpd() {
        return installments.stream()
                .mapToInt(Installment::getDpd)
                .max()
                .orElse(0);
    }

    public List<Installment> getOverdueInstallments() {
        return installments.stream()
                .filter(i -> i.getStatus() == InstallmentStatus.OVERDUE)
                .toList();
    }

    // ==================== EVENTS ====================

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

    public RepaymentScheduleId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getScheduleNumber() { return scheduleNumber; }
    public UUID getLoanId() { return loanId; }
    public UUID getProductId() { return productId; }
    public int getVersion() { return version; }
    public boolean isActive() { return active; }
    public int getTotalInstallments() { return totalInstallments; }
    public BigDecimal getTotalPrincipal() { return totalPrincipal; }
    public BigDecimal getTotalProfit() { return totalProfit; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public LocalDate getFirstDueDate() { return firstDueDate; }
    public LocalDate getLastDueDate() { return lastDueDate; }
    public List<Installment> getInstallments() { return Collections.unmodifiableList(installments); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }

    public BigDecimal getPaidPrincipal() {
        return installments.stream().map(Installment::getPaidPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getPaidProfit() {
        return installments.stream().map(Installment::getPaidProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getPaidTotal() {
        return installments.stream().map(Installment::getPaidTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isFullyPaid() {
        return installments.stream()
                .allMatch(i -> i.getStatus() == InstallmentStatus.PAID
                        || i.getStatus() == InstallmentStatus.WAIVED);
    }

    // ==================== VALUE OBJECTS ====================

    public record PaymentAllocation(
            UUID paymentId,
            UUID installmentId,
            int allocationOrder,
            BigDecimal principalAllocated,
            BigDecimal profitAllocated,
            BigDecimal feeAllocated,
            BigDecimal totalAllocated
    ) {}

    // ==================== DOMAIN EVENTS ====================

    public record ScheduleCreated(
            RepaymentScheduleId scheduleId,
            UUID tenantId,
            UUID loanId,
            int totalInstallments,
            BigDecimal totalPrincipal,
            BigDecimal totalProfit
    ) {}

    public record PaymentApplied(
            RepaymentScheduleId scheduleId,
            UUID tenantId,
            UUID loanId,
            UUID paymentId,
            BigDecimal amountApplied,
            BigDecimal remainingUnallocated
    ) {}

    public record ScheduleFullyPaid(
            RepaymentScheduleId scheduleId,
            UUID tenantId,
            UUID loanId
    ) {}
}
