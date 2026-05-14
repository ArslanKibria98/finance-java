package com.ksa.financing.collections.adapter.rest.request;

import com.ksa.financing.collections.domain.model.DunningStage;

import java.math.BigDecimal;
import java.util.Map;

public record UpdateDunningPolicyRequest(
        String policyName,
        String productCode,
        String description,

        Integer preDueDaysBefore,
        Integer gracePeriodDays,
        Integer softCollectionDpd,
        Integer hardCollectionDpd,
        Integer legalDpd,
        Integer writeOffDpd,

        Boolean lateFeeEnabled,
        String lateFeeType,
        BigDecimal lateFeeAmount,
        BigDecimal lateFeePercentage,
        Integer lateFeeMinDpd,
        BigDecimal lateFeeMaxAmount,
        String charityFundAccount,

        Boolean simahReportEnabled,
        Integer simahReportDpd,
        Integer simahDefaultStatusDpd,

        boolean autoAssignAgent,
        Integer agentAssignmentDpd,
        Integer walletFreezeDpd,
        Boolean penaltyWaiverAllowed,
        Integer maxPenaltyWaiversAllowed,

        Map<DunningStage, Map<String, Object>> stageActions
) {}
