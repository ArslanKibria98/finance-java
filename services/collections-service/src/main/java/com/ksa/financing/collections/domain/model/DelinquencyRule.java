package com.ksa.financing.collections.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Per-product delinquency rule — one aggregate per {tenant, product, delinquencyType} tuple.
 * Mirrors the LMS admin UI where each tab (Early Settlement, Due Loan, Late Payment,
 * Write-offs, Non-Performing Loan, Broken Promises) configures exactly one rule.
 *
 * <p>For {@code DelinquencyType.EARLY_SETTLEMENT} the rule may also own a collection of
 * {@link EarlySettlementConfig} rows (singles + ranges) when {@code isCustom=true}.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DelinquencyRule {

    private DelinquencyRuleId id;
    private UUID tenantId;
    private UUID productId;
    private DelinquencyType delinquencyType;

    private boolean isPercentage;
    private BigDecimal penaltyPercentage = BigDecimal.ZERO;
    private BigDecimal penaltyAmount     = BigDecimal.ZERO;

    private int fromDay;
    private int tillDay;

    /** 0 = N/A (Broken Promises), 1 = applicable. Matches existing LMS wire values. */
    private int penaltyType = 1;

    private int promisesPerYear;   // type=6 only
    private int promisesPerLoan;   // type=6 only

    /** type=1 only: true ⇒ use earlySettlementConfigs rather than the flat amount/day fields. */
    private boolean isCustom;

    private String charityFundAccount;

    private String channel = "LMS";
    private int recordState = 1;   // 1=Active, 0=Deleted
    private int version = 1;

    private LocalDateTime created;
    private LocalDateTime updatedAt;

    private final List<EarlySettlementConfig> earlySettlementConfigs = new ArrayList<>();
    private final List<Object> uncommittedEvents = new ArrayList<>();

    // ───── Factory ─────
    public static DelinquencyRule create(UUID tenantId, UUID productId, DelinquencyType type) {
        if (tenantId == null)  throw new IllegalArgumentException("tenantId required");
        if (productId == null) throw new IllegalArgumentException("productId required");
        if (type == null)      throw new IllegalArgumentException("delinquencyType required");

        var rule = new DelinquencyRule();
        rule.id = DelinquencyRuleId.generate();
        rule.tenantId = tenantId;
        rule.productId = productId;
        rule.delinquencyType = type;
        rule.created = LocalDateTime.now();
        rule.updatedAt = rule.created;
        rule.penaltyType = (type == DelinquencyType.BROKEN_PROMISES) ? 0 : 1;
        rule.registerEvent(new DelinquencyRuleCreated(rule.id, tenantId, productId, type));
        return rule;
    }

    /** Hydration constructor used by the persistence mapper. */
    public static DelinquencyRule hydrate(DelinquencyRuleId id, UUID tenantId, UUID productId,
                                          DelinquencyType type, boolean isPercentage,
                                          BigDecimal penaltyPercentage, BigDecimal penaltyAmount,
                                          int fromDay, int tillDay, int penaltyType,
                                          int promisesPerYear, int promisesPerLoan,
                                          boolean isCustom, String charityFundAccount,
                                          String channel, int recordState, int version,
                                          LocalDateTime created, LocalDateTime updatedAt) {
        var rule = new DelinquencyRule();
        rule.id = id;
        rule.tenantId = tenantId;
        rule.productId = productId;
        rule.delinquencyType = type;
        rule.isPercentage = isPercentage;
        rule.penaltyPercentage = penaltyPercentage != null ? penaltyPercentage : BigDecimal.ZERO;
        rule.penaltyAmount     = penaltyAmount     != null ? penaltyAmount     : BigDecimal.ZERO;
        rule.fromDay = fromDay;
        rule.tillDay = tillDay;
        rule.penaltyType = penaltyType;
        rule.promisesPerYear = promisesPerYear;
        rule.promisesPerLoan = promisesPerLoan;
        rule.isCustom = isCustom;
        rule.charityFundAccount = charityFundAccount;
        rule.channel = channel != null ? channel : "LMS";
        rule.recordState = recordState;
        rule.version = version;
        rule.created = created;
        rule.updatedAt = updatedAt;
        return rule;
    }

    // ───── Mutators (business methods) ─────
    public void updatePenalty(boolean isPercentage, BigDecimal percentage, BigDecimal amount) {
        this.isPercentage = isPercentage;
        this.penaltyPercentage = percentage != null ? percentage : BigDecimal.ZERO;
        this.penaltyAmount     = amount     != null ? amount     : BigDecimal.ZERO;
        touch();
    }

    public void updateDayRange(int fromDay, int tillDay) {
        if (tillDay < fromDay && delinquencyType != DelinquencyType.EARLY_SETTLEMENT) {
            throw new IllegalArgumentException("tillDay must be >= fromDay for type " + delinquencyType);
        }
        this.fromDay = fromDay;
        this.tillDay = tillDay;
        touch();
    }

    public void updateBrokenPromises(int perYear, int perLoan) {
        if (delinquencyType != DelinquencyType.BROKEN_PROMISES) {
            throw new IllegalStateException("promises fields apply only to BROKEN_PROMISES");
        }
        this.promisesPerYear = perYear;
        this.promisesPerLoan = perLoan;
        touch();
    }

    public void toggleCustom(boolean custom) {
        if (delinquencyType != DelinquencyType.EARLY_SETTLEMENT) {
            throw new IllegalStateException("isCustom applies only to EARLY_SETTLEMENT");
        }
        this.isCustom = custom;
        touch();
    }

    public void setCharityFundAccount(String account) {
        this.charityFundAccount = account;
        touch();
    }

    public void softDelete() {
        this.recordState = 0;
        touch();
        registerEvent(new DelinquencyRuleDeactivated(id, tenantId, productId, delinquencyType));
    }

    // ───── Early Settlement child configs ─────
    public EarlySettlementConfig addSingleConfig(int invoiceOrder, int fromDay, int tillDay,
                                                 boolean isPercentage, BigDecimal percentage,
                                                 BigDecimal amount) {
        ensureEarlySettlement();
        var cfg = EarlySettlementConfig.builder()
                .id(UUID.randomUUID())
                .delinquencyId(id.getValue())
                .invoiceOrder(invoiceOrder)
                .fromDay(fromDay)
                .tillDay(tillDay)
                .isPercentage(isPercentage)
                .discountPercentage(percentage != null ? percentage : BigDecimal.ZERO)
                .discountAmount(amount != null ? amount : BigDecimal.ZERO)
                .isRange(false)
                .rangeNo(0)
                .created(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        earlySettlementConfigs.add(cfg);
        touch();
        return cfg;
    }

    public EarlySettlementConfig addRangeConfigs(int rangeNo, int minInvoice, int maxInvoice,
                                                 int fromDay, int tillDay,
                                                 boolean isPercentage, BigDecimal percentage,
                                                 BigDecimal amount) {
        ensureEarlySettlement();
        if (maxInvoice < minInvoice) {
            throw new IllegalArgumentException("maxInvoiceOrder must be >= minInvoiceOrder");
        }
        var cfg = EarlySettlementConfig.builder()
                .id(UUID.randomUUID())
                .delinquencyId(id.getValue())
                .invoiceOrder(null)
                .fromDay(fromDay)
                .tillDay(tillDay)
                .isPercentage(isPercentage)
                .discountPercentage(percentage != null ? percentage : BigDecimal.ZERO)
                .discountAmount(amount != null ? amount : BigDecimal.ZERO)
                .isRange(true)
                .rangeNo(rangeNo)
                .minInvoiceOrder(minInvoice)
                .maxInvoiceOrder(maxInvoice)
                .created(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        earlySettlementConfigs.add(cfg);
        touch();
        return cfg;
    }

    public void attachHydratedConfigs(List<EarlySettlementConfig> configs) {
        if (configs != null) earlySettlementConfigs.addAll(configs);
    }

    public void clearEarlySettlementConfigs() {
        if (!earlySettlementConfigs.isEmpty()) {
            earlySettlementConfigs.clear();
            touch();
        }
    }

    public List<EarlySettlementConfig> listEarlySettlementConfigs() {
        return Collections.unmodifiableList(earlySettlementConfigs);
    }

    public boolean removeEarlySettlementConfig(UUID configId) {
        var removed = earlySettlementConfigs.removeIf(c -> c.getId().equals(configId));
        if (removed) touch();
        return removed;
    }

    // ───── Helpers ─────
    private void ensureEarlySettlement() {
        if (delinquencyType != DelinquencyType.EARLY_SETTLEMENT) {
            throw new IllegalStateException("EarlySettlement configs apply only to EARLY_SETTLEMENT");
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    // ───── Domain Events ─────
    public record DelinquencyRuleCreated(DelinquencyRuleId ruleId, UUID tenantId, UUID productId,
                                         DelinquencyType type) {}
    public record DelinquencyRuleUpdated(DelinquencyRuleId ruleId, UUID tenantId, UUID productId,
                                         DelinquencyType type) {}
    public record DelinquencyRuleDeactivated(DelinquencyRuleId ruleId, UUID tenantId, UUID productId,
                                             DelinquencyType type) {}

    private void registerEvent(Object event) {
        uncommittedEvents.add(event);
    }

    public List<Object> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
}
