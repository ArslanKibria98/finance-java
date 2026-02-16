package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for early settlement in Fineract.
 */
@Data
@Builder
public class FineractSettlementRequest {

    @JsonProperty("transactionDate")
    @JsonFormat(pattern = "dd MMMM yyyy")
    private LocalDate transactionDate;

    @JsonProperty("transactionAmount")
    private BigDecimal transactionAmount;

    @JsonProperty("note")
    private String note;

    @JsonProperty("locale")
    private String locale = "en";

    @JsonProperty("dateFormat")
    private String dateFormat = "dd MMMM yyyy";
}