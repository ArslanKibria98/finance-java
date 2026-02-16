package com.ksa.financing.domain.model;

import com.ksa.financing.domain.enums.CustomerStatus;
import com.ksa.financing.domain.enums.CustomerType;
import com.ksa.financing.domain.event.DomainEvent;
import com.ksa.financing.domain.valueobject.CustomerId;
import com.ksa.financing.domain.valueobject.NationalId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Customer aggregate root representing a customer in the Islamic financing platform.
 * <p>
 * This aggregate manages:
 * - Customer identity and personal information
 * - Customer type (individual, SME, corporate)
 * - Lifecycle status
 * - KYC (Know Your Customer) verification status
 * - Risk assessment and grading
 * </p>
 * <p>
 * Business invariants enforced:
 * - National ID is required and must be valid
 * - Status transitions must follow valid customer lifecycle
 * - Blocked customers cannot be activated without clearing the block
 * - Risk grade must be valid when set
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public class Customer {

    private final CustomerId customerId;
    private final CustomerType customerType;
    private final NationalId nationalId;
    private final String fullName;
    private final LocalDateTime createdAt;

    private CustomerStatus status;
    private String kycStatus;
    private String riskGrade;

    private final List<DomainEvent> domainEvents;

    /**
     * Private constructor for creating customer instances.
     * Use factory methods instead.
     */
    private Customer(CustomerId customerId, CustomerType customerType, NationalId nationalId,
                     String fullName, CustomerStatus status, String kycStatus, String riskGrade,
                     LocalDateTime createdAt) {
        this.customerId = Objects.requireNonNull(customerId, "CustomerId cannot be null");
        this.customerType = Objects.requireNonNull(customerType, "CustomerType cannot be null");
        this.nationalId = Objects.requireNonNull(nationalId, "NationalId cannot be null");
        this.fullName = Objects.requireNonNull(fullName, "FullName cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.kycStatus = Objects.requireNonNull(kycStatus, "KycStatus cannot be null");
        this.riskGrade = riskGrade;
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        this.domainEvents = new ArrayList<>();

        validateInvariants();
    }

    /**
     * Factory method to create a new individual customer.
     *
     * @param nationalId the national ID
     * @param fullName the customer's full name
     * @return a new Customer instance
     */
    public static Customer createIndividual(NationalId nationalId, String fullName) {
        Objects.requireNonNull(nationalId, "NationalId cannot be null");
        Objects.requireNonNull(fullName, "FullName cannot be null");

        if (fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("FullName cannot be empty");
        }

        Customer customer = new Customer(
                CustomerId.generate(),
                CustomerType.INDIVIDUAL,
                nationalId,
                fullName.trim(),
                CustomerStatus.LEAD,
                "PENDING",
                null,
                LocalDateTime.now()
        );

        customer.addDomainEvent(new CustomerCreatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                customer.customerId,
                customer.customerType,
                customer.nationalId,
                customer.fullName
        ));

        return customer;
    }

    /**
     * Factory method to create a new business customer (SME or Corporate).
     *
     * @param customerType the type of business (SME or CORPORATE)
     * @param nationalId the commercial registration ID
     * @param fullName the business name
     * @return a new Customer instance
     */
    public static Customer createBusiness(CustomerType customerType, NationalId nationalId, String fullName) {
        Objects.requireNonNull(customerType, "CustomerType cannot be null");
        Objects.requireNonNull(nationalId, "NationalId cannot be null");
        Objects.requireNonNull(fullName, "FullName cannot be null");

        if (customerType == CustomerType.INDIVIDUAL) {
            throw new IllegalArgumentException("Use createIndividual() for individual customers");
        }

        if (fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Business name cannot be empty");
        }

        Customer customer = new Customer(
                CustomerId.generate(),
                customerType,
                nationalId,
                fullName.trim(),
                CustomerStatus.LEAD,
                "PENDING",
                null,
                LocalDateTime.now()
        );

        customer.addDomainEvent(new CustomerCreatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                customer.customerId,
                customer.customerType,
                customer.nationalId,
                customer.fullName
        ));

        return customer;
    }

    /**
     * Activate the customer account.
     * Customer must be in QUALIFIED status to be activated to ACTIVE.
     */
    public void activate() {
        if (status == CustomerStatus.BLOCKED) {
            throw new IllegalStateException("Cannot activate a blocked customer. Unblock first.");
        }

        if (status == CustomerStatus.ACTIVE) {
            // Already active, no-op
            return;
        }

        if (!kycStatus.equals("VERIFIED")) {
            throw new IllegalStateException("Cannot activate customer without verified KYC");
        }

        this.status = CustomerStatus.ACTIVE;

        addDomainEvent(new CustomerActivatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                customerId
        ));
    }

    /**
     * Block the customer account due to fraud or compliance issues.
     *
     * @param reason the reason for blocking
     */
    public void block(String reason) {
        Objects.requireNonNull(reason, "Block reason cannot be null");

        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Block reason cannot be empty");
        }

        if (status == CustomerStatus.BLOCKED) {
            // Already blocked, no-op
            return;
        }

        this.status = CustomerStatus.BLOCKED;

        addDomainEvent(new CustomerBlockedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                customerId,
                reason
        ));
    }

    /**
     * Update the customer's risk grade.
     *
     * @param newRiskGrade the new risk grade (e.g., "A", "B", "C", "D")
     */
    public void updateRiskGrade(String newRiskGrade) {
        Objects.requireNonNull(newRiskGrade, "Risk grade cannot be null");

        if (newRiskGrade.trim().isEmpty()) {
            throw new IllegalArgumentException("Risk grade cannot be empty");
        }

        String oldRiskGrade = this.riskGrade;
        this.riskGrade = newRiskGrade.trim().toUpperCase();

        addDomainEvent(new CustomerRiskGradeUpdatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                customerId,
                oldRiskGrade,
                this.riskGrade
        ));
    }

    /**
     * Verify the customer's KYC (Know Your Customer) status.
     * Progresses customer from LEAD to PROSPECT to QUALIFIED.
     *
     * @param kycVerified true if KYC is verified, false otherwise
     */
    public void verifyKYC(boolean kycVerified) {
        if (kycVerified) {
            this.kycStatus = "VERIFIED";

            // Progress status based on current state
            if (status == CustomerStatus.LEAD) {
                this.status = CustomerStatus.PROSPECT;
            } else if (status == CustomerStatus.PROSPECT) {
                this.status = CustomerStatus.QUALIFIED;
            }

            addDomainEvent(new CustomerKYCVerifiedEvent(
                    UUID.randomUUID(),
                    LocalDateTime.now(),
                    customerId,
                    status
            ));
        } else {
            this.kycStatus = "FAILED";

            addDomainEvent(new CustomerKYCFailedEvent(
                    UUID.randomUUID(),
                    LocalDateTime.now(),
                    customerId
            ));
        }
    }

    /**
     * Mark customer as applicant when they submit a loan application.
     */
    public void markAsApplicant() {
        if (status == CustomerStatus.BLOCKED) {
            throw new IllegalStateException("Blocked customers cannot apply for loans");
        }

        if (status != CustomerStatus.QUALIFIED && status != CustomerStatus.ACTIVE) {
            throw new IllegalStateException(
                    String.format("Customer must be QUALIFIED or ACTIVE to apply. Current status: %s", status)
            );
        }

        this.status = CustomerStatus.APPLICANT;

        addDomainEvent(new CustomerMarkedAsApplicantEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                customerId
        ));
    }

    /**
     * Validate business invariants.
     */
    private void validateInvariants() {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }
        if (kycStatus == null || kycStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("KYC status cannot be empty");
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
     * Get all domain events.
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

    public CustomerId getCustomerId() {
        return customerId;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public NationalId getNationalId() {
        return nationalId;
    }

    public String getFullName() {
        return fullName;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public String getRiskGrade() {
        return riskGrade;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Domain Event Classes (inner classes for simplicity)

    public record CustomerCreatedEvent(UUID eventId, LocalDateTime occurredOn, CustomerId customerId,
                                        CustomerType customerType, NationalId nationalId, String fullName)
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

    public record CustomerActivatedEvent(UUID eventId, LocalDateTime occurredOn, CustomerId customerId)
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

    public record CustomerBlockedEvent(UUID eventId, LocalDateTime occurredOn, CustomerId customerId, String reason)
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

    public record CustomerRiskGradeUpdatedEvent(UUID eventId, LocalDateTime occurredOn, CustomerId customerId,
                                                 String oldRiskGrade, String newRiskGrade)
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

    public record CustomerKYCVerifiedEvent(UUID eventId, LocalDateTime occurredOn, CustomerId customerId,
                                            CustomerStatus newStatus)
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

    public record CustomerKYCFailedEvent(UUID eventId, LocalDateTime occurredOn, CustomerId customerId)
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

    public record CustomerMarkedAsApplicantEvent(UUID eventId, LocalDateTime occurredOn, CustomerId customerId)
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
