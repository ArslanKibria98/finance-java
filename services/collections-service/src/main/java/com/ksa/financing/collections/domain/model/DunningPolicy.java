package com.ksa.financing.collections.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DunningPolicy aggregate — admin-configurable delinquency rule set.
 * Zero framework imports. Owned by the Collections bounded context.
 *
 * Resolution order when engine needs a policy for a loan:
 *   1. Active policy where product_code = loan.productCode
 *   2. Active default policy (is_default = true, product_code = NULL)
 *   3. Hard-coded built-in defaults (DunningThresholds.defaults())
 */
public class DunningPolicy {

    private final DunningPolicyId id;
    private final UUID tenantId;
    private String policyName;
    private String productCode;             // NULL = tenant-wide default
    private String description;
    private boolean active;
    private boolean defaultPolicy;

    private DunningThresholds thresholds;
    private LateFeeConfig lateFee;
    private SimahReportingConfig simah;

    // Penalty Waiver Config
    private boolean penaltyWaiverAllowed;
    private int maxPenaltyWaiversAllowed;

    // Actions per stage — Map<stage, action-config-as-map>
    private final Map<DunningStage, Map<String, Object>> stageActions;

    // Escalation config
    private boolean autoAssignAgent;
    private Integer agentAssignmentDpd;
    private Integer walletFreezeDpd;

    private int version;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    private final List<Object> uncommittedEvents = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTORS
    // ═══════════════════════════════════════════════════════════════

    private DunningPolicy(DunningPolicyId id, UUID tenantId, String policyName,
                          String productCode, String description,
                          boolean active, boolean defaultPolicy,
                          DunningThresholds thresholds, LateFeeConfig lateFee,
                          SimahReportingConfig simah,
                          Map<DunningStage, Map<String, Object>> stageActions,
                          boolean autoAssignAgent, Integer agentAssignmentDpd,
                          Integer walletFreezeDpd,
                          boolean penaltyWaiverAllowed, int maxPenaltyWaiversAllowed,
                          LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.policyName = policyName;
        this.productCode = productCode;
        this.description = description;
        this.active = active;
        this.defaultPolicy = defaultPolicy;
        this.thresholds = thresholds;
        this.lateFee = lateFee;
        this.simah = simah;
        this.stageActions = stageActions != null ? new HashMap<>(stageActions) : new HashMap<>();
        this.autoAssignAgent = autoAssignAgent;
        this.agentAssignmentDpd = agentAssignmentDpd;
        this.walletFreezeDpd = walletFreezeDpd;
        this.penaltyWaiverAllowed = penaltyWaiverAllowed;
        this.maxPenaltyWaiversAllowed = maxPenaltyWaiversAllowed;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.version = 1;
    }

    // ═══════════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════

    public static DunningPolicy create(UUID tenantId, String policyName, String productCode,
                                       String description, boolean defaultPolicy,
                                       DunningThresholds thresholds, LateFeeConfig lateFee,
                                       SimahReportingConfig simah,
                                       Map<DunningStage, Map<String, Object>> stageActions,
                                       boolean autoAssignAgent, Integer agentAssignmentDpd,
                                       Integer walletFreezeDpd,
                                       boolean penaltyWaiverAllowed, int maxPenaltyWaiversAllowed,
                                       UUID createdBy) {
        if (tenantId == null)
            throw new IllegalArgumentException("tenantId cannot be null");
        if (policyName == null || policyName.isBlank())
            throw new IllegalArgumentException("policyName cannot be blank");
        if (thresholds == null)
            throw new IllegalArgumentException("thresholds cannot be null");

        var policy = new DunningPolicy(
                DunningPolicyId.generate(), tenantId, policyName.trim(),
                productCode, description,
                true, defaultPolicy,
                thresholds,
                lateFee != null ? lateFee : LateFeeConfig.disabled(),
                simah != null ? simah : SimahReportingConfig.defaults(),
                stageActions,
                autoAssignAgent, agentAssignmentDpd, walletFreezeDpd,
                penaltyWaiverAllowed, maxPenaltyWaiversAllowed,
                LocalDateTime.now());
        policy.createdBy = createdBy;
        policy.updatedBy = createdBy;

        policy.registerEvent(new DunningPolicyCreated(
                policy.id, tenantId, policyName, productCode, defaultPolicy));
        return policy;
    }

