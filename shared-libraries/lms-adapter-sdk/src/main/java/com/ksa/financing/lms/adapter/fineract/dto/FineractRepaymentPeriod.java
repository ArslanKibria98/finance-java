package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for a single repayment period in Fineract.
 */
@Data
public class FineractRepaymentPeriod {

    @JsonProperty("period")
    private Integer period;

    @JsonProperty("fromDate")
    private LocalDate fromDate;

    @JsonProperty("dueDate")
    private LocalDate dueDate;

    @JsonProperty("obligationsMetOnDate")
    private LocalDate obligationsMetOnDate;

    @JsonProperty("complete")
    private boolean complete;

    @JsonProperty("daysInPeriod")
    private Integer daysInPeriod;

    @JsonProperty("principalOriginalDue")
    private BigDecimal principalOriginalDue;

    @JsonProperty("principalDue")
    private BigDecimal principalDue;

    @JsonProperty("principalPaid")
    private BigDecimal principalPaid;

    @JsonProperty("principalWrittenOff")
    private BigDecimal principalWrittenOff;

    @JsonProperty("principalOutstanding")
    private BigDecimal principalOutstanding;

    @JsonProperty("principalLoanBalanceOutstanding")
    private BigDecimal principalLoanBalanceOutstanding;

    @JsonProperty("interestOriginalDue")
    private BigDecimal interestOriginalDue;

    @JsonProperty("interestDue")
    private BigDecimal interestDue;

    @JsonProperty("interestPaid")
    private BigDecimal interestPaid;

    @JsonProperty("interestWaived")
    private BigDecimal interestWaived;

    @JsonProperty("interestWrittenOff")
    private BigDecimal interestWrittenOff;

    @JsonProperty("interestOutstanding")
    private BigDecimal interestOutstanding;

    @JsonProperty("feeChargesDue")
    private BigDecimal feeChargesDue;

    @JsonProperty("feeChargesPaid")
    private BigDecimal feeChargesPaid;

    @JsonProperty("feeChargesWaived")
    private BigDecimal feeChargesWaived;

    @JsonProperty("feeChargesWrittenOff")
    private BigDecimal feeChargesWrittenOff;

    @JsonProperty("feeChargesOutstanding")
    private BigDecimal feeChargesOutstanding;

    @JsonProperty("penaltyChargesDue")
    private BigDecimal penaltyChargesDue;

    @JsonProperty("penaltyChargesPaid")
    private BigDecimal penaltyChargesPaid;

    @JsonProperty("penaltyChargesWaived")
    private BigDecimal penaltyChargesWaived;

    @JsonProperty("penaltyChargesWrittenOff")
    private BigDecimal penaltyChargesWrittenOff;

    @JsonProperty("penaltyChargesOutstanding")
    private BigDecimal penaltyChargesOutstanding;

    @JsonProperty("totalOriginalDueForPeriod")
    private BigDecimal totalOriginalDueForPeriod;

    @JsonProperty("totalDueForPeriod")
    private BigDecimal totalDueForPeriod;

    @JsonProperty("totalPaidForPeriod")
    private BigDecimal totalPaidForPeriod;

    @JsonProperty("totalPaidInAdvanceForPeriod")
    private BigDecimal totalPaidInAdvanceForPeriod;

    @JsonProperty("totalPaidLateForPeriod")
    private BigDecimal totalPaidLateForPeriod;

    @JsonProperty("totalWaivedForPeriod")
    private BigDecimal totalWaivedForPeriod;

    @JsonProperty("totalWrittenOffForPeriod")
    private BigDecimal totalWrittenOffForPeriod;

    @JsonProperty("totalOutstandingForPeriod")
    private BigDecimal totalOutstandingForPeriod;

    @JsonProperty("totalInstallmentAmountForPeriod")
    private BigDecimal totalInstallmentAmountForPeriod;
}