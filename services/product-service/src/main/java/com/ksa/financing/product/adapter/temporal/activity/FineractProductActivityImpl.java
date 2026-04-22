package com.ksa.financing.product.adapter.temporal.activity;

import com.ksa.financing.product.infrastructure.ledger.LedgerServiceClient;
import com.ksa.islamic.orchestration.activity.product.FineractProductActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FineractProductActivityImpl implements FineractProductActivity {

    private final LedgerServiceClient ledgerServiceClient;

    @Override
    public CreateFineractProductResult createLoanProduct(CreateFineractProductInput input) {
        log.info("Creating loan product via ledger-service: productCode={} tenant={}", input.productCode(), input.tenantId());

        try {
            String shortName = generateUniqueShortName(input.productCode());
            int interestType = "FLAT".equalsIgnoreCase(input.rateType()) ? 1 : 0;
            String fineractName = input.nameEn() + " (" + input.productCode() + ")";

            // Build Fineract loanproducts payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", fineractName);
            payload.put("shortName", shortName);
            payload.put("description", input.nameEn() + " - " + input.shariaStructure());
            payload.put("currencyCode", input.currency() != null ? input.currency() : "SAR");
            payload.put("digitsAfterDecimal", 2);
            payload.put("inMultiplesOf", 0);
            payload.put("principal", input.minAmount() != null ? input.minAmount() : new BigDecimal("10000"));
            payload.put("minPrincipal", input.minAmount());
            payload.put("maxPrincipal", input.maxAmount());
            payload.put("numberOfRepayments", input.maxTenureMonths() > 0 ? input.maxTenureMonths() : 12);
            payload.put("repaymentEvery", 1);
            payload.put("repaymentFrequencyType", 2); // MONTHS
            payload.put("interestRatePerPeriod", input.baseProfitRate() != null ? input.baseProfitRate() : new BigDecimal("5.0"));
            payload.put("interestRateFrequencyType", 2); // PER_YEAR
            payload.put("amortizationType", 1); // EQUAL_INSTALLMENTS
            payload.put("interestType", interestType);
            payload.put("interestCalculationPeriodType", 1); // SAME_AS_REPAYMENT
            payload.put("daysInYearType", 365);
            payload.put("daysInMonthType", 30);
            payload.put("isInterestRecalculationEnabled", false);
            payload.put("transactionProcessingStrategyCode", "mifos-standard-strategy");
            payload.put("accountingRule", 1); // NONE
            payload.put("locale", "en");
            payload.put("dateFormat", "yyyy-MM-dd");
            if (input.gracePeriodDays() > 0) {
                payload.put("graceOnPrincipalPayment", 1);
            }
            payload.put("externalId", input.productId());

            // Route through ledger-service → Fineract (not direct)
            Long resourceId = ledgerServiceClient.createLoanProduct(payload);

            String fineractProductId = String.valueOf(resourceId);
            log.info("Loan product created via ledger-service: fineractProductId={}", fineractProductId);

            return new CreateFineractProductResult(fineractProductId, shortName, true, null);

        } catch (Exception e) {
            log.error("Failed to create loan product via ledger-service: {}", e.getMessage(), e);
            return new CreateFineractProductResult(null, null, false, e.getMessage());
        }
    }

    /**
     * Generates a unique 4-char shortName for Fineract by hashing the product code.
     * Takes the first letter of the code prefix + 3 hex chars from hash.
     * Example: "MRB-E2E-001" → "ME7A" (M + 3-char hash)
     */
    private String generateUniqueShortName(String productCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(productCode.getBytes(StandardCharsets.UTF_8));
            String hexHash = String.format("%02x%02x", hash[0], hash[1]).toUpperCase();
            String prefix = productCode.substring(0, 1).toUpperCase();
            return prefix + hexHash.substring(0, 3);
        } catch (Exception e) {
            // Fallback: use last 4 chars of product code
            return productCode.length() > 4
                    ? productCode.substring(productCode.length() - 4).toUpperCase()
                    : productCode.toUpperCase();
        }
    }

    @Override
    public void deactivateLoanProduct(DeactivateFineractProductInput input) {
        log.info("Deactivating Fineract loan product: fineractProductId={} tenant={}",
                input.fineractProductId(), input.tenantId());
        log.warn("Fineract product deactivation is a no-op for compensation. Manual cleanup may be needed for fineractProductId={}", input.fineractProductId());
    }
}
