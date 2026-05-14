package com.ksa.financing.collections.adapter.rest.request;

import com.ksa.financing.collections.domain.model.DunningStage;

import java.math.BigDecimal;
import java.util.Map;

public record CreateDunningPolicyRequest(
        String policyName,
        String productCode,
        String description,
        boolean defaultPolicy,

        // Thresholds
        Integer preDueDaysBefore,
        Integer gracePeriodDays,
        Integer softCollectionDpd,
        Integer hardCollectionDpd,
        Integer legalDpd,
        Integer writeOffDpd,

        // Late fee
        Boolean lateFeeEnabled,
        String lateFeeType,           // FLAT | PERCENTAGE
        BigDecimal lateFeeAmount,
        BigDecimal lateFeePercentage,
        Integer lateFeeMinDpd,
        BigDecimal lateFeeMaxAmount,
        String charityFundAccount,

        // Simah
        Boolean simahReportEnabled,
        Integer simahReportDpd,
        Integer simahDefaultStatusDpd,

        // Escalation
        boolean autoAssignAgent,
        Integer agentAssignmentDpd,
        Integer walletFreezeDpd,
        boolean penaltyWaiverAllowed,
        Integer maxPenaltyWaiversAllowed,

        // Stage actions (stage -> arbitrary action config)
        Map<DunningStage, Map<String, Object>> stageActions
) {}
