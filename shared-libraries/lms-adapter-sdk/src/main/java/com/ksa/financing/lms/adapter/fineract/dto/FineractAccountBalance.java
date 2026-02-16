package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for Fineract GL account balance.
 */
@Data
public class FineractAccountBalance {

    @JsonProperty("glAccountId")
    private Long glAccountId;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("accountCode")
    private String accountCode;

    @JsonProperty("debitBalance")
    private BigDecimal debitBalance;

    @JsonProperty("creditBalance")
    private BigDecimal creditBalance;

    @JsonProperty("netBalance")
    private BigDecimal netBalance;

    @JsonProperty("asOfDate")
    private LocalDate asOfDate;
}