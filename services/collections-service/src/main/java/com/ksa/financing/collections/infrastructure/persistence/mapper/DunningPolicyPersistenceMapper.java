package com.ksa.financing.collections.infrastructure.persistence.mapper;

import com.ksa.financing.collections.domain.model.DunningPolicy;
import com.ksa.financing.collections.domain.model.DunningPolicyId;
import com.ksa.financing.collections.domain.model.DunningStage;
import com.ksa.financing.collections.domain.model.DunningThresholds;
import com.ksa.financing.collections.domain.model.LateFeeConfig;
import com.ksa.financing.collections.domain.model.LateFeeType;
import com.ksa.financing.collections.domain.model.SimahReportingConfig;
import com.ksa.financing.collections.infrastructure.persistence.entity.DunningPolicyJpaEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Maps between {@link DunningPolicy} domain aggregate and its JPA entity. */
@Component
public class DunningPolicyPersistenceMapper {

    public DunningPolicyJpaEntity toEntity(DunningPolicy p) {
        var e = new DunningPolicyJpaEntity();
        e.setId(p.getId().getValue());
        e.setTenantId(p.getTenantId());
        e.setPolicyName(p.getPolicyName());
        e.setProductCode(p.getProductCode());
        e.setDescription(p.getDescription());
        e.setActive(p.isActive());
        e.setDefaultPolicy(p.isDefaultPolicy());

        var t = p.getThresholds();
        e.setPreDueDaysBefore(t.preDueDaysBefore());
        e.setGracePeriodDays(t.gracePeriodDays());
        e.setSoftCollectionDpd(t.softCollectionDpd());
        e.setHardCollectionDpd(t.hardCollectionDpd());
        e.setLegalDpd(t.legalDpd());
        e.setWriteOffDpd(t.writeOffDpd());

        var actions = p.getStageActions();
        e.setPreDueActions(actions.get(DunningStage.PRE_DUE_REMINDER));
        e.setDueDateActions(actions.get(DunningStage.DUE_DATE_REMINDER));
        e.setGracePeriodActions(actions.get(DunningStage.GRACE_PERIOD));
        e.setSoftCollectionActions(actions.get(DunningStage.SOFT_COLLECTION));
        e.setHardCollectionActions(actions.get(DunningStage.HARD_COLLECTION));
        e.setLegalActions(actions.get(DunningStage.LEGAL));
        e.setWriteOffActions(actions.get(DunningStage.WRITE_OFF));

        var lf = p.getLateFee();
        e.setLateFeeEnabled(lf.enabled());
        e.setLateFeeType(lf.type() != null ? lf.type().name() : null);
        e.setLateFeeAmount(lf.flatAmount());
        e.setLateFeePercentage(lf.percentage());
        e.setLateFeeMinDpd(lf.minDpd());
        e.setLateFeeMaxAmount(lf.maxAmount());
        e.setCharityFundAccount(lf.charityFundAccount());

        var s = p.getSimah();
        e.setSimahReportEnabled(s.enabled());
        e.setSimahReportDpd(s.reportDpd());
        e.setSimahDefaultStatusDpd(s.defaultStatusDpd());

        e.setAutoAssignAgent(p.isAutoAssignAgent());
        e.setAgentAssignmentDpd(p.getAgentAssignmentDpd());
        e.setWalletFreezeDpd(p.getWalletFreezeDpd());
        e.setPenaltyWaiverAllowed(p.isPenaltyWaiverAllowed());
        e.setMaxPenaltyWaiversAllowed(p.getMaxPenaltyWaiversAllowed());

        e.setCreatedAt(p.getCreatedAt() != null ? p.getCreatedAt() : LocalDateTime.now());
        e.setUpdatedAt(p.getUpdatedAt());
        e.setCreatedBy(p.getCreatedBy());
        e.setUpdatedBy(p.getUpdatedBy());
        e.setVersion(p.getVersion());
        return e;
    }

    public DunningPolicy toDomain(DunningPolicyJpaEntity e) {
        var thresholds = new DunningThresholds(
                e.getPreDueDaysBefore(),
                e.getGracePeriodDays(),
                e.getSoftCollectionDpd(),
                e.getHardCollectionDpd(),
                e.getLegalDpd(),
                e.getWriteOffDpd());

        var lateFee = e.isLateFeeEnabled()
                ? new LateFeeConfig(
                        true,
                        e.getLateFeeType() != null ? LateFeeType.valueOf(e.getLateFeeType()) : null,
                        e.getLateFeeAmount(),
                        e.getLateFeePercentage(),
                        e.getLateFeeMinDpd(),
                        e.getLateFeeMaxAmount(),
                        e.getCharityFundAccount())
                : LateFeeConfig.disabled();

        var simah = new SimahReportingConfig(
                e.isSimahReportEnabled(),
                e.getSimahReportDpd(),
                e.getSimahDefaultStatusDpd());

        Map<DunningStage, Map<String, Object>> actions = new EnumMap<>(DunningStage.class);
        putIfPresent(actions, DunningStage.PRE_DUE_REMINDER,   e.getPreDueActions());
        putIfPresent(actions, DunningStage.DUE_DATE_REMINDER,  e.getDueDateActions());
        putIfPresent(actions, DunningStage.GRACE_PERIOD,       e.getGracePeriodActions());
        putIfPresent(actions, DunningStage.SOFT_COLLECTION,    e.getSoftCollectionActions());
        putIfPresent(actions, DunningStage.HARD_COLLECTION,    e.getHardCollectionActions());
        putIfPresent(actions, DunningStage.LEGAL,              e.getLegalActions());
        putIfPresent(actions, DunningStage.WRITE_OFF,          e.getWriteOffActions());

        return DunningPolicy.reconstitute(
                DunningPolicyId.of(e.getId()),
                e.getTenantId(),
                e.getPolicyName(),
                e.getProductCode(),
                e.getDescription(),
                e.isActive(),
                e.isDefaultPolicy(),
                thresholds, lateFee, simah,
                actions,
                e.isAutoAssignAgent(),
                e.getAgentAssignmentDpd(),
                e.getWalletFreezeDpd(),
                e.isPenaltyWaiverAllowed(),
                e.getMaxPenaltyWaiversAllowed(),
                e.getVersion(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getCreatedBy(),
                e.getUpdatedBy());
    }

    private static void putIfPresent(Map<DunningStage, Map<String, Object>> target,
                                     DunningStage stage, Map<String, Object> value) {
        if (value != null && !value.isEmpty()) {
            target.put(stage, new HashMap<>(value));
        }
    }
}
