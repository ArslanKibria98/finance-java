package com.ksa.financing.product.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.product.domain.model.Product;
import com.ksa.financing.product.domain.model.ProductType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageProductUseCase {
    Product create(CreateProductCommand command);
    Product getById(UUID tenantId, UUID productId);
    Product getById(UUID productId);
    List<Product> listByTenant(UUID tenantId);
    PageResponse<Product> listByTenant(UUID tenantId, PageQuery query);
    Product updateBasicInfo(UUID tenantId, UUID productId, UpdateBasicInfoCommand command);
    ActivationResultDto activate(UUID tenantId, UUID productId);
    void deactivate(UUID tenantId, UUID productId);
    void softDelete(UUID tenantId, UUID productId);

    record ActivationResultDto(
        String productId,
        String fineractProductId,
        String status,
        String failureReason,
        boolean success,
        String workflowId
    ) {}

    record CreateProductCommand(
        UUID tenantId,
        String productCode,
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        String shortDescriptionEn,
        String shortDescriptionAr,
        ProductType productType,
        String targetSegment,
        UUID masterCategoryId,
        UUID subCategoryId,
        UUID templateId,
        String notificationEmail,
        List<String> customerTypes,
        boolean involvesCommodity,
        String setupMethod,
        String shariaStructure,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Integer minTenureMonths,
        Integer maxTenureMonths,
        List<Integer> allowedTenures,
        BigDecimal baseProfitRate,
        String rateType,
        String repaymentFrequency,
        int gracePeriodDays,
        boolean earlySettlementAllowed,
        boolean penaltyWaiverAllowed,
        Integer maxPenaltyWaiversAllowed,
        UUID countryId,
        UUID createdBy
    ) {}

    record UpdateBasicInfoCommand(
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        String shortDescriptionEn,
        String shortDescriptionAr,
        String notificationEmail,
        List<String> customerTypes,
        boolean involvesCommodity,
        String logoUrl,
        UUID countryId,
        UUID updatedBy
    ) {}
}
