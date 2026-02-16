package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a loan in Fineract.
 */
@Data
@Builder
public class FineractLoanRequest {

    @JsonProperty("clientId")
    private Long clientId;

    @JsonProperty("productId")
    private Long productId;

    @JsonProperty("principal")
    private BigDecimal principal;

    @JsonProperty("loanTermFrequency")
    private Integer loanTermFrequency;

    @JsonProperty("loanTermFrequencyType")
    private Integer loanTermFrequencyType; // 2 = Months

    @JsonProperty("numberOfRepayments")
    private Integer numberOfRepayments;

    @JsonProperty("repaymentEvery")
    private Integer repaymentEvery;

    @JsonProperty("repaymentFrequencyType")
    private Integer repaymentFrequencyType; // 2 = Months

    @JsonProperty("interestRatePerPeriod")
    private BigDecimal interestRatePerPeriod;

    @JsonProperty("amortizationType")
    private Integer amortizationType; // 1 = Equal installments

    @JsonProperty("interestType")
    private Integer interestType; // 0 = Declining Balance

    @JsonProperty("interestCalculationPeriodType")
    private Integer interestCalculationPeriodType; // 1 = Same as repayment period

    @JsonProperty("expectedDisbursementDate")
    @JsonFormat(pattern = "dd MMMM yyyy")
    private LocalDate expectedDisbursementDate;

    @JsonProperty("submittedOnDate")
    @JsonFormat(pattern = "dd MMMM yyyy")
    private LocalDate submittedOnDate;

    @JsonProperty("loanOfficerId")
    private Long loanOfficerId;

    @JsonProperty("fundId")
    private Long fundId;

    @JsonProperty("loanPurposeId")
    private Long loanPurposeId;

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("transactionProcessingStrategyId")
    private Long transactionProcessingStrategyId;

    @JsonProperty("dateFormat")
    private String dateFormat = "dd MMMM yyyy";

    @JsonProperty("locale")
    private String locale = "en";

    // Islamic Finance specific - stored in datatables
    @JsonProperty("datatables")
    private List<DataTableEntry> datatables;

    @Data
    @Builder
    public static class DataTableEntry {
        private String registeredTableName;
        private Map<String, Object> data;
    }
}