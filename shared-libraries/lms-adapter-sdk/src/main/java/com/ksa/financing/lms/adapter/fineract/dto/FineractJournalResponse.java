package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response DTO from Fineract journal entry creation.
 */
@Data
public class FineractJournalResponse {

    @JsonProperty("resourceId")
    private Long resourceId;

    @JsonProperty("transactionId")
    private String transactionId;
}