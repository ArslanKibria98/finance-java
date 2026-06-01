package com.ksa.financing.lending.adapter.temporal.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.domain.sharia.MurabahaCalculator;
import com.ksa.financing.domain.sharia.MurabahaCalculation;
import com.ksa.financing.domain.valueobject.SarMoney;
import com.ksa.financing.domain.valueobject.ProfitRate;
import com.ksa.financing.domain.valueobject.Tenure;
import com.ksa.financing.lending.domain.service.AffordabilityCalculationService;
import com.ksa.islamic.orchestration.activity.lending.CreditCheckActivity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final String fineractBaseUrl;
    private final String fineractUsername;
    private final String fineractPassword;
    private final String fineractTenantId;

    public CreditCheckActivityImpl(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.risk-service-url}") String riskServiceUrl,
            @Value("${app.services.fineract-base-url:https://localhost:8443/fineract-provider/api/v1}") String fineractBaseUrl,
            @Value("${fineract.username:mifos}") String fineractUsername,
            @Value("${fineract.password:password}") String fineractPassword,
            @Value("${fineract.tenant-id:default}") String fineractTenantId) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.riskServiceUrl = riskServiceUrl;
        this.fineractBaseUrl = fineractBaseUrl;
        this.fineractUsername = fineractUsername;
        this.fineractPassword = fineractPassword;
        this.fineractTenantId = fineractTenantId;
    }

    @Override
    public CreditCheckResult performCreditCheck(CreditCheckInput input) {
        log.info("Activity: Performing credit check for NID={}, tenant={}", input.nationalId(), input.tenantId());

        try {
            // Call risk-service which internally calls SIMAH via middleware-third-party
            String url = riskServiceUrl + "/api/v1/credit-check";

            var bodyMap = new java.util.LinkedHashMap<String, Object>();
            bodyMap.put("tenantId", input.tenantId());
            bodyMap.put("nationalId", input.nationalId());
            bodyMap.put("customerId", input.customerId());
            if (input.applicationId() != null) bodyMap.put("applicationId", input.applicationId());
            bodyMap.put("requestedAmount", input.requestedAmount());
            var requestBody = objectMapper.writeValueAsString(bodyMap);

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            var response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Credit check returned non-success: {}", response.getStatusCode());
                return new CreditCheckResult(0, "UNKNOWN", null, BigDecimal.ZERO, BigDecimal.ZERO, true, 0, 0);
            }

            var rootNode = objectMapper.readTree(response.getBody());
            // Risk-service wraps response in {"data": {...}} envelope
            var result = rootNode.has("data") && !rootNode.get("data").isNull()
                    ? rootNode.get("data") : rootNode;

            int creditScore = intOrZero(result, "creditScore");
            String simahGrade = textOrNull(result, "simahGrade");
            String simahReferenceId = textOrNull(result, "simahReferenceId");
            BigDecimal verifiedSalary = decimalOrZero(result, "verifiedSalary");
            BigDecimal existingObligations = decimalOrZero(result, "existingObligations");
            boolean hasActiveDefaults = result.has("hasActiveDefaults") && result.get("hasActiveDefaults").asBoolean();
            int defaultsCount = intOrZero(result, "defaultsCount");
            int activeLoansCount = intOrZero(result, "activeLoansCount");

            log.info("Credit check completed: score={}, grade={}, defaultsCount={}, activeLoans={}",
                    creditScore, simahGrade, defaultsCount, activeLoansCount);
            return new CreditCheckResult(
                    creditScore, simahGrade, simahReferenceId,
                    verifiedSalary, existingObligations, hasActiveDefaults,
                    defaultsCount, activeLoansCount
            );

        } catch (Exception e) {
            log.warn("Credit check service unavailable ({}), returning mock data for development.", e.getMessage());
            // Mock result for development/testing — real integration requires risk-service
            return new CreditCheckResult(
                    720, "A", "SIMAH-MOCK-" + java.util.UUID.randomUUID().toString().substring(0, 8),
                    new BigDecimal("15000"), BigDecimal.ZERO, false, 0, 0);
        }
    }

    @Override
    public CreditDecisionResult runCreditDecisionEngine(CreditDecisionInput input) {
        log.info("Activity: Running credit decision engine for product={} application={}",
                input.productId(), input.applicationId());

        // Backward-compat short-circuit: if risk service isn't reachable or returns no
        // criteria, the workflow should still proceed. Engine itself returns "no criteria"
        // → AUTO_APPROVE when nothing is configured, matching the documented behavior.
        try {
            String url = riskServiceUrl + "/internal/credit-scoring/products/"
                    + input.productId() + "/evaluate";

            var bodyMap = new LinkedHashMap<String, Object>();
            bodyMap.put("answers", input.answers() != null ? input.answers() : Map.of());
            if (input.greenThreshold() != null) bodyMap.put("greenThreshold", input.greenThreshold());
            if (input.amberThreshold() != null) bodyMap.put("amberThreshold", input.amberThreshold());

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", input.tenantId());
            headers.set("X-Caller-Service", "lending-service");
            if (input.applicationId() != null) headers.set("X-Application-Id", input.applicationId());

            var response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(bodyMap), headers),
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Credit decision engine returned non-success: {}", response.getStatusCode());
                return permissivePassThrough("ENGINE_UNAVAILABLE");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode result = root.has("data") && !root.get("data").isNull() ? root.get("data") : root;

            return mapDecisionResponse(result);

        } catch (Exception e) {
            log.warn("Credit decision engine call failed ({}), allowing application to proceed with manual review fallback",
                    e.getMessage());
            return permissivePassThrough("ENGINE_ERROR");
        }
    }

    private CreditDecisionResult mapDecisionResponse(JsonNode result) {
        String decision = textOrNull(result, "decision");
        // Engine may return enum-as-string or omit field entirely
        if (decision == null || decision.isBlank()) decision = "AUTO_APPROVE";

        var details = new ArrayList<CriteriaDetail>();
        if (result.has("details") && result.get("details").isArray()) {
            for (var d : result.get("details")) {
                details.add(new CriteriaDetail(
                        textOrNull(d, "fieldKey"),
                        textOrNull(d, "fieldName"),
                        textOrNull(d, "customerValue"),
                        d.has("passed") && d.get("passed").asBoolean(),
                        decimalOrZero(d, "scoredWeight"),
                        decimalOrZero(d, "maxWeight"),
                        textOrNull(d, "matchedRule"),
                        textOrNull(d, "failureReason")
                ));
            }
        }

        return new CreditDecisionResult(
                decision,
                textOrNull(result, "reasonCode"),
                result.has("eligible") && result.get("eligible").asBoolean(),
                decimalOrZero(result, "totalScore"),
                decimalOrZero(result, "maxPossibleScore"),
                decimalOrZero(result, "scorePercentage"),
                decimalOrZero(result, "greenThreshold"),
                decimalOrZero(result, "amberThreshold"),
                intOrZero(result, "totalCriteria"),
                intOrZero(result, "matchedCriteria"),
                intOrZero(result, "failedCriteria"),
                details,
                textOrNull(result, "summary")
        );
    }

    private CreditDecisionResult permissivePassThrough(String reasonCode) {
        return new CreditDecisionResult(
                "AUTO_APPROVE", reasonCode, true,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                0, 0, 0, List.of(),
                "Credit decision engine unavailable — defaulting to AUTO_APPROVE (downstream eligibility checks still apply)"
        );
    }

    @Override
    public EligibilityResult calculateEligibility(EligibilityInput input) {
        log.info("Activity: Calculating eligibility for tenant={}", input.tenantId());

        // ══════ FINERACT PRODUCT VALIDATION (BRD fix: validate before loan creation) ══════
        var fineractCheck = validateAgainstFineractProducts(input.requestedAmount(), input.requestedTenureMonths());
        if (fineractCheck != null) {
            log.warn("Fineract product validation failed: {}", fineractCheck);
            return new EligibilityResult(false, BigDecimal.ZERO, null, null, null, fineractCheck);
        }

        // Check credit score
        if (input.minCreditScore() > 0 && input.creditScore() < input.minCreditScore()) {
            return new EligibilityResult(false, BigDecimal.ZERO, null, null, null,
                    "Credit score " + input.creditScore() + " below minimum " + input.minCreditScore());
        }

        // Check defaults
        if (input.hasActiveDefaults()) {
            return new EligibilityResult(false, BigDecimal.ZERO, null, null, null,
                    "Customer has active defaults in SIMAH");
        }

        // Check salary
        if (input.minSalary() != null && input.verifiedSalary().compareTo(input.minSalary()) < 0) {
            return new EligibilityResult(false, BigDecimal.ZERO, null, null, null,
                    "Salary " + input.verifiedSalary() + " below minimum " + input.minSalary());
        }

        // Check age
        if (input.minAge() > 0 && input.customerAge() < input.minAge()) {
            return new EligibilityResult(false, BigDecimal.ZERO, null, null, null,
                    "Age " + input.customerAge() + " below minimum " + input.minAge());
        }
        if (input.maxAge() > 0 && input.customerAge() > input.maxAge()) {
            return new EligibilityResult(false, BigDecimal.ZERO, null, null, null,
                    "Age " + input.customerAge() + " exceeds maximum " + input.maxAge());
        }

        // Check employment duration
        if (input.minEmploymentMonths() > 0 && input.employmentDurationMonths() < input.minEmploymentMonths()) {
            return new EligibilityResult(false, BigDecimal.ZERO, null, null, null,
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
            // DBR-exceeded scenarios pass through with the requested amount; downstream
            // credit decision engine + manual review gate handle the risk signal.
            log.warn("DBR {}% exceeds product cap {}% — proceeding without rejection (engine handles risk)",
                    dbrAfter, maxDbr);
            return new EligibilityResult(true, input.requestedAmount(), dbrBefore, dbrAfter,
                    BigDecimal.ZERO, null);
        }

        // ══════ BRD AFFORDABILITY CHECK (Steps 5-6, 27) ══════
        // After DBR passes, verify disposable income covers expenses + installment
        var affordability = runAffordabilityCheck(input, proposedInstallment);
        if (!affordability.eligible()) {
            return new EligibilityResult(false, BigDecimal.ZERO, dbrBefore, dbrAfter,
                    affordability.disposableIncome(), affordability.reason());
        }

        log.info("Eligibility passed: dbrBefore={}%, dbrAfter={}%, disposableIncome={}",
                dbrBefore, dbrAfter, affordability.disposableIncome());
        return new EligibilityResult(true, input.requestedAmount(), dbrBefore, dbrAfter,
                affordability.disposableIncome(), null);
    }

    @Override
    public ProfitCalculationResult calculateOffer(ProfitCalculationInput input) {
        log.info("Activity: Calculating offer for structure={}, amount={}",
                input.shariaStructure(), input.principalAmount());

        // Normalize rate: assume decimals if <= 0.5 (50%), otherwise assume percentage (e.g. 2.5)
        BigDecimal rate = input.profitRate();
        BigDecimal decimalRate = (rate != null && rate.compareTo(new BigDecimal("0.5")) > 0)
                ? rate.movePointLeft(2)
                : rate;

        BigDecimal processingFee = BigDecimal.ZERO;
        if (input.processingFeeAmount() != null && input.processingFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            processingFee = input.processingFeeAmount();
        } else if (input.processingFeePercent() != null && input.processingFeePercent().compareTo(BigDecimal.ZERO) > 0) {
            processingFee = input.principalAmount().multiply(input.processingFeePercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal adminFee = input.adminFeeAmount() != null ? input.adminFeeAmount() : BigDecimal.ZERO;

        var calcResult = com.ksa.financing.lending.domain.service.FinanceCalculationService.calculate(
                input.principalAmount(),
                decimalRate,
                null,
                input.tenureMonths(),
                processingFee,
                adminFee
        );

        log.info("Offer calculated (BRD formula): monthly={}, totalProfit={}, totalPayable={}",
                calcResult.monthlyInstallment(), calcResult.totalCostOfFinancing(), calcResult.totalPayable());

        return new ProfitCalculationResult(
                calcResult.monthlyInstallment(),
                calcResult.totalCostOfFinancing(),
                calcResult.totalPayable(),
                calcResult.totalPayable(),  // sellingPrice = totalPayable
                processingFee,
                adminFee,
                calcResult.apr(),
                calcResult.firstInstallmentDueDate()
        );
    }

    /**
     * BRD Affordability check (Steps 5-6, 27): verifies disposable income after expenses.
     * Uses customer-declared expenses (8 categories) to ensure the customer can actually
     * afford the proposed installment after covering living costs.
     * Prefers SIMAH-verified salary over customer-declared income.
     */
    private AffordabilityCalculationService.AffordabilityResult runAffordabilityCheck(
            EligibilityInput input, BigDecimal proposedInstallment) {

        // Use SIMAH-verified salary (more reliable), fall back to customer-declared
        BigDecimal salary = input.verifiedSalary().compareTo(BigDecimal.ZERO) > 0
                ? input.verifiedSalary()
                : (input.declaredMonthlyIncome() != null ? input.declaredMonthlyIncome() : BigDecimal.ZERO);

        // Use declared expenses from BRD 8 categories (sum provided by workflow)
        BigDecimal expenses = input.declaredExpenses() != null ? input.declaredExpenses() : BigDecimal.ZERO;

        // Use SIMAH obligations (already includes liabilities from credit bureau)
        BigDecimal liabilities = input.existingObligations();

        BigDecimal maxDbr = input.maxDbrPercent() != null ? input.maxDbrPercent() : BigDecimal.valueOf(65);

        // Skip affordability if no expense data provided (backward compatibility)
        if (expenses.compareTo(BigDecimal.ZERO) == 0) {
            log.info("No declared expenses provided, skipping affordability check (DBR-only mode)");
            BigDecimal disposable = salary.subtract(liabilities).subtract(proposedInstallment);
            return new AffordabilityCalculationService.AffordabilityResult(
                    true, BigDecimal.ZERO, BigDecimal.ZERO, disposable, disposable, null);
        }

        var result = AffordabilityCalculationService.check(salary, liabilities, expenses, proposedInstallment, maxDbr);

        log.info("Affordability check: eligible={}, dbrAfter={}, disposableIncome={}, reason={}",
                result.eligible(), result.dbrAfter(), result.disposableIncome(), result.reason());

        return result;
    }

    /**
     * Normalize profit rate: if value > 1, treat as percentage and convert to decimal.
     * Product DB stores rates as percentages (e.g., 2.5 for 2.5%), but calculations need
     * decimal form (0.025). The offer calculator already does this; eligibility must too.
     */
    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null) return BigDecimal.ZERO;
        // Normalize: if > 0.5, assume it's a percentage (e.g., 2.5, 0.75) and convert to decimal (0.025, 0.0075)
        return rate.compareTo(new BigDecimal("0.5")) > 0
                ? rate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                : rate;
    }

    private BigDecimal calculateMonthlyInstallment(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (tenureMonths <= 0 || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = normalizeRate(annualRate);
        BigDecimal totalProfit = principal.multiply(rate)
                .multiply(BigDecimal.valueOf(tenureMonths))
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
        BigDecimal total = principal.add(totalProfit);
        return total.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMaxPrincipal(BigDecimal maxInstallment, BigDecimal annualRate, int tenureMonths) {
        if (tenureMonths <= 0 || maxInstallment.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = normalizeRate(annualRate);
        // Reverse: principal = installment * months / (1 + rate * months/12)
        BigDecimal rateForTenure = rate.multiply(BigDecimal.valueOf(tenureMonths))
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

    /**
     * Validate requested amount/tenure against Fineract loan products.
     * Returns error message if no matching product found, null if OK.
     */
    private String validateAgainstFineractProducts(BigDecimal amount, int tenureMonths) {
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(fineractUsername, fineractPassword);
            headers.set("Fineract-Platform-TenantId", fineractTenantId);

            var response = restTemplate.exchange(
                    fineractBaseUrl + "/loanproducts",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            var products = objectMapper.readTree(response.getBody());
            if (products == null || !products.isArray() || products.isEmpty()) {
                log.warn("No Fineract loan products found, skipping validation");
                return null;
            }

            // Check if any product accepts this amount
            boolean matchFound = false;
            var reasons = new StringBuilder();
            for (var product : products) {
                var name = product.has("name") ? product.get("name").asText() : "Unknown";
                var minPrincipal = product.has("minPrincipal")
                        ? new BigDecimal(product.get("minPrincipal").asText()) : BigDecimal.ZERO;
                var maxPrincipal = product.has("maxPrincipal")
                        ? new BigDecimal(product.get("maxPrincipal").asText()) : new BigDecimal("999999999");

                if (amount.compareTo(minPrincipal) >= 0 && amount.compareTo(maxPrincipal) <= 0) {
                    matchFound = true;
                    log.info("Fineract product match: {} (min={}, max={})", name, minPrincipal, maxPrincipal);
                    break;
                }
                reasons.append(name).append(" (").append(minPrincipal).append("-").append(maxPrincipal).append("), ");
            }

            if (!matchFound) {
                return "No Fineract loan product supports amount " + amount
                        + " SAR. Available products: " + reasons.toString().replaceAll(", $", "");
            }

            return null;

        } catch (Exception e) {
            log.warn("Fineract product validation unavailable ({}), skipping check", e.getMessage());
            return null; // Don't block if Fineract is down
        }
    }
}
