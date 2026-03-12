package com.ksa.financing.product.adapter.rest.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    UUID tenantId,
    String productCode,
    String nameEn,
    String nameAr,
    String descriptionEn,
    String descriptionAr,
    String shortDescriptionEn,
    String shortDescriptionAr,
    String logoUrl,
    String productType,
    String targetSegment,
    UUID masterCategoryId,
    UUID subCategoryId,
    UUID templateId,
    String notificationEmail,
    List<String> customerTypes,
    boolean involvesCommodity,
    String setupMethod,
    int wizardStep,
    boolean wizardCompleted,
    String shariaStructure,
    boolean commodityRequired,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    boolean visibleToCustomers,
    boolean visibleToPartners,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    int minTenureMonths,
    int maxTenureMonths,
    List<Integer> allowedTenures,
    BigDecimal baseProfitRate,
    String rateType,
    String repaymentFrequency,
    int gracePeriodDays,
    boolean earlySettlementAllowed,
    boolean waiveUnearnedProfit,
    Integer minTenureBeforeSettlement,
    String currency,
    UUID countryId,
    String fineractProductId,
    CountryResponse country,
    List<AdminFeeSlabResponse> adminFeeSlabs,
    TermsConditionsResponse termsConditions,
    FeeSettingsResponse feeSettings,
    List<ApplicationStepResponse> applicationSteps,
    DurationSettingsResponse durationSettings,
    List<EnvironmentConfigResponse> environmentConfigs,
    List<ApprovalWorkflowResponse> approvalWorkflows,
    List<DocumentResponse> documents,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy,
    int versionNumber
) {
    public record AdminFeeSlabResponse(
        UUID id,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal profitPercentage,
        BigDecimal processingFee,
        BigDecimal adminFee,
        String partnerScope,
        String status,
        int sortOrder,
        Integer minTenure,
        Integer maxTenure
    ) {}

    public record TermsConditionsResponse(
        UUID id,
        String termsEn,
        String termsAr
    ) {}

    public record FeeSettingsResponse(
        UUID id,
        BigDecimal minFinancingAmount,
        BigDecimal maxFinancingAmount,
        BigDecimal vatPercentage,
        BigDecimal revenueEligibilityThreshold,
        BigDecimal maxDbrPercentage,
        String dbrCalculationMethod,
        String dbrExceptions
    ) {}

    public record ApplicationStepResponse(
        UUID id,
        int stepNumber,
        String titleEn,
        String titleAr,
        String description,
        boolean required,
        int sortOrder
    ) {}

    public record DurationSettingsResponse(
        UUID id,
        int requestDurationDays,
        int approvalDurationDays,
        int disbursementDurationDays,
        int repaymentDurationDays
    ) {}

    public record EnvironmentConfigResponse(
        UUID id,
        UUID environmentConfigId,
        boolean active,
        int sortOrder
    ) {}

    public record ApprovalWorkflowResponse(
        UUID id,
        String workflowType,
        String nameEn,
        String nameAr,
        String description,
        String templateSource,
        boolean active,
        int priority,
        List<ApprovalConditionResponse> conditions,
        List<ApprovalActionResponse> actions
    ) {}

    public record ApprovalConditionResponse(
        UUID id,
        String field,
        String operator,
        String value,
        int sortOrder
    ) {}

    public record ApprovalActionResponse(
        UUID id,
        String actionType,
        String configuration,
        int sortOrder
    ) {}

    public record CountryResponse(
        UUID id,
        String code,
        String alpha3Code,
        String nameEn,
        String nameAr
    ) {}

    public record DocumentResponse(
        UUID id,
        String nameEn,
        String nameAr,
        String documentType,
        String fileUrl,
        Long fileSizeBytes,
        String fileVersion,
        String createdByName,
        String status,
        boolean required,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
