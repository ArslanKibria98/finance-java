package com.ksa.financing.lms.adapter.fineract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for Fineract currency information.
 */
@Data
public class FineractCurrency {

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("decimalPlaces")
    private Integer decimalPlaces;

    @JsonProperty("inMultiplesOf")
    private Integer inMultiplesOf;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("nameCode")
    private String nameCode;

    @JsonProperty("displaySymbol")
    private String displaySymbol;
}