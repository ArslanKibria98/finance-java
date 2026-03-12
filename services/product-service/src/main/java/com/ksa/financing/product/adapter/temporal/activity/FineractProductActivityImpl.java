package com.ksa.financing.product.adapter.temporal.activity;

import com.ksa.financing.lms.adapter.fineract.dto.FineractLoanProductRequest;
import com.ksa.financing.lms.adapter.fineract.dto.FineractLoanProductResponse;
import com.ksa.financing.product.infrastructure.fineract.ProductFineractClient;
import com.ksa.islamic.orchestration.activity.product.FineractProductActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
@Slf4j
public class FineractProductActivityImpl implements FineractProductActivity {

    private final ProductFineractClient fineractClient;

    @Override
    public CreateFineractProductResult createLoanProduct(CreateFineractProductInput input) {
        log.info("Creating loan product in Fineract: productCode={} tenant={}", input.productCode(), input.tenantId());

        try {
            BigDecimal defaultPrincipal = input.minAmount().add(input.maxAmount())
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

            int defaultTenure = (input.minTenureMonths() + input.maxTenureMonths()) / 2;
            if (defaultTenure <= 0) defaultTenure = input.minTenureMonths();

            String shortName = input.productCode().length() > 4
                    ? input.productCode().substring(0, 4).toUpperCase()
                    : input.productCode().toUpperCase();

            int interestType = "FLAT".equalsIgnoreCase(input.rateType()) ? 1 : 0;

            var request = FineractLoanProductRequest.builder()
                    .name(input.nameEn())
                    .shortName(shortName)
                    .description(input.nameEn() + " - " + input.shariaStructure())
                    .currencyCode(input.currency() != null ? input.currency() : "SAR")
                    .principal(defaultPrincipal)
                    .minPrincipal(input.minAmount())
                    .maxPrincipal(input.maxAmount())
                    .numberOfRepayments(defaultTenure)
                    .minNumberOfRepayments(input.minTenureMonths())
                    .maxNumberOfRepayments(input.maxTenureMonths())
                    .interestRatePerPeriod(input.baseProfitRate())
                    .interestType(interestType)
                    .graceOnPrincipalPayment(input.gracePeriodDays() > 0 ? 1 : null)
                    .externalId(input.productId())
                    .build();

            FineractLoanProductResponse response = fineractClient.createLoanProduct(request);

            String fineractProductId = String.valueOf(response.getResourceId());
            log.info("Loan product created in Fineract: fineractProductId={}", fineractProductId);

            return new CreateFineractProductResult(fineractProductId, shortName, true, null);

        } catch (Exception e) {
            log.error("Failed to create loan product in Fineract: {}", e.getMessage(), e);
            return new CreateFineractProductResult(null, null, false, e.getMessage());
        }
    }

    @Override
    public void deactivateLoanProduct(DeactivateFineractProductInput input) {
        log.info("Deactivating Fineract loan product: fineractProductId={} tenant={}",
                input.fineractProductId(), input.tenantId());
        log.warn("Fineract product deactivation is a no-op for compensation. Manual cleanup may be needed for fineractProductId={}", input.fineractProductId());
    }
}
