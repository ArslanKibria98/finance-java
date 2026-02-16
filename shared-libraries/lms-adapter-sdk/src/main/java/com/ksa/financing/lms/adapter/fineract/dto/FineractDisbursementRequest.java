package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for loan disbursement in Fineract.
 */
@Data
@Builder
public class FineractDisbursementRequest {

    @JsonProperty("actualDisbursementDate")
    @JsonFormat(pattern = "dd MMMM yyyy")
    private LocalDate actualDisbursementDate;

    @JsonProperty("transactionAmount")
    private BigDecimal transactionAmount;

    @JsonProperty("paymentTypeId")
    private Long paymentTypeId;

    @JsonProperty("note")
    private String note;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("checkNumber")
    private String checkNumber;

    @JsonProperty("routingCode")
    private String routingCode;

    @JsonProperty("receiptNumber")
    private String receiptNumber;

    @JsonProperty("bankNumber")
    private String bankNumber;

    @JsonProperty("locale")
    private String locale = "en";

    @JsonProperty("dateFormat")
    private String dateFormat = "dd MMMM yyyy";
}