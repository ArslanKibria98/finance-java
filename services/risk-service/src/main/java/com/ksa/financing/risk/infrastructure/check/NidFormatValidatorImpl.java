package com.ksa.financing.risk.infrastructure.check;

import com.ksa.financing.domain.valueobject.NationalId;
import com.ksa.financing.risk.domain.port.out.NidFormatValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NidFormatValidatorImpl implements NidFormatValidator {

    @Override
    public NidValidationResult validate(NidValidationInput input) {
        log.info("Starting NID format validation");

        try {
            String nationalIdStr = input.nationalId();

            if (nationalIdStr == null || nationalIdStr.isBlank()) {
                log.warn("NID is null or blank");
                return new NidValidationResult(false, null, "National ID is required");
            }

            NationalId nationalId = NationalId.of(nationalIdStr);
            String idType = nationalId.isCitizen() ? "CITIZEN" : "RESIDENT";

            log.info("NID format validation passed, idType: {}", idType);
            return new NidValidationResult(true, idType, null);

        } catch (IllegalArgumentException e) {
            log.warn("NID format invalid: {}", e.getMessage());
            return new NidValidationResult(false, null,
                "Invalid National ID format. Please check and re-enter.");
        } catch (Exception e) {
            log.error("NID format validation error", e);
            return new NidValidationResult(false, null, "Validation error");
        }
    }

}
