package com.ksa.financing.domain.model;

import com.ksa.financing.domain.enums.ShariaStructure;
import com.ksa.financing.domain.event.DomainEvent;
import com.ksa.financing.domain.valueobject.ProductId;
import com.ksa.financing.domain.valueobject.ProfitRate;
import com.ksa.financing.domain.valueobject.SarMoney;
import com.ksa.financing.domain.valueobject.Tenure;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Product aggregate root representing an Islamic financing product.
 * <p>
 * This aggregate manages:
 * - Product identity and configuration
 * - Sharia structure definition
 * - Eligibility criteria (amount and tenure ranges)
 * - Profit rate configuration
 * - Product lifecycle status
 * </p>
 * <p>
 * Business invariants enforced:
 * - Min amount must be less than or equal to max amount
 * - Min tenure must be less than or equal to max tenure
 * - Profit rate must be positive
 * - Product code must be unique (enforced at repository level)
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public class Product {

    private final ProductId productId;
    private final String productCode;
    private final String name;
    private final ShariaStructure shariaStructure;
    private final SarMoney minAmount;
    private final SarMoney maxAmount;
    private final Tenure minTenure;
    private final Tenure maxTenure;
    private final ProfitRate baseProfitRate;

    private String status;

    private final List<DomainEvent> domainEvents;

    /**
     * Private constructor for creating product instances.
     * Use factory methods instead.
     */
    private Product(ProductId productId, String productCode, String name,
                    ShariaStructure shariaStructure, SarMoney minAmount, SarMoney maxAmount,
                    Tenure minTenure, Tenure maxTenure, ProfitRate baseProfitRate, String status) {
        this.productId = Objects.requireNonNull(productId, "ProductId cannot be null");
        this.productCode = Objects.requireNonNull(productCode, "ProductCode cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.shariaStructure = Objects.requireNonNull(shariaStructure, "ShariaStructure cannot be null");
        this.minAmount = Objects.requireNonNull(minAmount, "MinAmount cannot be null");
        this.maxAmount = Objects.requireNonNull(maxAmount, "MaxAmount cannot be null");
        this.minTenure = Objects.requireNonNull(minTenure, "MinTenure cannot be null");
        this.maxTenure = Objects.requireNonNull(maxTenure, "MaxTenure cannot be null");
        this.baseProfitRate = Objects.requireNonNull(baseProfitRate, "BaseProfitRate cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.domainEvents = new ArrayList<>();

        validateInvariants();
    }

    /**
     * Factory method to create a new product.
     *
     * @param productCode the unique product code
     * @param name the product name
     * @param shariaStructure the Sharia-compliant structure
     * @param minAmount the minimum financing amount
     * @param maxAmount the maximum financing amount
     * @param minTenure the minimum tenure
     * @param maxTenure the maximum tenure
     * @param baseProfitRate the base profit rate
     * @return a new Product instance
     */
    public static Product createProduct(String productCode, String name, ShariaStructure shariaStructure,
                                        SarMoney minAmount, SarMoney maxAmount,
                                        Tenure minTenure, Tenure maxTenure,
                                        ProfitRate baseProfitRate) {
        Objects.requireNonNull(productCode, "ProductCode cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");

        if (productCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Product code cannot be empty");
        }

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }

        Product product = new Product(
                ProductId.generate(),
                productCode.trim().toUpperCase(),
                name.trim(),
                shariaStructure,
                minAmount,
                maxAmount,
                minTenure,
                maxTenure,
                baseProfitRate,
                "INACTIVE" // Products start as inactive and must be explicitly activated
        );

        product.addDomainEvent(new ProductCreatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                product.productId,
                product.productCode,
                product.name,
                product.shariaStructure
        ));

        return product;
    }

    /**
     * Activate the product.
     * Only active products can be used for loan origination.
     */
    public void activate() {
        if (status.equals("ACTIVE")) {
            // Already active, no-op
            return;
        }

        this.status = "ACTIVE";

        addDomainEvent(new ProductActivatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                productId
        ));
    }

    /**
     * Deactivate the product.
     * Deactivated products cannot be used for new loans.
     */
    public void deactivate() {
        if (status.equals("INACTIVE")) {
            // Already inactive, no-op
            return;
        }

        this.status = "INACTIVE";

        addDomainEvent(new ProductDeactivatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                productId
        ));
    }

    /**
     * Check if a given amount is eligible for this product.
     *
     * @param amount the amount to check
     * @return true if the amount is within the product's range, false otherwise
     */
    public boolean isEligibleAmount(SarMoney amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        return amount.isGreaterThanOrEqualTo(minAmount) && amount.isLessThan(maxAmount);
    }

    /**
     * Check if a given tenure is eligible for this product.
     *
     * @param tenure the tenure to check
     * @return true if the tenure is within the product's range, false otherwise
     */
    public boolean isEligibleTenure(Tenure tenure) {
        Objects.requireNonNull(tenure, "Tenure cannot be null");
        return !tenure.isShorterThan(minTenure) && !tenure.isLongerThan(maxTenure);
    }

    /**
     * Check if the product is active.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return status.equals("ACTIVE");
    }

    /**
     * Calculate the profit amount for a given principal and tenure.
     * Uses the base profit rate.
     *
     * @param principal the principal amount
     * @return the calculated profit amount
     */
    public SarMoney calculateProfit(SarMoney principal) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        return baseProfitRate.multiply(principal);
    }

    /**
     * Validate business invariants.
     */
    private void validateInvariants() {
        if (productCode == null || productCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Product code cannot be empty");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }

        if (!minAmount.isPositive()) {
            throw new IllegalArgumentException("Minimum amount must be positive");
        }

        if (!maxAmount.isPositive()) {
            throw new IllegalArgumentException("Maximum amount must be positive");
        }

        if (minAmount.isGreaterThan(maxAmount)) {
            throw new IllegalArgumentException(
                    String.format("Minimum amount (%s) cannot be greater than maximum amount (%s)",
                            minAmount.toFormattedString(), maxAmount.toFormattedString())
            );
        }

        if (minTenure.isLongerThan(maxTenure)) {
            throw new IllegalArgumentException(
                    String.format("Minimum tenure (%s) cannot be longer than maximum tenure (%s)",
                            minTenure.toFormattedString(), maxTenure.toFormattedString())
            );
        }

        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be empty");
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

    public ProductId getProductId() {
        return productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getName() {
        return name;
    }

    public ShariaStructure getShariaStructure() {
        return shariaStructure;
    }

    public SarMoney getMinAmount() {
        return minAmount;
    }

    public SarMoney getMaxAmount() {
        return maxAmount;
    }

    public Tenure getMinTenure() {
        return minTenure;
    }

    public Tenure getMaxTenure() {
        return maxTenure;
    }

    public ProfitRate getBaseProfitRate() {
        return baseProfitRate;
    }

    public String getStatus() {
        return status;
    }

    // Domain Event Classes (inner classes for simplicity)

    public record ProductCreatedEvent(UUID eventId, LocalDateTime occurredOn, ProductId productId,
                                       String productCode, String name, ShariaStructure shariaStructure)
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

    public record ProductActivatedEvent(UUID eventId, LocalDateTime occurredOn, ProductId productId)
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

    public record ProductDeactivatedEvent(UUID eventId, LocalDateTime occurredOn, ProductId productId)
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
