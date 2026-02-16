package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

/**
 * Generic response DTO for Fineract command operations.
 */
@Data
public class FineractCommandResponse {

    @JsonProperty("resourceId")
    private Long resourceId;

    @JsonProperty("officeId")
    private Long officeId;

    @JsonProperty("clientId")
    private Long clientId;

    @JsonProperty("loanId")
    private Long loanId;

    @JsonProperty("savingsId")
    private Long savingsId;

    @JsonProperty("subResourceId")
    private Long subResourceId;

    @JsonProperty("changes")
    private Map<String, Object> changes;
}