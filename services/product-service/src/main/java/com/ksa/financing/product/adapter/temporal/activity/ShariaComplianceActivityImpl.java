package com.ksa.financing.product.adapter.temporal.activity;

import com.ksa.islamic.orchestration.activity.product.ShariaComplianceActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShariaComplianceActivityImpl implements ShariaComplianceActivity {

    private static final Set<String> VALID_SHARIA_STRUCTURES = Set.of(
            "MURABAHA", "TAWARRUQ", "IJARA", "MUSHARAKAH", "DIMINISHING_MUSHARAKAH",
            "ISTISNA", "SALAM", "WAKALAH"
    );

    private static final BigDecimal MAX_PROFIT_RATE = new BigDecimal("30.00");

    @Override
    public ShariaCheckResult checkCompliance(ShariaCheckInput input) {
        log.info("Checking Sharia compliance for product: {} structure={}", input.productCode(), input.shariaStructure());

        // Validate Sharia structure
        if (input.shariaStructure() == null || input.shariaStructure().isBlank()) {
            return new ShariaCheckResult(false, "NON_COMPLIANT",
                    "Sharia structure is required for Islamic financing products", null);
        }

        if (!VALID_SHARIA_STRUCTURES.contains(input.shariaStructure().toUpperCase())) {
            return new ShariaCheckResult(false, "NON_COMPLIANT",
                    "Invalid Sharia structure: " + input.shariaStructure()
                            + ". Valid structures: " + VALID_SHARIA_STRUCTURES, null);
        }

        // Validate profit rate is within Sharia-compliant bounds
        if (input.baseProfitRate() != null && input.baseProfitRate().compareTo(MAX_PROFIT_RATE) > 0) {
            return new ShariaCheckResult(false, "NON_COMPLIANT",
                    "Profit rate " + input.baseProfitRate() + "% exceeds maximum allowed rate of " + MAX_PROFIT_RATE + "%", null);
        }

        // Validate currency is SAR for KSA products
        if (input.currency() != null && !"SAR".equalsIgnoreCase(input.currency())) {
            log.warn("Non-SAR currency detected: {}. Additional compliance review may be needed.", input.currency());
        }

        // Validate minimum amount is positive
        if (input.minAmount() != null && input.minAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return new ShariaCheckResult(false, "NON_COMPLIANT",
                    "Minimum financing amount must be positive", null);
        }

        // Generate a compliance reference
        String approvalRef = "SHARIA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Sharia compliance check passed for product: {} ref={}", input.productCode(), approvalRef);
        return new ShariaCheckResult(true, "COMPLIANT", null, approvalRef);
    }
}
