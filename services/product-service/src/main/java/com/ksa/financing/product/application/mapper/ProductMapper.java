package com.ksa.financing.product.application.mapper;

// TODO [ARCH-VIOLATION] Application layer imports adapter layer DTOs.
// Fix: Move ProductResponse/ProductSummaryResponse to application.dto package,
// or create application-layer DTOs and map to adapter responses in the controller.
import com.ksa.financing.product.adapter.rest.response.ProductResponse;
import com.ksa.financing.product.adapter.rest.response.ProductResponse.AdminFeeSlabResponse;
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
                product.getAdminFeeSlabs() != null
                    ? product.getAdminFeeSlabs().stream()
                        .map(s -> new AdminFeeSlabResponse(
                                s.id(), s.minAmount(), s.maxAmount(),
                                s.profitPercentage(), s.processingFee(),
                                s.adminFee(), s.partnerScope(), s.status(),
                                s.sortOrder(), s.minTenure(), s.maxTenure()))
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
                product.getProductType() != null ? product.getProductType().name() : null,
                product.getStatus() != null ? product.getStatus().name() : null,
                product.getMasterCategoryId(),
                product.getSubCategoryId(),
                product.getWizardStep(),
                product.isWizardCompleted(),
                product.getCreatedAt()
        );
    }
}
