package com.ksa.financing.product.domain.port.out;

import com.ksa.financing.product.domain.model.DurationSettings;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductSettingsRepository {

    // Tab 1: Application Steps (replace-all)
    void saveApplicationSteps(UUID tenantId, UUID productId, List<ApplicationStepData> steps);

    // Tab 2: Terms & Conditions (upsert)
    void saveTermsConditions(UUID tenantId, UUID productId, String termsEn, String termsAr);

    // Tab 3: Fee Settings (upsert)
    void saveFeeSettings(UUID tenantId, UUID productId, BigDecimal revenueThreshold,
                         BigDecimal maxDbrPct,
                         String dbrMethod, String dbrExceptions,
                         BigDecimal maxDti, Integer minAge, Integer maxAge,
                         BigDecimal gdbrPercentage,
                         Boolean penaltyWaiverAllowed,
                         Integer maxPenaltyWaiversAllowed,
                         BigDecimal minFinancingAmount,
                         BigDecimal maxFinancingAmount,
                         BigDecimal vatPercentage);

    // Tab 4: Admin Fee Slabs (replace-all)
    void saveAdminFeeSlabs(UUID tenantId, UUID productId, List<AdminFeeSlabData> slabs);

    // Tab 5: Environment Configs (replace-all)
    void saveEnvironmentConfigs(UUID tenantId, UUID productId, List<EnvironmentConfigLinkData> configs);


    // Tab 6: Duration Settings (upsert — 0 means "no wait")
    void saveDurationSettings(UUID tenantId, UUID productId,
                              Integer requestDurationDays,
                              Integer approvalDurationDays,
                              Integer disbursementDurationHours,
                              Integer repaymentDurationDays);

    // Tab 6: Duration Settings (read)
    Optional<DurationSettings> findDurationSettings(UUID tenantId, UUID productId);

    // Tab 7: Approval Workflows (replace-all)
    void saveApprovalWorkflows(UUID tenantId, UUID productId, List<ApprovalWorkflowData> workflows);

    // --- Data Transfer Records ---

    record ApplicationStepData(int stepNumber, String titleEn, String titleAr,
                               String description, boolean required, int sortOrder) {}

    record AdminFeeSlabData(BigDecimal minAmount, BigDecimal maxAmount,
                            BigDecimal profitPercentage, BigDecimal processingFee,
                            BigDecimal adminFee, String partnerScope, String status,
                            int sortOrder, Integer minTenure, Integer maxTenure) {}

    record EnvironmentConfigLinkData(UUID environmentConfigId, boolean active, int sortOrder) {}

    record ApprovalWorkflowData(String workflowType, String nameEn, String nameAr,
                                String description, String templateSource, boolean active, int priority,
                                List<ApprovalConditionData> conditions, List<ApprovalActionData> actions) {}

    record ApprovalConditionData(String field, String operator, String value, int sortOrder) {}

    record ApprovalActionData(String actionType, String configuration, int sortOrder) {}

}
