package com.ksa.financing.product.domain.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageProductSettingsUseCase {

    // Tab 1: Application Steps
    void updateApplicationSteps(UUID tenantId, UUID productId, List<ApplicationStepCommand> steps);

    // Tab 2: Terms & Conditions
    void updateTermsConditions(UUID tenantId, UUID productId, UpdateTermsConditionsCommand command);

    // Tab 3: Fee Settings
    void updateFeeSettings(UUID tenantId, UUID productId, UpdateFeeSettingsCommand command);

    // Tab 4: Admin Fee Slabs
    void updateAdminFeeSlabs(UUID tenantId, UUID productId, List<AdminFeeSlabCommand> slabs);

    // Tab 5: Environment Configs
    void updateEnvironmentConfigs(UUID tenantId, UUID productId, List<EnvironmentConfigLinkCommand> configs);

    // Tab 6: Duration Settings
    void updateDurationSettings(UUID tenantId, UUID productId, UpdateDurationSettingsCommand command);

    // Tab 7: Approval Workflows
    void updateApprovalWorkflows(UUID tenantId, UUID productId, List<ApprovalWorkflowCommand> workflows);

    // --- Command Records ---

    record ApplicationStepCommand(
        int stepNumber, String titleEn, String titleAr,
        String description, boolean required, int sortOrder
    ) {}

    record UpdateTermsConditionsCommand(String termsEn, String termsAr) {}

    record UpdateFeeSettingsCommand(
        BigDecimal minFinancingAmount, BigDecimal maxFinancingAmount,
        BigDecimal vatPercentage, BigDecimal revenueEligibilityThreshold,
        BigDecimal maxDbrPercentage, String dbrCalculationMethod, String dbrExceptions
    ) {}

    record AdminFeeSlabCommand(
        BigDecimal minAmount, BigDecimal maxAmount,
        BigDecimal profitPercentage, BigDecimal processingFee,
        BigDecimal adminFee, String partnerScope, String status,
        int sortOrder, Integer minTenure, Integer maxTenure
    ) {}

    record EnvironmentConfigLinkCommand(UUID environmentConfigId, boolean active, int sortOrder) {}

    record UpdateDurationSettingsCommand(
        int requestDurationDays, int approvalDurationDays,
        int disbursementDurationDays, int repaymentDurationDays
    ) {}

    record ApprovalWorkflowCommand(
        String workflowType, String nameEn, String nameAr, String description,
        String templateSource, boolean active, int priority,
        List<ApprovalConditionCommand> conditions, List<ApprovalActionCommand> actions
    ) {}

    record ApprovalConditionCommand(String field, String operator, String value, int sortOrder) {}

    record ApprovalActionCommand(String actionType, String configuration, int sortOrder) {}

}
