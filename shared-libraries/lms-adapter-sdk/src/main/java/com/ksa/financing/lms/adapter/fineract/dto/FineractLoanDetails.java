package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Fineract loan details response.
 */
@Data
public class FineractLoanDetails {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("accountNo")
    private String accountNo;

    @JsonProperty("clientId")
    private Long clientId;

    @JsonProperty("clientName")
    private String clientName;

    @JsonProperty("loanProductId")
    private Long loanProductId;

    @JsonProperty("loanProductName")
    private String loanProductName;

    @JsonProperty("principal")
    private BigDecimal principal;

    @JsonProperty("numberOfRepayments")
    private Integer numberOfRepayments;

    @JsonProperty("repaymentFrequencyType")
    private Integer repaymentFrequencyType;

    @JsonProperty("status")
    private FineractLoanStatus status;

    @JsonProperty("approvedOnDate")
    private LocalDate approvedOnDate;

    @JsonProperty("actualDisbursementDate")
    private LocalDate actualDisbursementDate;

    @JsonProperty("maturityDate")
    private LocalDate maturityDate;

    @JsonProperty("disbursedAmount")
    private BigDecimal disbursedAmount;

    @JsonProperty("principalOutstanding")
    private BigDecimal principalOutstanding;

    @JsonProperty("interestOutstanding")
    private BigDecimal interestOutstanding;

    @JsonProperty("totalOutstanding")
    private BigDecimal totalOutstanding;

    @JsonProperty("interestCharged")
    private BigDecimal interestCharged;

    @JsonProperty("installmentAmount")
    private BigDecimal installmentAmount;

    @JsonProperty("lastPaymentDate")
    private LocalDate lastPaymentDate;

    @JsonProperty("lastPaymentAmount")
    private BigDecimal lastPaymentAmount;

    @JsonProperty("nextPaymentDueDate")
    private LocalDate nextPaymentDueDate;

    @JsonProperty("numberOfRepaymentsPaid")
    private Integer numberOfRepaymentsPaid;

    @JsonProperty("numberOfRepaymentsRemaining")
    private Integer numberOfRepaymentsRemaining;

    @JsonProperty("daysInArrears")
    private Integer daysInArrears;

    @JsonProperty("totalOverdue")
    private BigDecimal totalOverdue;

    @JsonProperty("penaltyChargesOutstanding")
    private BigDecimal penaltyChargesOutstanding;

    @JsonProperty("officeId")
    private Long officeId;

    @JsonProperty("loanOfficerId")
    private Long loanOfficerId;

    @JsonProperty("lastModifiedOn")
    private LocalDateTime lastModifiedOn;
}