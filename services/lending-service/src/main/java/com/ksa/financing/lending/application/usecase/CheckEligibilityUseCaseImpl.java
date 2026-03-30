package com.ksa.financing.lending.application.usecase;

import com.ksa.financing.lending.domain.port.in.CheckEligibilityUseCase;
import com.ksa.financing.lending.domain.port.out.ProductConfigPort;
import com.ksa.financing.lending.domain.service.AffordabilityCalculationService;
import com.ksa.financing.lending.domain.service.FinanceCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * BRD Steps 4-10: "Get Initial Offer" → "Congratulations!" flow.
 * Stateless pre-qualification — no DB, no workflow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckEligibilityUseCaseImpl implements CheckEligibilityUseCase {

    private final ProductConfigPort productConfigPort;

    @Override
    public EligibilityCheckResult checkEligibility(CheckEligibilityCommand cmd) {
        log.info("Checking eligibility: amount={}, salary={}, tenure={}",
                cmd.amount(), cmd.salary(), cmd.tenureMonths());

        // Fetch product config for rates and thresholds
        var config = productConfigPort.fetchProductConfig(
                cmd.tenantId(), cmd.productId(), cmd.amount(), cmd.tenureMonths());

        // Validate against product limits — return rejection with reasons
        var errors = validateProductLimits(cmd.amount(), cmd.tenureMonths(), cmd.salary(), config);
        if (!errors.isEmpty()) {
            var reason = String.join("; ", errors);
            return new EligibilityCheckResult(
                    false, null, cmd.tenureMonths(), 0, null, null,
                    null, null, null, null, null, reason
            );
        }

        // Calculate finance details using BRD formula
        var finCalc = FinanceCalculationService.calculate(
                cmd.amount(),
                config.profitRate(),
                config.costOfTermPercent(),
                cmd.tenureMonths(),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        // Sum expenses
        var totalExpenses = AffordabilityCalculationService.sumExpenses(
                cmd.foodGroceries(), cmd.utilities(), cmd.healthcare(), cmd.communication(),
                cmd.housingRent(), cmd.clothingEssentials(), cmd.education(), cmd.transportation()
        );

        // Check affordability (DBR)
        var affordability = AffordabilityCalculationService.check(
                cmd.salary(),
                cmd.liabilities(),
                totalExpenses,
                finCalc.monthlyInstallment(),
                config.maxDbrPercent()
        );

        // If not eligible due to DBR, calculate max eligible amount
        BigDecimal maxEligible = cmd.amount();
        if (!affordability.eligible() && cmd.salary().compareTo(BigDecimal.ZERO) > 0) {
            maxEligible = FinanceCalculationService.calculateMaxEligibleAmount(
                    cmd.salary(),
                    cmd.liabilities() != null ? cmd.liabilities() : BigDecimal.ZERO,
                    config.maxDbrPercent(),
                    config.profitRate(),
                    cmd.tenureMonths()
            );

            // If max eligible > 0, recalculate with reduced amount
            if (maxEligible.compareTo(BigDecimal.ZERO) > 0 && maxEligible.compareTo(cmd.amount()) < 0) {
                var reducedCalc = FinanceCalculationService.calculate(
                        maxEligible, config.profitRate(), config.costOfTermPercent(),
                        cmd.tenureMonths(), BigDecimal.ZERO, BigDecimal.ZERO
                );

                return new EligibilityCheckResult(
                        true,
                        reducedCalc.monthlyInstallment(),
                        reducedCalc.tenureMonths(),
                        reducedCalc.numInstallments(),
                        reducedCalc.totalPayable(),
                        reducedCalc.costOfTerm(),
                        affordability.dbrBefore(),
                        affordability.dbrAfter(),
                        affordability.disposableIncome(),
                        maxEligible,
                        reducedCalc.firstInstallmentDueDate(),
                        "Eligible for reduced amount: " + maxEligible + " SAR"
                );
            }
        }

        return new EligibilityCheckResult(
                affordability.eligible(),
                finCalc.monthlyInstallment(),
                finCalc.tenureMonths(),
                finCalc.numInstallments(),
                finCalc.totalPayable(),
                finCalc.costOfTerm(),
                affordability.dbrBefore(),
                affordability.dbrAfter(),
                affordability.disposableIncome(),
                maxEligible,
                finCalc.firstInstallmentDueDate(),
                affordability.reason()
        );
    }

    private java.util.List<String> validateProductLimits(BigDecimal amount, int tenureMonths,
                                                          BigDecimal salary, ProductConfigPort.ProductConfig config) {
        var errors = new ArrayList<String>();

        if (!config.fineractLinked()) {
            errors.add("Product '" + config.productName() + "' is not linked to any Fineract loan product. Please configure the product in Fineract first.");
            return errors;
        }

        if (config.minAmount() != null && amount.compareTo(config.minAmount()) < 0) {
            errors.add("Amount " + amount + " SAR is below minimum " + config.minAmount() + " SAR for this product");
        }
        if (config.maxAmount() != null && amount.compareTo(config.maxAmount()) > 0) {
            errors.add("Amount " + amount + " SAR exceeds maximum " + config.maxAmount() + " SAR for this product");
        }
        if (config.minTenureMonths() > 0 && tenureMonths < config.minTenureMonths()) {
            errors.add("Tenure " + tenureMonths + " months is below minimum " + config.minTenureMonths() + " months");
        }
        if (config.maxTenureMonths() > 0 && tenureMonths > config.maxTenureMonths()) {
            errors.add("Tenure " + tenureMonths + " months exceeds maximum " + config.maxTenureMonths() + " months");
        }
        if (config.minSalary() != null && salary != null && salary.compareTo(config.minSalary()) < 0) {
            errors.add("Salary " + salary + " SAR is below minimum " + config.minSalary() + " SAR required for this product");
        }

        return errors;
    }
}
