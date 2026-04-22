package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Fineract Reschedule API Response DTO.
 * Returned by POST /rescheduleloans and POST /rescheduleloans/{id}?command=approve
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FineractRescheduleResponse {

    @JsonProperty("resourceId")
    private Long resourceId;           // Fineract reschedule request ID

    @JsonProperty("loanId")
    private Long loanId;

    @JsonProperty("clientId")
    private Long clientId;

    @JsonProperty("changes")
    private Object changes;            // Fineract returns changed fields on approve
}
