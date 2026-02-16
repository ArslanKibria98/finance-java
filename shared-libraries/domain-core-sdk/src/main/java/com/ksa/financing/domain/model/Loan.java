package com.ksa.financing.domain.model;

import com.ksa.financing.domain.enums.LoanStatus;
import com.ksa.financing.domain.enums.ShariaStructure;
import com.ksa.financing.domain.event.DomainEvent;
import com.ksa.financing.domain.valueobject.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Loan aggregate root representing an Islamic financing loan.
 * <p>
 * This is the main aggregate root for the loan lifecycle, managing:
 * - Loan identity and references (contract, customer, product)
 * - Financial terms (principal, profit rate, tenure)
 * - Lifecycle state (status transitions)
 * - Outstanding balances (principal and profit)
 * - Delinquency tracking (DPD)
 * </p>
 * <p>
 * Business invariants enforced:
 * - Can only disburse if approved (PENDING_DISBURSEMENT status)
 * - Can only settle if active (ACTIVE, DELINQUENT, or DEFAULT status)
 * - Outstanding amounts cannot be negative
 * - Status transitions must follow valid lifecycle
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public class Loan {

    private final LoanId loanId;
    private final ContractId contractId;
    private final CustomerId customerId;
    private final ProductId productId;
    private final SarMoney principal;
    private final ProfitRate profitRate;
    private final SarMoney profitAmount;
    private final SarMoney totalAmount;
    private final Tenure tenure;
    private final ShariaStructure shariaStructure;
    private final LocalDate bookingDate;

    private LoanStatus status;
    private LocalDate disbursementDate;
    private LocalDate maturityDate;
    private SarMoney outstandingPrincipal;
    private SarMoney outstandingProfit;
    private int currentDPD;
    private int maxDPD;

    private final List<DomainEvent> domainEvents;

    /**
     * Private constructor for creating loan instances.
     * Use factory methods instead.
     */
    private Loan(LoanId loanId, ContractId contractId, CustomerId customerId, ProductId productId,
                 SarMoney principal, ProfitRate profitRate, SarMoney profitAmount, SarMoney totalAmount,
                 Tenure tenure, ShariaStructure shariaStructure, LocalDate bookingDate,
                 LoanStatus status, LocalDate disbursementDate, LocalDate maturityDate,
                 SarMoney outstandingPrincipal, SarMoney outstandingProfit,
                 int currentDPD, int maxDPD) {
        this.loanId = Objects.requireNonNull(loanId, "LoanId cannot be null");
        this.contractId = Objects.requireNonNull(contractId, "ContractId cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "CustomerId cannot be null");
        this.productId = Objects.requireNonNull(productId, "ProductId cannot be null");
        this.principal = Objects.requireNonNull(principal, "Principal cannot be null");
        this.profitRate = Objects.requireNonNull(profitRate, "ProfitRate cannot be null");
        this.profitAmount = Objects.requireNonNull(profitAmount, "ProfitAmount cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "TotalAmount cannot be null");
        this.tenure = Objects.requireNonNull(tenure, "Tenure cannot be null");
        this.shariaStructure = Objects.requireNonNull(shariaStructure, "ShariaStructure cannot be null");
        this.bookingDate = Objects.requireNonNull(bookingDate, "BookingDate cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.disbursementDate = disbursementDate;
        this.maturityDate = maturityDate;
        this.outstandingPrincipal = Objects.requireNonNull(outstandingPrincipal, "OutstandingPrincipal cannot be null");
        this.outstandingProfit = Objects.requireNonNull(outstandingProfit, "OutstandingProfit cannot be null");
        this.currentDPD = currentDPD;
        this.maxDPD = maxDPD;
        this.domainEvents = new ArrayList<>();

        validateInvariants();
    }

    /**
     * Factory method to create a new loan in PENDING_DISBURSEMENT status.
     *
     * @param contractId the contract identifier
     * @param customerId the customer identifier
     * @param productId the product identifier
     * @param principal the principal amount
     * @param profitRate the profit rate
     * @param tenure the loan tenure
     * @param shariaStructure the Sharia structure
     * @param bookingDate the booking date
     * @return a new Loan instance
     */
    public static Loan createNew(ContractId contractId, CustomerId customerId, ProductId productId,
                                 SarMoney principal, ProfitRate profitRate, Tenure tenure,
                                 ShariaStructure shariaStructure, LocalDate bookingDate) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        Objects.requireNonNull(profitRate, "ProfitRate cannot be null");

        if (!principal.isPositive()) {
            throw new IllegalArgumentException("Principal must be positive");
        }

        // Calculate profit amount and total amount
        SarMoney profitAmount = profitRate.multiply(principal);
        SarMoney totalAmount = principal.add(profitAmount);

        Loan loan = new Loan(
                LoanId.generate(),
                contractId,
                customerId,
                productId,
                principal,
                profitRate,
                profitAmount,
                totalAmount,
                tenure,
                shariaStructure,
                bookingDate,
                LoanStatus.PENDING_DISBURSEMENT,
                null,
                null,
                principal, // Initially, outstanding principal equals principal
                profitAmount, // Initially, outstanding profit equals profit amount
                0,
                0
        );

        loan.addDomainEvent(new LoanCreatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                loan.loanId,
                loan.customerId,
                loan.principal,
                loan.totalAmount
        ));

        return loan;
    }

    /**
     * Approve the loan and mark it ready for disbursement.
     * This is an alias for the natural loan lifecycle.
     */
    public void approve() {
        if (status != LoanStatus.PENDING_DISBURSEMENT) {
            throw new IllegalStateException(
                    String.format("Cannot approve loan in status %s. Must be PENDING_DISBURSEMENT", status)
            );
        }
        // Status remains PENDING_DISBURSEMENT but could trigger approval event
        addDomainEvent(new LoanApprovedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                loanId
        ));
    }

    /**
     * Reject the loan application.
     * Loans cannot be rejected once disbursed.
     */
    public void reject() {
        if (status != LoanStatus.PENDING_DISBURSEMENT) {
            throw new IllegalStateException(
                    String.format("Cannot reject loan in status %s. Must be PENDING_DISBURSEMENT", status)
            );
        }
        this.status = LoanStatus.CLOSED;
        addDomainEvent(new LoanRejectedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                loanId
        ));
    }

    /**
     * Disburse the loan to the customer.
     * Can only disburse if loan is in PENDING_DISBURSEMENT status.
     *
     * @param disbursementDate the date of disbursement
     */
    public void disburse(LocalDate disbursementDate) {
        Objects.requireNonNull(disbursementDate, "DisbursementDate cannot be null");

        if (status != LoanStatus.PENDING_DISBURSEMENT) {
            throw new IllegalStateException(
                    String.format("Cannot disburse loan in status %s. Must be PENDING_DISBURSEMENT", status)
            );
        }

        if (disbursementDate.isBefore(bookingDate)) {
            throw new IllegalArgumentException("Disbursement date cannot be before booking date");
        }

        this.status = LoanStatus.ACTIVE;
        this.disbursementDate = disbursementDate;
        this.maturityDate = tenure.getEndDate(disbursementDate);

        addDomainEvent(new LoanDisbursedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                loanId,
                principal,
                disbursementDate,
                maturityDate
        ));
    }

    /**
     * Mark the loan as overdue when payments are late.
     *
     * @param dpd the days past due
     */
    public void markOverdue(int dpd) {
        if (!status.isActive()) {
            throw new IllegalStateException(
                    String.format("Cannot mark loan overdue in status %s", status)
            );
        }

        if (dpd < 0) {
            throw new IllegalArgumentException("DPD cannot be negative");
        }

        this.currentDPD = dpd;
        if (dpd > maxDPD) {
            this.maxDPD = dpd;
        }

        // Update status based on DPD
        if (dpd > 90) {
            this.status = LoanStatus.DEFAULT;
        } else if (dpd > 0) {
            this.status = LoanStatus.DELINQUENT;
        }

        addDomainEvent(new LoanMarkedOverdueEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                loanId,
                dpd,
                status
        ));
    }

    /**
     * Mark the loan as active (DPD = 0).
     * Used when loan becomes current after being overdue.
     */
    public void markActive() {
        if (!status.isActive()) {
            throw new IllegalStateException(
                    String.format("Cannot mark loan active from status %s", status)
            );
        }

        this.currentDPD = 0;
        this.status = LoanStatus.ACTIVE;

        addDomainEvent(new LoanMarkedActiveEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                loanId
        ));
    }

    /**
     * Settle the loan when fully paid.
     * Can only settle if loan is active (ACTIVE, DELINQUENT, or DEFAULT).
     *
     * @param settlementDate the date of settlement
     */
    public void settle(LocalDate settlementDate) {
        Objects.requireNonNull(settlementDate, "SettlementDate cannot be null");

        if (!status.canAcceptPayment()) {
            throw new IllegalStateException(
                    String.format("Cannot settle loan in status %s", status)
            );
        }

        this.status = LoanStatus.SETTLED;
        this.outstandingPrincipal = SarMoney.zero();
        this.outstandingProfit = SarMoney.zero();
        this.currentDPD = 0;

        addDomainEvent(new LoanSettledEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                loanId,
                settlementDate
        ));
    }

    /**
     * Write off the loan as bad debt.
     * Used when loan is deemed uncollectible.
     */
    public void writeOff() {
        if (status.isFinalState()) {
            throw new IllegalStateException(
                    String.format("Cannot write off loan in final status %s", status)
            );
        }

        this.status = LoanStatus.WRITTEN_OFF;

        addDomainEvent(new LoanWrittenOffEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                loanId,
                outstandingPrincipal,
                outstandingProfit
        ));
    }

    /**
     * Update outstanding balances after a payment.
     *
     * @param principalPaid the principal amount paid
     * @param profitPaid the profit amount paid
     */
    public void recordPayment(SarMoney principalPaid, SarMoney profitPaid) {
        Objects.requireNonNull(principalPaid, "PrincipalPaid cannot be null");
        Objects.requireNonNull(profitPaid, "ProfitPaid cannot be null");

        if (!status.canAcceptPayment()) {
            throw new IllegalStateException(
                    String.format("Cannot record payment for loan in status %s", status)
            );
        }

        this.outstandingPrincipal = this.outstandingPrincipal.subtract(principalPaid);
        this.outstandingProfit = this.outstandingProfit.subtract(profitPaid);

        if (this.outstandingPrincipal.isNegative() || this.outstandingProfit.isNegative()) {
            throw new IllegalStateException("Outstanding amounts cannot be negative");
        }

        addDomainEvent(new PaymentRecordedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                loanId,
                principalPaid,
                profitPaid,
                outstandingPrincipal,
                outstandingProfit
        ));
    }

    /**
     * Validate business invariants.
     */
    private void validateInvariants() {
        if (principal.isNegative()) {
            throw new IllegalArgumentException("Principal cannot be negative");
        }
        if (profitAmount.isNegative()) {
            throw new IllegalArgumentException("Profit amount cannot be negative");
        }
        if (totalAmount.isNegative()) {
            throw new IllegalArgumentException("Total amount cannot be negative");
        }
        if (outstandingPrincipal.isNegative()) {
            throw new IllegalArgumentException("Outstanding principal cannot be negative");
        }
        if (outstandingProfit.isNegative()) {
            throw new IllegalArgumentException("Outstanding profit cannot be negative");
        }
        if (currentDPD < 0) {
            throw new IllegalArgumentException("Current DPD cannot be negative");
        }
        if (maxDPD < 0) {
            throw new IllegalArgumentException("Max DPD cannot be negative");
        }
    }

    /**
     * Add a domain event to the list.
     *
     * @param event the domain event
     */
    private void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    /**
     * Get all domain events and clear the list.
     *
     * @return unmodifiable list of domain events
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Clear all domain events.
     * Should be called after events have been published.
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // Getters

    public LoanId getLoanId() {
        return loanId;
    }

    public ContractId getContractId() {
        return contractId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public SarMoney getPrincipal() {
        return principal;
    }

    public ProfitRate getProfitRate() {
        return profitRate;
    }

    public SarMoney getProfitAmount() {
        return profitAmount;
    }

    public SarMoney getTotalAmount() {
        return totalAmount;
    }

    public Tenure getTenure() {
        return tenure;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public ShariaStructure getShariaStructure() {
        return shariaStructure;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public LocalDate getDisbursementDate() {
        return disbursementDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public SarMoney getOutstandingPrincipal() {
        return outstandingPrincipal;
    }

    public SarMoney getOutstandingProfit() {
        return outstandingProfit;
    }

    public int getCurrentDPD() {
        return currentDPD;
    }

    public int getMaxDPD() {
        return maxDPD;
    }

    // Domain Event Classes (inner classes for simplicity)

    public record LoanCreatedEvent(UUID eventId, LocalDateTime occurredOn, LoanId loanId,
                                    CustomerId customerId, SarMoney principal, SarMoney totalAmount)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }

    public record LoanApprovedEvent(UUID eventId, LocalDateTime occurredOn, LoanId loanId)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }

    public record LoanRejectedEvent(UUID eventId, LocalDateTime occurredOn, LoanId loanId)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }

    public record LoanDisbursedEvent(UUID eventId, LocalDateTime occurredOn, LoanId loanId,
                                      SarMoney amount, LocalDate disbursementDate, LocalDate maturityDate)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }

    public record LoanMarkedOverdueEvent(UUID eventId, LocalDateTime occurredOn, LoanId loanId,
                                          int dpd, LoanStatus newStatus)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }

    public record LoanMarkedActiveEvent(UUID eventId, LocalDateTime occurredOn, LoanId loanId)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }

    public record LoanSettledEvent(UUID eventId, LocalDateTime occurredOn, LoanId loanId,
                                    LocalDate settlementDate)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }

    public record LoanWrittenOffEvent(UUID eventId, LocalDateTime occurredOn, LoanId loanId,
                                       SarMoney outstandingPrincipal, SarMoney outstandingProfit)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }

    public record PaymentRecordedEvent(UUID eventId, LocalDateTime occurredOn, LoanId loanId,
                                        SarMoney principalPaid, SarMoney profitPaid,
                                        SarMoney remainingPrincipal, SarMoney remainingProfit)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }
}
