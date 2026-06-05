package com.ksa.financing.risk.domain.port.out;

public interface NidFormatValidator {

    NidValidationResult validate(NidValidationInput input);

    record NidValidationInput(String nationalId) {}

    record NidValidationResult(boolean valid, String idType, String failureReason) {}
}
