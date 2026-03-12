package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO for creating a loan product in Fineract.
 * Maps to POST /api/v1/loanproducts
 */
@Data
@Builder
public class FineractLoanProductRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("shortName")
    private String shortName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("currencyCode")
    private String currencyCode;

    @JsonProperty("digitsAfterDecimal")
    @Builder.Default
    private Integer digitsAfterDecimal = 2;

    @JsonProperty("inMultiplesOf")
    @Builder.Default
    private Integer inMultiplesOf = 1;

    @JsonProperty("principal")
    private BigDecimal principal;

    @JsonProperty("minPrincipal")
    private BigDecimal minPrincipal;

    @JsonProperty("maxPrincipal")
    private BigDecimal maxPrincipal;

    @JsonProperty("numberOfRepayments")
    private Integer numberOfRepayments;

    @JsonProperty("minNumberOfRepayments")
    private Integer minNumberOfRepayments;

    @JsonProperty("maxNumberOfRepayments")
    private Integer maxNumberOfRepayments;

    @JsonProperty("repaymentEvery")
    @Builder.Default
    private Integer repaymentEvery = 1;

    @JsonProperty("repaymentFrequencyType")
    @Builder.Default
    private Integer repaymentFrequencyType = 2; // Months

    @JsonProperty("interestRatePerPeriod")
    private BigDecimal interestRatePerPeriod;

    @JsonProperty("interestRateFrequencyType")
    @Builder.Default
    private Integer interestRateFrequencyType = 3; // Per Year

    @JsonProperty("amortizationType")
    @Builder.Default
    private Integer amortizationType = 1; // Equal installments

    @JsonProperty("interestType")
    @Builder.Default
    private Integer interestType = 0; // Declining balance

    @JsonProperty("interestCalculationPeriodType")
    @Builder.Default
    private Integer interestCalculationPeriodType = 1; // Same as repayment

    @JsonProperty("transactionProcessingStrategyCode")
    @Builder.Default
    private String transactionProcessingStrategyCode = "mifos-standard-strategy";

    @JsonProperty("graceOnPrincipalPayment")
    private Integer graceOnPrincipalPayment;

    @JsonProperty("graceOnInterestPayment")
    private Integer graceOnInterestPayment;

    @JsonProperty("graceOnArrearsAgeing")
    private Integer graceOnArrearsAgeing;

    @JsonProperty("daysInYearType")
    @Builder.Default
    private Integer daysInYearType = 360; // Islamic finance standard

    @JsonProperty("daysInMonthType")
    @Builder.Default
    private Integer daysInMonthType = 30;

    @JsonProperty("canDefineInstallmentAmount")
    @Builder.Default
    private Boolean canDefineInstallmentAmount = false;

    @JsonProperty("accountingRule")
    @Builder.Default
    private Integer accountingRule = 1; // None (for now)

    @JsonProperty("isInterestRecalculationEnabled")
    @Builder.Default
    private Boolean isInterestRecalculationEnabled = false;

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("locale")
    @Builder.Default
    private String locale = "en";

    @JsonProperty("dateFormat")
    @Builder.Default
    private String dateFormat = "dd MMMM yyyy";
}
