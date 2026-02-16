package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for approving a loan in Fineract.
 */
@Data
@Builder
public class FineractApprovalRequest {

    @JsonProperty("approvedOnDate")
    @JsonFormat(pattern = "dd MMMM yyyy")
    private LocalDate approvedOnDate;

    @JsonProperty("approvedLoanAmount")
    private BigDecimal approvedAmount;

    @JsonProperty("expectedDisbursementDate")
    @JsonFormat(pattern = "dd MMMM yyyy")
    private LocalDate expectedDisbursementDate;

    @JsonProperty("note")
    private String note;

    @JsonProperty("locale")
    private String locale = "en";

    @JsonProperty("dateFormat")
    private String dateFormat = "dd MMMM yyyy";
}