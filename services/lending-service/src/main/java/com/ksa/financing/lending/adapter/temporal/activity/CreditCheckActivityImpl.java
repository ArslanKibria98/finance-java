package com.ksa.financing.lending.adapter.temporal.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.domain.sharia.MurabahaCalculator;
import com.ksa.financing.domain.sharia.MurabahaCalculation;
import com.ksa.financing.domain.valueobject.SarMoney;
import com.ksa.financing.domain.valueobject.ProfitRate;
import com.ksa.financing.domain.valueobject.Tenure;
import com.ksa.islamic.orchestration.activity.lending.CreditCheckActivity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Performs credit check by calling risk-service (which calls SIMAH via middleware-third-party).
 * Also calculates eligibility (DBR) and Sharia-compliant offer using domain-core-sdk.
 */
@Slf4j
@Component
public class CreditCheckActivityImpl implements CreditCheckActivity {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String riskServiceUrl;

    public CreditCheckActivityImpl(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.risk-service-url}") String riskServiceUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.riskServiceUrl = riskServiceUrl;
    }

    @Override
    public CreditCheckResult performCreditCheck(CreditCheckInput input) {
        log.info("Activity: Performing credit check for NID={}, tenant={}", input.nationalId(), input.tenantId());

        try {
            // Call risk-service which internally calls SIMAH via middleware-third-party
            String url = riskServiceUrl + "/api/v1/credit-check";

            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "tenantId", input.tenantId(),
                    "nationalId", input.nationalId(),
                    "customerId", input.customerId(),
                    "requestedAmount", input.requestedAmount()
            ));

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            var response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Credit check returned non-success: {}", response.getStatusCode());
                return new CreditCheckResult(0, "UNKNOWN", null, BigDecimal.ZERO, BigDecimal.ZERO, true);
            }

            var result = objectMapper.readTree(response.getBody());

            int creditScore = intOrZero(result, "creditScore");
            String simahGrade = textOrNull(result, "simahGrade");
            String simahReferenceId = textOrNull(result, "simahReferenceId");
            BigDecimal verifiedSalary = decimalOrZero(result, "verifiedSalary");
            BigDecimal existingObligations = decimalOrZero(result, "existingObligations");
            boolean hasActiveDefaults = result.has("hasActiveDefaults") && result.get("hasActiveDefaults").asBoolean();

            log.info("Credit check completed: score={}, grade={}", creditScore, simahGrade);
            return new CreditCheckResult(
                    creditScore, simahGrade, simahReferenceId,
                    verifiedSalary, existingObligations, hasActiveDefaults
            );

        } catch (Exception e) {
            log.warn("Credit check service unavailable ({}), returning mock data for development.", e.getMessage());
            // Mock result for development/testing — real integration requires risk-service
            return new CreditCheckResult(
                    720, "A", "SIMAH-MOCK-" + java.util.UUID.randomUUID().toString().substring(0, 8),
                    new BigDecimal("15000"), BigDecimal.ZERO, false);
        }
    }

    @Override
    public EligibilityResult calculateEligibility(EligibilityInput input) {
        log.info("Activity: Calculating eligibility for tenant={}", input.tenantId());

        // Check credit score
        if (input.minCreditScore() > 0 && input.creditScore() < input.minCreditScore()) {
            return new EligibilityResult(false, BigDecimal.ZERO, null, null,
                    "Credit score " + input.creditScore() + " below minimum " + input.minCreditScore());
        }

        // Check defaults
        if (input.hasActiveDefaults()) {
            return new EligibilityResult(false, BigDecimal.ZERO, null, null,
                    "Customer has active defaults in SIMAH");
        }

        // Check salary
        if (input.minSalary() != null && input.verifiedSalary().compareTo(input.minSalary()) < 0) {
            return new EligibilityResult(false, BigDecimal.ZERO, null, null,
                    "Salary " + input.verifiedSalary() + " below minimum " + input.minSalary());
        }

        // Check age
        if (input.minAge() > 0 && input.customerAge() < input.minAge()) {
            return new EligibilityResult(false, BigDecimal.ZERO, null, null,
                    "Age " + input.customerAge() + " below minimum " + input.minAge());
        }
        if (input.maxAge() > 0 && input.customerAge() > input.maxAge()) {
            return new EligibilityResult(false, BigDecimal.ZERO, null, null,
                    "Age " + input.customerAge() + " exceeds maximum " + input.maxAge());
        }

        // Check employment duration
        if (input.minEmploymentMonths() > 0 && input.employmentDurationMonths() < input.minEmploymentMonths()) {
            return new EligibilityResult(false, BigDecimal.ZERO, null, null,
                    "Employment duration " + input.employmentDurationMonths() + " months below minimum " + input.minEmploymentMonths());
        }

        // Calculate DBR (Debt Burden Ratio)
        // DBR = (existing obligations + proposed installment) / verified salary * 100
        BigDecimal proposedInstallment = calculateMonthlyInstallment(
                input.requestedAmount(), input.profitRate(), input.requestedTenureMonths());

        BigDecimal dbrBefore = input.verifiedSalary().compareTo(BigDecimal.ZERO) > 0
                ? input.existingObligations().divide(input.verifiedSalary(), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal totalObligations = input.existingObligations().add(proposedInstallment);
        BigDecimal dbrAfter = input.verifiedSalary().compareTo(BigDecimal.ZERO) > 0
                ? totalObligations.divide(input.verifiedSalary(), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.valueOf(100);

        BigDecimal maxDbr = input.maxDbrPercent() != null ? input.maxDbrPercent() : BigDecimal.valueOf(65);
        if (dbrAfter.compareTo(maxDbr) > 0) {
            // Calculate max eligible amount based on DBR cap
            BigDecimal availableForInstallment = input.verifiedSalary()
                    .multiply(maxDbr).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                    .subtract(input.existingObligations());

            BigDecimal maxEligible = calculateMaxPrincipal(
                    availableForInstallment, input.profitRate(), input.requestedTenureMonths());

            if (maxEligible.compareTo(BigDecimal.ZERO) <= 0) {
                return new EligibilityResult(false, BigDecimal.ZERO, dbrBefore, dbrAfter,
                        "DBR exceeds maximum " + maxDbr + "% — no eligible amount");
            }

            return new EligibilityResult(true, maxEligible, dbrBefore, dbrAfter, null);
        }

        log.info("Eligibility passed: dbrBefore={}%, dbrAfter={}%", dbrBefore, dbrAfter);
        return new EligibilityResult(true, input.requestedAmount(), dbrBefore, dbrAfter, null);
    }

    @Override
    public ProfitCalculationResult calculateOffer(ProfitCalculationInput input) {
        log.info("Activity: Calculating offer for structure={}, amount={}",
                input.shariaStructure(), input.principalAmount());

        // Use domain-core-sdk MurabahaCalculator for Sharia-compliant calculation
        MurabahaCalculation calc = MurabahaCalculator.calculate(
                SarMoney.of(input.principalAmount()),
                new ProfitRate(input.profitRate()),
                new Tenure(input.tenureMonths()),
                java.time.LocalDate.now()
        );

        BigDecimal processingFee = input.processingFeePercent() != null
                ? input.principalAmount().multiply(input.processingFeePercent())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal adminFee = input.adminFeeAmount() != null ? input.adminFeeAmount() : BigDecimal.ZERO;

        log.info("Offer calculated: monthly={}, totalProfit={}, totalPayable={}",
                calc.monthlyInstallment(), calc.profitAmount(), calc.salePrice());

        return new ProfitCalculationResult(
                calc.monthlyInstallment().getValue(),
                calc.profitAmount().getValue(),
                calc.salePrice().getValue(),  // totalPayable = sale price
                calc.salePrice().getValue(),  // sellingPrice
                processingFee,
                adminFee
        );
    }

    private BigDecimal calculateMonthlyInstallment(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (tenureMonths <= 0 || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalProfit = principal.multiply(annualRate)
                .multiply(BigDecimal.valueOf(tenureMonths))
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
        BigDecimal total = principal.add(totalProfit);
        return total.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMaxPrincipal(BigDecimal maxInstallment, BigDecimal annualRate, int tenureMonths) {
        if (tenureMonths <= 0 || maxInstallment.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // Reverse: principal = installment * months / (1 + rate * months/12)
        BigDecimal rateForTenure = annualRate.multiply(BigDecimal.valueOf(tenureMonths))
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.ONE.add(rateForTenure);
        return maxInstallment.multiply(BigDecimal.valueOf(tenureMonths))
                .divide(divisor, 2, RoundingMode.HALF_UP);
    }

    private String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private int intOrZero(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asInt() : 0;
    }

    private BigDecimal decimalOrZero(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? new BigDecimal(node.get(field).asText()) : BigDecimal.ZERO;
    }
}
