package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Response DTO for Fineract transaction operations.
 */
@Data
public class FineractTransactionResponse {

    @JsonProperty("resourceId")
    private Long resourceId;

    @JsonProperty("transactionId")
    private Long transactionId;

    @JsonProperty("loanId")
    private Long loanId;

    @JsonProperty("transactionDate")
    private LocalDate transactionDate;

    @JsonProperty("transactionAmount")
    private BigDecimal transactionAmount;

    @JsonProperty("changes")
    private Map<String, Object> changes;
}