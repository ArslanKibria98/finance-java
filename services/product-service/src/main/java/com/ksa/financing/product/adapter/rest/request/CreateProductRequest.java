package com.ksa.financing.product.adapter.rest.request;

import com.ksa.financing.product.domain.model.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProductRequest(
    String productCode,
    @NotBlank String nameEn,
    String nameAr,
    String descriptionEn,
    String descriptionAr,
    String shortDescriptionEn,
    String shortDescriptionAr,
    @NotNull ProductType productType,
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
    UUID countryId
) {}
