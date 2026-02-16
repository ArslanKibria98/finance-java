package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response DTO from Fineract loan creation.
 */
@Data
public class FineractLoanResponse {

    @JsonProperty("loanId")
    private Long loanId;

    @JsonProperty("resourceId")
    private Long resourceId;

    @JsonProperty("clientId")
    private Long clientId;

    @JsonProperty("officeId")
    private Long officeId;

    @JsonProperty("externalId")
    private String externalId;
}