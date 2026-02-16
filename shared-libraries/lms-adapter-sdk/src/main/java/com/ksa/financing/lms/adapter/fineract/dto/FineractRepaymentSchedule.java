package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for Fineract repayment schedule response.
 */
@Data
public class FineractRepaymentSchedule {

    @JsonProperty("currency")
    private FineractCurrency currency;

    @JsonProperty("loanTermInDays")
    private Integer loanTermInDays;

    @JsonProperty("totalPrincipalDisbursed")
    private BigDecimal totalPrincipalDisbursed;

    @JsonProperty("totalPrincipalExpected")
    private BigDecimal totalPrincipalExpected;

    @JsonProperty("totalPrincipalPaid")
    private BigDecimal totalPrincipalPaid;

    @JsonProperty("totalInterestCharged")
    private BigDecimal totalInterestCharged;

    @JsonProperty("totalFeeChargesCharged")
    private BigDecimal totalFeeChargesCharged;

    @JsonProperty("totalPenaltyChargesCharged")
    private BigDecimal totalPenaltyChargesCharged;

    @JsonProperty("totalWaived")
    private BigDecimal totalWaived;

    @JsonProperty("totalWrittenOff")
    private BigDecimal totalWrittenOff;

    @JsonProperty("totalRepaymentExpected")
    private BigDecimal totalRepaymentExpected;

    @JsonProperty("totalRepayment")
    private BigDecimal totalRepayment;

    @JsonProperty("totalOutstanding")
    private BigDecimal totalOutstanding;

    @JsonProperty("periods")
    private List<FineractRepaymentPeriod> periods;
}