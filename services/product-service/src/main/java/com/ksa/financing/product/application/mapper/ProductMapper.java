package com.ksa.financing.product.application.mapper;

// TODO [ARCH-VIOLATION] Application layer imports adapter layer DTOs.
// Fix: Move ProductResponse/ProductSummaryResponse to application.dto package,
// or create application-layer DTOs and map to adapter responses in the controller.
import com.ksa.financing.product.adapter.rest.response.ProductResponse;
import com.ksa.financing.product.adapter.rest.response.ProductResponse.*;
import com.ksa.financing.product.adapter.rest.response.ProductSummaryResponse;
import com.ksa.financing.product.domain.model.Product;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ProductMapper {

    private ProductMapper() {}

    public static ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        return new ProductResponse(
                product.getId(),
                product.getTenantId(),
                product.getProductCode(),
                product.getNameEn(),
                product.getNameAr(),
                product.getDescriptionEn(),
                product.getDescriptionAr(),
                product.getShortDescriptionEn(),
                product.getShortDescriptionAr(),
                product.getLogoUrl(),
                product.getProductType() != null ? product.getProductType().name() : null,
                product.getTargetSegment(),
                product.getMasterCategoryId(),
                product.getSubCategoryId(),
                product.getTemplateId(),
                product.getNotificationEmail(),
                product.getCustomerTypes(),
                product.isInvolvesCommodity(),
                product.getSetupMethod(),
                product.getWizardStep(),
                product.isWizardCompleted(),
                product.getShariaStructure(),
                product.isCommodityRequired(),
                product.getStatus() != null ? product.getStatus().name() : null,
                product.getStartDate(),
                product.getEndDate(),
                product.isVisibleToCustomers(),
                product.isVisibleToPartners(),
                product.getMinAmount(),
                product.getMaxAmount(),
                product.getMinTenureMonths(),
                product.getMaxTenureMonths(),
                product.getAllowedTenures(),
                product.getBaseProfitRate(),
                product.getRateType(),
                product.getRepaymentFrequency(),
                product.getGracePeriodDays(),
                product.isEarlySettlementAllowed(),
                product.isWaiveUnearnedProfit(),
                product.getMinTenureBeforeSettlement(),
                product.getCurrency(),
                product.getCountryId(),
                product.getFineractProductId(),
                product.getCountry() != null
                    ? new CountryResponse(
                            product.getCountry().getId(),
                            product.getCountry().getCode(),
                            product.getCountry().getAlpha3Code(),
                            product.getCountry().getNameEn(),
                            product.getCountry().getNameAr())
                    : null,
                product.getAdminFeeSlabs() != null
                    ? product.getAdminFeeSlabs().stream()
                        .map(s -> new AdminFeeSlabResponse(
                                s.id(), s.minAmount(), s.maxAmount(),
                                s.profitPercentage(), s.processingFee(),
                                s.adminFee(), s.partnerScope(), s.status(),
                                s.sortOrder(), s.minTenure(), s.maxTenure()))
                        .toList()
                    : Collections.emptyList(),
                product.getTermsConditions() != null
                    ? new TermsConditionsResponse(
                            product.getTermsConditions().id(),
                            product.getTermsConditions().termsEn(),
                            product.getTermsConditions().termsAr())
                    : null,
                product.getFeeSettings() != null
                    ? new FeeSettingsResponse(
                            product.getFeeSettings().id(),
                            product.getFeeSettings().minFinancingAmount(),
                            product.getFeeSettings().maxFinancingAmount(),
                            product.getFeeSettings().vatPercentage(),
                            product.getFeeSettings().revenueEligibilityThreshold(),
                            product.getFeeSettings().maxDbrPercentage(),
                            product.getFeeSettings().globalDbrPercentage(),
                            product.getFeeSettings().dbrCalculationMethod(),
                            product.getFeeSettings().dbrExceptions())
                    : null,
                product.getApplicationSteps() != null
                    ? product.getApplicationSteps().stream()
                        .map(s -> new ApplicationStepResponse(
                                s.id(), s.stepNumber(), s.titleEn(), s.titleAr(),
                                s.description(), s.required(), s.sortOrder()))
                        .toList()
                    : Collections.emptyList(),
                product.getDurationSettings() != null
                    ? new DurationSettingsResponse(
                            product.getDurationSettings().id(),
                            product.getDurationSettings().requestDurationDays(),
                            product.getDurationSettings().approvalDurationDays(),
                            product.getDurationSettings().disbursementDurationDays(),
                            product.getDurationSettings().repaymentDurationDays())
                    : null,
                product.getEnvironmentConfigs() != null
                    ? product.getEnvironmentConfigs().stream()
                        .map(ec -> new EnvironmentConfigResponse(
                                ec.id(), ec.environmentConfigId(), ec.active(), ec.sortOrder()))
                        .toList()
                    : Collections.emptyList(),
                product.getApprovalWorkflows() != null
                    ? product.getApprovalWorkflows().stream()
                        .map(wf -> new ApprovalWorkflowResponse(
                                wf.id(), wf.workflowType(), wf.nameEn(), wf.nameAr(),
                                wf.description(), wf.templateSource(), wf.active(), wf.priority(),
                                wf.conditions() != null
                                    ? wf.conditions().stream()
                                        .map(c -> new ApprovalConditionResponse(
                                                c.id(), c.field(), c.operator(), c.value(), c.sortOrder()))
                                        .toList()
                                    : Collections.emptyList(),
                                wf.actions() != null
                                    ? wf.actions().stream()
                                        .map(a -> new ApprovalActionResponse(
                                                a.id(), a.actionType(), a.configuration(), a.sortOrder()))
                                        .toList()
                                    : Collections.emptyList()))
                        .toList()
                    : Collections.emptyList(),
                product.getDocuments() != null
                    ? product.getDocuments().stream()
                        .map(d -> new ProductResponse.DocumentResponse(
                                d.id(), d.nameEn(), d.nameAr(), d.documentType(),
                                d.fileUrl(), d.fileSizeBytes(), d.fileVersion(),
                                d.createdByName(), d.status(), d.required(),
                                d.sortOrder(), d.createdAt(), d.updatedAt()))
                        .toList()
                    : Collections.emptyList(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getCreatedBy(),
                product.getUpdatedBy(),
                product.getVersionNumber()
        );
    }

    public static ProductSummaryResponse toSummaryResponse(Product product) {
        if (product == null) {
            return null;
        }

        return new ProductSummaryResponse(
                product.getId(),
                product.getProductCode(),
                product.getNameEn(),
                product.getNameAr(),
                product.getShortDescriptionEn(),
                product.getShortDescriptionAr(),
                product.getProductType() != null ? product.getProductType().name() : null,
                product.getTargetSegment(),
                product.getShariaStructure(),
                product.getStatus() != null ? product.getStatus().name() : null,
                product.getMasterCategoryId(),
                product.getMasterCategoryNameEn(),
                product.getMasterCategoryNameAr(),
                product.getSubCategoryId(),
                product.getSubCategoryNameEn(),
                product.getSubCategoryNameAr(),
                product.getNotificationEmail(),
                product.getCountry() != null ? product.getCountry().getNameEn() : null,
                product.getCountry() != null ? product.getCountry().getNameAr() : null,
                product.getWizardStep(),
                product.isWizardCompleted(),
                product.getMinAmount(),
                product.getMaxAmount(),
                product.getMinTenureMonths(),
                product.getMaxTenureMonths(),
                product.getAllowedTenures(),
                product.getBaseProfitRate(),
                product.getRateType(),
                product.getRepaymentFrequency(),
                product.getCurrency(),
                product.getFineractProductId(),
                product.isVisibleToCustomers(),
                product.isVisibleToPartners(),
                product.getCreatedAt()
        );
    }
}
