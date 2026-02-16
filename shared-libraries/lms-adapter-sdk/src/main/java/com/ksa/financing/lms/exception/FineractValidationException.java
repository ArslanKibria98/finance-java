package com.ksa.financing.lms.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception for Fineract business rule violations.
 */
public class FineractValidationException extends FineractException {

    private Map<String, List<String>> validationErrors;

    public FineractValidationException(String message) {
        super(message);
    }

    public FineractValidationException(String message, Map<String, List<String>> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }
}