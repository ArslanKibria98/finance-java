package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO from Fineract loan product creation.
 */
@Data
@NoArgsConstructor
public class FineractLoanProductResponse {

    @JsonProperty("resourceId")
    private Long resourceId;

    @JsonProperty("subResourceId")
    private Long subResourceId;
}
