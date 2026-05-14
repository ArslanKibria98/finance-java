package com.ksa.financing.collections.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "dunning_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DunningPolicyJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "policy_name", nullable = false, length = 100)
    private String policyName;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_default", nullable = false)
    private boolean defaultPolicy;

    @Column(name = "pre_due_days_before", nullable = false)
    private int preDueDaysBefore;

    @Column(name = "grace_period_days", nullable = false)
    private int gracePeriodDays;

    @Column(name = "soft_collection_dpd", nullable = false)
    private int softCollectionDpd;

    @Column(name = "hard_collection_dpd", nullable = false)
    private int hardCollectionDpd;

    @Column(name = "legal_dpd", nullable = false)
    private int legalDpd;

    @Column(name = "write_off_dpd", nullable = false)
    private int writeOffDpd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pre_due_actions", columnDefinition = "jsonb")
    private Map<String, Object> preDueActions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "due_date_actions", columnDefinition = "jsonb")
    private Map<String, Object> dueDateActions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grace_period_actions", columnDefinition = "jsonb")
    private Map<String, Object> gracePeriodActions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "soft_collection_actions", columnDefinition = "jsonb")
    private Map<String, Object> softCollectionActions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hard_collection_actions", columnDefinition = "jsonb")
    private Map<String, Object> hardCollectionActions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "legal_actions", columnDefinition = "jsonb")
    private Map<String, Object> legalActions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "write_off_actions", columnDefinition = "jsonb")
    private Map<String, Object> writeOffActions;

    @Column(name = "late_fee_enabled", nullable = false)
    private boolean lateFeeEnabled;

    @Column(name = "late_fee_type", length = 20)
    private String lateFeeType;

    @Column(name = "late_fee_amount", precision = 19, scale = 4)
    private BigDecimal lateFeeAmount;

    @Column(name = "late_fee_percentage", precision = 5, scale = 2)
    private BigDecimal lateFeePercentage;

    @Column(name = "late_fee_min_dpd", nullable = false)
    private int lateFeeMinDpd;

    @Column(name = "late_fee_max_amount", precision = 19, scale = 4)
    private BigDecimal lateFeeMaxAmount;

    @Column(name = "charity_fund_account", length = 100)
    private String charityFundAccount;

    @Column(name = "simah_report_enabled", nullable = false)
    private boolean simahReportEnabled;

    @Column(name = "simah_report_dpd", nullable = false)
    private int simahReportDpd;

    @Column(name = "simah_default_status_dpd", nullable = false)
    private int simahDefaultStatusDpd;

    @Column(name = "auto_assign_agent", nullable = false)
    private boolean autoAssignAgent;

    @Column(name = "agent_assignment_dpd")
    private Integer agentAssignmentDpd;

    @Column(name = "wallet_freeze_dpd")
    private Integer walletFreezeDpd;

    @Column(name = "penalty_waiver_allowed", nullable = false)
    private boolean penaltyWaiverAllowed;

    @Column(name = "max_penalty_waivers_allowed", nullable = false)
    private int maxPenaltyWaiversAllowed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
