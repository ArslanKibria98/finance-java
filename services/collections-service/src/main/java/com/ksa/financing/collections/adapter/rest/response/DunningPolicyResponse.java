package com.ksa.financing.collections.adapter.rest.response;

import com.ksa.financing.collections.domain.model.DunningStage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record DunningPolicyResponse(
        UUID id,
        UUID tenantId,
        String policyName,
        String productCode,
        String description,
        boolean active,
        boolean defaultPolicy,

        int preDueDaysBefore,
        int gracePeriodDays,
        int softCollectionDpd,
        int hardCollectionDpd,
        int legalDpd,
        int writeOffDpd,

        boolean lateFeeEnabled,
        String lateFeeType,
        BigDecimal lateFeeAmount,
        BigDecimal lateFeePercentage,
        int lateFeeMinDpd,
        BigDecimal lateFeeMaxAmount,
        String charityFundAccount,

        boolean simahReportEnabled,
        int simahReportDpd,
        int simahDefaultStatusDpd,

        boolean autoAssignAgent,
        Integer agentAssignmentDpd,
        Integer walletFreezeDpd,
        boolean penaltyWaiverAllowed,
        int maxPenaltyWaiversAllowed,

        Map<DunningStage, Map<String, Object>> stageActions,

        int version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UUID createdBy,
        UUID updatedBy
) {}
