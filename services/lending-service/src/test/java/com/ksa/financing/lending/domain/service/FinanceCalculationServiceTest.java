package com.ksa.financing.lending.domain.service;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the reducing-balance amortization engine per
 * {@code docs/reducing-balance-load-documentation.md}.
 */
class FinanceCalculationServiceTest {

    private static final BigDecimal PRINCIPAL = new BigDecimal("100000");
    private static final BigDecimal PROFIT_RATE_5_PCT = new BigDecimal("0.05");
    private static final BigDecimal PROC_FEE = new BigDecimal("500");
    private static final BigDecimal ADMIN_FEE = new BigDecimal("200");
    private static final BigDecimal VAT_15 = new BigDecimal("15");

    // Reducing-balance reference values for P=100k, r=5%:
    //   n=12 → EMI=8560.75, profit≈2728.98
    //   n=24 → EMI=4387.14, profit≈5291.34
    private static final Offset<BigDecimal> TOL = Offset.offset(new BigDecimal("1.50"));

    @Test
    @DisplayName("Fees non-inclusive → fees added as separate per-installment component on top of EMI")
    void feesNonInclusive_addedToInstallment() {
        var result = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 24,
                PROC_FEE, ADMIN_FEE, VAT_15, false);

        // Profit comes purely from reducing-balance schedule (NOT inflated by fees)
        assertThat(result.profitBeforeVat()).isCloseTo(new BigDecimal("5291.34"), TOL);
        // VAT on profit = 5291.34 × 0.15 ≈ 793.70
        assertThat(result.vatAmount()).isCloseTo(new BigDecimal("793.70"), TOL);
        // totalPayable = principal + profit + fees = 100000 + 5291.34 + 700 = 105991.34
        assertThat(result.totalPayable()).isCloseTo(new BigDecimal("105991.34"), TOL);
        // EMI = annuity EMI (4387.14) + fee/24 (29.17) ≈ 4416.31
        assertThat(result.monthlyInstallment()).isCloseTo(new BigDecimal("4416.31"), TOL);
        assertThat(result.isDisbursementInclusive()).isFalse();
    }

    @Test
    @DisplayName("Fees inclusive → fees deducted at disbursement, NOT in EMI")
    void feesInclusive_excludedFromInstallment() {
        var result = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 24,
                PROC_FEE, ADMIN_FEE, VAT_15, true);

        assertThat(result.profitBeforeVat()).isCloseTo(new BigDecimal("5291.34"), TOL);
        assertThat(result.totalPayable()).isCloseTo(new BigDecimal("105291.34"), TOL);
        assertThat(result.monthlyInstallment()).isCloseTo(new BigDecimal("4387.14"), TOL);
        assertThat(result.isDisbursementInclusive()).isTrue();
    }

    @Test
    @DisplayName("No fees → totalPayable = principal + reducing-balance profit")
    void noFees() {
        var result = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 24,
                BigDecimal.ZERO, BigDecimal.ZERO, VAT_15, false);

        assertThat(result.profitBeforeVat()).isCloseTo(new BigDecimal("5291.34"), TOL);
        assertThat(result.totalPayable()).isCloseTo(new BigDecimal("105291.34"), TOL);
        assertThat(result.monthlyInstallment()).isCloseTo(new BigDecimal("4387.14"), TOL);
    }

    @Test
    @DisplayName("Very low rate → reducing-balance profit is small but non-zero")
    void lowRate() {
        var result = FinanceCalculationService.calculate(
                PRINCIPAL, new BigDecimal("0.005"), 24,
                PROC_FEE, ADMIN_FEE, VAT_15, false);

        // P=100k, r=0.5%, n=24 reducing balance → profit ≈ 521.67
        assertThat(result.profitBeforeVat()).isCloseTo(new BigDecimal("521.67"), TOL);
        // totalPayable = 100000 + 521.67 + 700 (fees, non-inclusive) ≈ 101221.67
        assertThat(result.totalPayable()).isCloseTo(new BigDecimal("101221.67"), TOL);
    }

    @Test
    @DisplayName("Tenure increases total profit (non-linear under reducing balance)")
    void tenureIncreasesProfit() {
        var twelve = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 12,
                BigDecimal.ZERO, BigDecimal.ZERO, VAT_15, true);
        var twentyFour = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 24,
                BigDecimal.ZERO, BigDecimal.ZERO, VAT_15, true);

        // Reducing-balance profits — NOT linear with tenure (flat rate would have been 5000 vs 10000)
        assertThat(twelve.profitBeforeVat()).isCloseTo(new BigDecimal("2728.98"), TOL);
        assertThat(twentyFour.profitBeforeVat()).isCloseTo(new BigDecimal("5291.34"), TOL);
        // Doubling tenure gives < 2× profit (because principal declines faster initially)
        assertThat(twentyFour.profitBeforeVat()).isLessThan(twelve.profitBeforeVat().multiply(new BigDecimal("2")));
    }

    @Test
    @DisplayName("VAT amount = profit × VAT%, totalPayable identical with/without VAT")
    void vatAppliedToProfit() {
        var withVat = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 24,
                BigDecimal.ZERO, BigDecimal.ZERO, VAT_15, true);
        var withoutVat = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, 24,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true);

        // totalPayable identical (VAT is inside profit split, sums back)
        assertThat(withVat.totalPayable()).isCloseTo(withoutVat.totalPayable(), TOL);
        // VAT = 5291.34 × 0.15 ≈ 793.70
        assertThat(withVat.vatAmount()).isCloseTo(new BigDecimal("793.70"), TOL);
        assertThat(withoutVat.vatAmount()).isCloseTo(BigDecimal.ZERO, TOL);
    }

    @Test
    @DisplayName("Backward-compat wrapper defaults VAT=15 and isDisbursementInclusive=TRUE")
    void backwardCompatWrapper() {
        var result = FinanceCalculationService.calculate(
                PRINCIPAL, PROFIT_RATE_5_PCT, null, 24, PROC_FEE, ADMIN_FEE);

        // Defaults: VAT=15, isDisbursementInclusive=TRUE → fees excluded from schedule
        assertThat(result.profitBeforeVat()).isCloseTo(new BigDecimal("5291.34"), TOL);
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
