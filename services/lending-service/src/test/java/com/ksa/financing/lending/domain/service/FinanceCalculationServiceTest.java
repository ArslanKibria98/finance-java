package com.ksa.financing.lending.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinanceCalculationServiceTest {

    private static final BigDecimal PRINCIPAL = new BigDecimal("100000");
    private static final BigDecimal PROFIT_RATE_5_PCT = new BigDecimal("0.05");
    private static final BigDecimal PROC_FEE = new BigDecimal("500");
    private static final BigDecimal ADMIN_FEE = new BigDecimal("200");
    private static final BigDecimal VAT_15 = new BigDecimal("15");

    @Test
    @DisplayName("CASE 2B: fees + inclusive=FALSE → fees added to profit")
    void case_2B_feesAddedToRepayment() {
        var result = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 24,
                PROC_FEE, ADMIN_FEE, VAT_15, false);

        // profitFromPercentage = 100000 × 0.05 × (24/12) = 10,000
        // totalProfitBeforeVat = 10,000 + 500 + 200 = 10,700
        // vatOnProfit = 10,700 × 0.15 = 1,605
        // totalPayable = 100,000 + (10,700 − 1,605) + 1,605 = 110,700
        // EMI = 110,700 / 24 = 4,612.50

        assertThat(result.profitBeforeVat()).isEqualByComparingTo("10700.00");
        assertThat(result.vatAmount()).isEqualByComparingTo("1605.00");
        assertThat(result.profitAfterVat()).isEqualByComparingTo("9095.00");
        assertThat(result.totalPayable()).isEqualByComparingTo("110700.00");
        assertThat(result.monthlyInstallment()).isEqualByComparingTo("4612.50");
        assertThat(result.isDisbursementInclusive()).isFalse();
    }

    @Test
    @DisplayName("CASE 2A: fees + inclusive=TRUE → fees NOT added (already deducted Day-0)")
    void case_2A_feesDeductedAtDisbursement() {
        var result = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 24,
                PROC_FEE, ADMIN_FEE, VAT_15, true);

        // totalProfitBeforeVat = 10,000 (fees excluded)
        // totalPayable = 100,000 + 10,000 = 110,000
        // EMI = 110,000 / 24 = 4,583.33

        assertThat(result.profitBeforeVat()).isEqualByComparingTo("10000.00");
        assertThat(result.totalPayable()).isEqualByComparingTo("110000.00");
        assertThat(result.monthlyInstallment()).isEqualByComparingTo("4583.33");
        assertThat(result.isDisbursementInclusive()).isTrue();
    }

    @Test
    @DisplayName("CASE 2C: no fees → totalProfit = profitFromPercentage only")
    void case_2C_noFees() {
        var result = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 24,
                BigDecimal.ZERO, BigDecimal.ZERO, VAT_15, false);

        assertThat(result.profitBeforeVat()).isEqualByComparingTo("10000.00");
        assertThat(result.totalPayable()).isEqualByComparingTo("110000.00");
        assertThat(result.monthlyInstallment()).isEqualByComparingTo("4583.33");
    }

    @Test
    @DisplayName("CASE 1: profitRate < 1% → totalProfit = fees only")
    void case_1_zeroOrSubOnePercentProfit() {
        var result = FinanceCalculationService.calculate(
                PRINCIPAL, new BigDecimal("0.005"), 24,
                PROC_FEE, ADMIN_FEE, VAT_15, false);

        // CASE 1: totalProfitBeforeVat = 500 + 200 = 700
        // totalPayable = 100,000 + 700 = 100,700
        assertThat(result.profitBeforeVat()).isEqualByComparingTo("700.00");
        assertThat(result.totalPayable()).isEqualByComparingTo("100700.00");
        assertThat(result.monthlyInstallment()).isEqualByComparingTo("4195.83");
    }

    @Test
    @DisplayName("Annual rate prorating: 12 months vs 24 months should scale profit linearly")
    void annualRateProrating() {
        var twelve = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 12,
                BigDecimal.ZERO, BigDecimal.ZERO, VAT_15, true);
        var twentyFour = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 24,
                BigDecimal.ZERO, BigDecimal.ZERO, VAT_15, true);

        assertThat(twelve.profitBeforeVat()).isEqualByComparingTo("5000.00");
        assertThat(twentyFour.profitBeforeVat()).isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("VAT cancellation in totalPayable: Step 4 (−vat) + Step 5 (+vat) is net-zero")
    void vatNetCancellationInTotalPayable() {
        var withVat = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 24,
                BigDecimal.ZERO, BigDecimal.ZERO, VAT_15, true);
        var withoutVat = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 24,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true);

        // totalPayable should be identical regardless of VAT — formula self-cancels VAT
        assertThat(withVat.totalPayable()).isEqualByComparingTo(withoutVat.totalPayable());
        // But vatAmount differs
        assertThat(withVat.vatAmount()).isEqualByComparingTo("1500.00");
        assertThat(withoutVat.vatAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Backward-compat wrapper defaults VAT=15 and isDisbursementInclusive=TRUE")
    void backwardCompatWrapper() {
        var result = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, null, 24, PROC_FEE, ADMIN_FEE);

        // Defaults: VAT=15, isDisbursementInclusive=TRUE → CASE 2A
        assertThat(result.profitBeforeVat()).isEqualByComparingTo("10000.00");
        assertThat(result.isDisbursementInclusive()).isTrue();
        assertThat(result.vatPercentage()).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("Validation: negative principal rejected")
    void validation_negativePrincipal() {
        assertThatThrownBy(() -> FinanceCalculationService.calculate(
                BigDecimal.ZERO, PROFIT_RATE_5_PCT, 24, BigDecimal.ZERO, BigDecimal.ZERO, VAT_15, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Principal");
    }

    @Test
    @DisplayName("Validation: zero tenure rejected")
    void validation_zeroTenure() {
        assertThatThrownBy(() -> FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 0, BigDecimal.ZERO, BigDecimal.ZERO, VAT_15, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenure");
    }
}
