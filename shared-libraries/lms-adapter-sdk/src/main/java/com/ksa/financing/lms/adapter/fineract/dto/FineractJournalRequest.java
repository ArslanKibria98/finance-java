package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating journal entries in Fineract.
 */
@Data
@Builder
public class FineractJournalRequest {

    @JsonProperty("transactionDate")
    @JsonFormat(pattern = "dd MMMM yyyy")
    private LocalDate transactionDate;

    @JsonProperty("comments")
    private String comments;

    @JsonProperty("currencyCode")
    private String currencyCode = "SAR";

    @JsonProperty("debits")
    private List<JournalLine> debits;

    @JsonProperty("credits")
    private List<JournalLine> credits;

    @JsonProperty("referenceNumber")
    private String referenceNumber;

    @JsonProperty("accountingRule")
    private Long accountingRule;

    @JsonProperty("locale")
    private String locale = "en";

    @JsonProperty("dateFormat")
    private String dateFormat = "dd MMMM yyyy";

    @Data
    @Builder
    public static class JournalLine {
        @JsonProperty("glAccountId")
        private Long glAccountId;

        @JsonProperty("amount")
        private BigDecimal amount;

        @JsonProperty("comments")
        private String comments;
    }
}