    public static DunningPolicy reconstitute(DunningPolicyId id, UUID tenantId, String policyName,
                                             String productCode, String description,
                                             boolean active, boolean defaultPolicy,
                                             DunningThresholds thresholds, LateFeeConfig lateFee,
                                             SimahReportingConfig simah,
                                             Map<DunningStage, Map<String, Object>> stageActions,
                                             boolean autoAssignAgent, Integer agentAssignmentDpd,
                                             Integer walletFreezeDpd,
                                             boolean penaltyWaiverAllowed, int maxPenaltyWaiversAllowed,
                                             int version,
                                             LocalDateTime createdAt, LocalDateTime updatedAt,
                                             UUID createdBy, UUID updatedBy) {
        var policy = new DunningPolicy(id, tenantId, policyName, productCode, description,
                active, defaultPolicy, thresholds, lateFee, simah,
                stageActions, autoAssignAgent, agentAssignmentDpd, walletFreezeDpd,
                penaltyWaiverAllowed, maxPenaltyWaiversAllowed,
                createdAt);
        policy.version = version;
        policy.updatedAt = updatedAt;
        policy.createdBy = createdBy;
        policy.updatedBy = updatedBy;
        return policy;
    }

    // ═══════════════════════════════════════════════════════════════
    // MUTATIONS
    // ═══════════════════════════════════════════════════════════════

    public void updateThresholds(DunningThresholds newThresholds, UUID updatedBy) {
        if (newThresholds == null)
            throw new IllegalArgumentException("thresholds cannot be null");
        this.thresholds = newThresholds;
        touch(updatedBy);
        registerEvent(new DunningPolicyThresholdsChanged(id, tenantId, newThresholds));
    }

    public void updateLateFee(LateFeeConfig newLateFee, UUID updatedBy) {
        this.lateFee = newLateFee != null ? newLateFee : LateFeeConfig.disabled();
        touch(updatedBy);
    }

    public void updateSimah(SimahReportingConfig newSimah, UUID updatedBy) {
        this.simah = newSimah != null ? newSimah : SimahReportingConfig.defaults();
        touch(updatedBy);
    }

    public void setStageActions(DunningStage stage, Map<String, Object> actions, UUID updatedBy) {
        if (stage == null)
            throw new IllegalArgumentException("stage cannot be null");
        if (actions == null || actions.isEmpty()) {
            this.stageActions.remove(stage);
        } else {
            this.stageActions.put(stage, new HashMap<>(actions));
        }
        touch(updatedBy);
    }

    public void rename(String newName, String newDescription, UUID updatedBy) {
        if (newName == null || newName.isBlank())
            throw new IllegalArgumentException("policyName cannot be blank");
        this.policyName = newName.trim();
        this.description = newDescription;
        touch(updatedBy);
    }

    public void bindToProduct(String newProductCode, UUID updatedBy) {
        this.productCode = newProductCode;
        touch(updatedBy);
    }

    public void updateEscalation(boolean autoAssign, Integer agentDpd, Integer walletFreezeDpd,
                                 UUID updatedBy) {
        this.autoAssignAgent = autoAssign;
        this.agentAssignmentDpd = agentDpd;
        this.walletFreezeDpd = walletFreezeDpd;
        touch(updatedBy);
    }
    
    public void updateWaiverSettings(boolean allowed, int maxWaivers, UUID updatedBy) {
        this.penaltyWaiverAllowed = allowed;
        this.maxPenaltyWaiversAllowed = maxWaivers;
        touch(updatedBy);
    }

