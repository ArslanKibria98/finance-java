package com.ksa.financing.ledger.application.dto;

import java.util.List;

public record CoaConfigurationValidationResponse(
        boolean valid,
        List<String> missingMandatoryFieldKeys,
        List<String> invalidAccountCodes
) {}
