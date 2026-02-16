package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for Fineract loan status.
 */
@Data
public class FineractLoanStatus {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("code")
    private String code;

    @JsonProperty("value")
    private String value;

    @JsonProperty("pendingApproval")
    private Boolean pendingApproval;

    @JsonProperty("waitingForDisbursal")
    private Boolean waitingForDisbursal;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("closedObligationsMet")
    private Boolean closedObligationsMet;

    @JsonProperty("closedWrittenOff")
    private Boolean closedWrittenOff;

    @JsonProperty("closedRescheduled")
    private Boolean closedRescheduled;

    @JsonProperty("closed")
    private Boolean closed;

    @JsonProperty("overpaid")
    private Boolean overpaid;
}