    public void activate(UUID updatedBy) {
        if (!this.active) {
            this.active = true;
            touch(updatedBy);
            registerEvent(new DunningPolicyActivated(id, tenantId));
        }
    }

    public void deactivate(UUID updatedBy) {
        if (this.active) {
            this.active = false;
            touch(updatedBy);
            registerEvent(new DunningPolicyDeactivated(id, tenantId));
        }
    }

    public void markAsDefault(UUID updatedBy) {
        this.defaultPolicy = true;
        touch(updatedBy);
    }

    public void unmarkAsDefault(UUID updatedBy) {
        this.defaultPolicy = false;
        touch(updatedBy);
    }

    private void touch(UUID updatedBy) {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    // ═══════════════════════════════════════════════════════════════
    // DOMAIN BEHAVIOUR — used by dynamic delinquency engine
    // ═══════════════════════════════════════════════════════════════

    /** Stage this DPD falls into for this policy. */
    public DunningStage resolveStage(int dpd) {
        return thresholds.stageFor(dpd);
    }

    /** Should an overdue installment get a Sharia-compliant charity-fund late fee? */
    public boolean appliesLateFee(int dpd) {
        return lateFee.enabled() && dpd >= lateFee.minDpd();
    }

    /** Should this loan be reported to Simah credit bureau at this DPD? */
    public boolean shouldReportToSimah(int dpd) {
        return simah.shouldReport(dpd);
    }

    /** Should this loan be marked DEFAULT on Simah at this DPD? */
    public boolean shouldMarkSimahDefault(int dpd) {
        return simah.shouldMarkDefault(dpd);
    }

    /** Should a collections agent be auto-assigned at this DPD? */
    public boolean shouldAssignAgent(int dpd) {
        return autoAssignAgent
                && agentAssignmentDpd != null
                && dpd >= agentAssignmentDpd;
    }

    /** Should customer's wallet be frozen at this DPD? */
    public boolean shouldFreezeWallet(int dpd) {
        return walletFreezeDpd != null && dpd >= walletFreezeDpd;
    }

    public Map<String, Object> actionsForStage(DunningStage stage) {
        Map<String, Object> raw = stageActions.get(stage);
        return raw == null ? Map.of() : Collections.unmodifiableMap(raw);
    }

    // ═══════════════════════════════════════════════════════════════
    // EVENTS
    // ═══════════════════════════════════════════════════════════════

    private void registerEvent(Object event) {
        uncommittedEvents.add(event);
    }

    public List<Object> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════

    public DunningPolicyId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getPolicyName() { return policyName; }
    public String getProductCode() { return productCode; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public boolean isDefaultPolicy() { return defaultPolicy; }
    public DunningThresholds getThresholds() { return thresholds; }
    public LateFeeConfig getLateFee() { return lateFee; }
    public SimahReportingConfig getSimah() { return simah; }
    public Map<DunningStage, Map<String, Object>> getStageActions() {
        return Collections.unmodifiableMap(stageActions);
    }
    public boolean isAutoAssignAgent() { return autoAssignAgent; }
    public Integer getAgentAssignmentDpd() { return agentAssignmentDpd; }
    public Integer getWalletFreezeDpd() { return walletFreezeDpd; }
    public boolean isPenaltyWaiverAllowed() { return penaltyWaiverAllowed; }
    public int getMaxPenaltyWaiversAllowed() { return maxPenaltyWaiversAllowed; }
    public int getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }

    // ═══════════════════════════════════════════════════════════════
    // DOMAIN EVENTS
    // ═══════════════════════════════════════════════════════════════

    public record DunningPolicyCreated(
            DunningPolicyId policyId, UUID tenantId, String policyName,
            String productCode, boolean isDefault) {}

    public record DunningPolicyThresholdsChanged(
            DunningPolicyId policyId, UUID tenantId, DunningThresholds newThresholds) {}

    public record DunningPolicyActivated(DunningPolicyId policyId, UUID tenantId) {}

    public record DunningPolicyDeactivated(DunningPolicyId policyId, UUID tenantId) {}
}
