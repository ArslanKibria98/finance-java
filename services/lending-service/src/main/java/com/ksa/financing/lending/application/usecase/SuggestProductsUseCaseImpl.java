package com.ksa.financing.lending.application.usecase;

import com.ksa.financing.lending.domain.port.in.SuggestProductsUseCase;
import com.ksa.financing.lending.domain.port.out.ProductConfigPort;
import com.ksa.financing.lending.domain.service.AffordabilityCalculationService;
import com.ksa.financing.lending.domain.service.FinanceCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.UUID;

/**
 * Financial-first product suggestion use case.
 *
 * <p>Input: customer financial profile only (salary, liabilities from SIMAH, expenses, dependents).
 * No amount, no tenure — system computes MAX eligible amount per product.
 *
 * <p>Flow per product:
 * <ol>
 *   <li>Compute customer's max affordable monthly installment (dual-check: DTI + residual)</li>
 *   <li>Reverse-calc max principal = maxInstallment × tenure / (1 + rate × tenure/12)</li>
 *   <li>Cap principal at product's maxAmount, floor at product's minAmount</li>
 *   <li>If max principal &lt; product minAmount → not eligible</li>
 *   <li>Forward-calc installment preview for the suggested amount</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestProductsUseCaseImpl implements SuggestProductsUseCase {

    private final ProductConfigPort productConfigPort;

    @Override
    public SuggestProductsResult suggestProducts(SuggestProductsCommand cmd) {
        log.info("Suggesting products (financial-first): salary={}, liabilities={}, adults={}, children={}",
                cmd.salary(), cmd.liabilities(), cmd.adultDependents(), cmd.childDependents());

        var totalExpenses = AffordabilityCalculationService.sumExpenses(
                cmd.foodGroceries(), cmd.utilities(), cmd.healthcare(), cmd.communication(),
                cmd.housingRent(), cmd.clothingEssentials(), cmd.education(), cmd.transportation(),
                cmd.adultDependents(), cmd.childDependents()
        );

        var dbrBefore = cmd.liabilities() != null && cmd.salary().compareTo(BigDecimal.ZERO) > 0
                ? cmd.liabilities().divide(cmd.salary(), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        var products = productConfigPort.listActiveProducts(cmd.tenantId(), cmd.authToken());
        log.info("Evaluating {} active products for customer", products.size());

        var eligible = products.stream()
                .map(p -> evaluate(p, cmd, totalExpenses))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(EligibleProduct::amount).reversed())
                .toList();

        log.info("Suggested {} eligible products out of {} evaluated",
                eligible.size(), products.size());

        return new SuggestProductsResult(dbrBefore, totalExpenses, products.size(), eligible);
    }

    private EligibleProduct evaluate(
            ProductConfigPort.ProductConfig product,
            SuggestProductsCommand cmd,
            BigDecimal totalExpenses
    ) {
        if (!product.fineractLinked()) return null;

        if (product.minSalary() != null && cmd.salary().compareTo(product.minSalary()) < 0) return null;

        var tenureMonths = product.maxTenureMonths() > 0
                ? product.maxTenureMonths()
                : 60;

        var capacityCheck = AffordabilityCalculationService.check(
                cmd.salary(),
                cmd.liabilities(),
                totalExpenses,
                BigDecimal.ZERO,
                product.maxDbrPercent()
        );

        var maxAffordableInstallment = capacityCheck.maxAffordableInstalment();
        if (maxAffordableInstallment == null || maxAffordableInstallment.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        var maxPrincipal = FinanceCalculationService.calculateMaxAmountFromInstallment(
                maxAffordableInstallment,
                product.profitRate(),
                tenureMonths
        );

        if (product.maxAmount() != null && maxPrincipal.compareTo(product.maxAmount()) > 0) {
            maxPrincipal = product.maxAmount();
        }

        if (product.minAmount() != null && maxPrincipal.compareTo(product.minAmount()) < 0) {
            return null;
        }

        if (maxPrincipal.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        var finCalc = FinanceCalculationService.calculate(
                maxPrincipal,
                product.profitRate(),
                tenureMonths,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                product.vatPercent(),
                product.isDisbursementInclusive()
        );

        var affordability = AffordabilityCalculationService.check(
                cmd.salary(),
                cmd.liabilities(),
                totalExpenses,
                finCalc.monthlyInstallment(),
                product.maxDbrPercent()
        );

        if (!affordability.eligible()) return null;

        return new EligibleProduct(
                product.productId() != null ? UUID.fromString(product.productId()) : null,
                product.productCode(),
                product.productName(),
                product.shariaStructure(),
                product.shariaStructure(),
                product.profitRate(),
                finCalc.apr(),
                finCalc.requestedAmount(),
                finCalc.tenureMonths(),
                finCalc.numInstallments(),
                finCalc.monthlyInstallment(),
                finCalc.totalPayable(),
                finCalc.costOfTerm(),
                affordability.dbrAfter(),
                affordability.disposableIncome(),
                finCalc.firstInstallmentDueDate(),
                product.minAmount(),
                product.maxAmount(),
                product.minTenureMonths(),
                product.maxTenureMonths()
        );
    }
}
