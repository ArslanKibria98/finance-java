package com.ksa.financing.lms.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Result of loan product creation in the CBS.
 */
@Data
@Builder
public class LoanProductResult {

    private String loanProductId;
    private String shortName;
    private boolean success;
    private String errorMessage;

    public static LoanProductResult failed(String errorMessage) {
        return LoanProductResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
