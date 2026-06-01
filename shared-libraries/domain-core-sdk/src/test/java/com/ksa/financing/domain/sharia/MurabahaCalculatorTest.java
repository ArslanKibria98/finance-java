package com.ksa.financing.domain.sharia;

import com.ksa.financing.domain.valueobject.ProfitRate;
import com.ksa.financing.domain.valueobject.SarMoney;
import com.ksa.financing.domain.valueobject.Tenure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for MurabahaCalculator.
 * <p>
 * Tests cover:
 * - Basic Murabaha calculation (cost + profit)
 * - Monthly installment calculation
 * - Amortization schedule generation
 * - Different profit rates (5%, 10%, 15%)
 * - Different tenures (12, 24, 36, 60 months)
 * - Zero profit rate
 * - Invalid inputs (null, negative values)
 * - Declining balance method
 * </p>
 */
@DisplayName("MurabahaCalculator Tests")
class MurabahaCalculatorTest {

    private LocalDate startDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.of(2024, 1, 1);
    }

    private static final BigDecimal TOL = new BigDecimal("1.50");

    @Test
    @DisplayName("Should calculate basic Murabaha with 5% profit rate for 12 months (reducing balance)")
    void shouldCalculateBasicMurabaha() {
        // Given
        SarMoney costPrice = SarMoney.of(100000);
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);

        // When
        MurabahaCalculation result = MurabahaCalculator.calculate(costPrice, profitRate, tenure, startDate, SarMoney.zero());

        // Then — reducing-balance: P=100k, r=5%, n=12 → EMI=8560.75, profit=2728.98
        assertThat(result).isNotNull();
        assertThat(result.costPrice()).isEqualTo(costPrice);
        assertThat(result.profitAmount().getValue()).isCloseTo(new BigDecimal("2728.98"), within(TOL));
        assertThat(result.salePrice().getValue()).isCloseTo(new BigDecimal("102728.98"), within(TOL));
        assertThat(result.monthlyInstallment().getValue()).isCloseTo(new BigDecimal("8560.75"), within(TOL));
        assertThat(result.schedule()).hasSize(12);
    }

    @Test
    @DisplayName("Should calculate Murabaha with 10% profit rate for 24 months (reducing balance)")
    void shouldCalculateMurabahaWith10PercentFor24Months() {
        // Given
        SarMoney costPrice = SarMoney.of(50000);
        ProfitRate profitRate = ProfitRate.ofPercentage(10.0);
        Tenure tenure = Tenure.ofMonths(24);

        // When
        MurabahaCalculation result = MurabahaCalculator.calculate(costPrice, profitRate, tenure, startDate, SarMoney.zero());

        // Then — reducing-balance: P=50k, r=10%, n=24 → EMI≈2307.25, profit≈5373.92
        assertThat(result.profitAmount().getValue()).isCloseTo(new BigDecimal("5373.92"), within(TOL));
        assertThat(result.salePrice().getValue()).isCloseTo(new BigDecimal("55373.92"), within(TOL));
        assertThat(result.monthlyInstallment().getValue()).isCloseTo(new BigDecimal("2307.25"), within(TOL));
        assertThat(result.schedule()).hasSize(24);
    }

    @Test
    @DisplayName("Should calculate Murabaha with 15% profit rate for 36 months (reducing balance)")
    void shouldCalculateMurabahaWith15PercentFor36Months() {
        // Given
        SarMoney costPrice = SarMoney.of(200000);
        ProfitRate profitRate = ProfitRate.ofPercentage(15.0);
        Tenure tenure = Tenure.ofMonths(36);

        // When
        MurabahaCalculation result = MurabahaCalculator.calculate(costPrice, profitRate, tenure, startDate, SarMoney.zero());

        // Then — reducing-balance: P=200k, r=15%, n=36 → EMI≈6933.20, profit≈49590.33
        assertThat(result.profitAmount().getValue()).isCloseTo(new BigDecimal("49590.33"), within(TOL));
        assertThat(result.salePrice().getValue()).isCloseTo(new BigDecimal("249590.33"), within(TOL));
        assertThat(result.monthlyInstallment().getValue()).isCloseTo(new BigDecimal("6933.20"), within(TOL));
        assertThat(result.schedule()).hasSize(36);
    }

    @Test
    @DisplayName("Should calculate Murabaha for 60 months tenure (reducing balance)")
    void shouldCalculateMurabahaFor60Months() {
        // Given
        SarMoney costPrice = SarMoney.of(150000);
        ProfitRate profitRate = ProfitRate.ofPercentage(8.0);
        Tenure tenure = Tenure.ofMonths(60);

        // When
        MurabahaCalculation result = MurabahaCalculator.calculate(costPrice, profitRate, tenure, startDate, SarMoney.zero());

        // Then — reducing-balance: P=150k, r=8%, n=60 → EMI≈3041.46, profit≈32487.50
        assertThat(result.profitAmount().getValue()).isCloseTo(new BigDecimal("32487.50"), within(TOL));
        assertThat(result.salePrice().getValue()).isCloseTo(new BigDecimal("182487.50"), within(TOL));
        assertThat(result.monthlyInstallment().getValue()).isCloseTo(new BigDecimal("3041.46"), within(TOL));
        assertThat(result.schedule()).hasSize(60);
    }

    @Test
    @DisplayName("Should generate valid amortization schedule with all installments")
    void shouldGenerateValidAmortizationSchedule() {
        // Given
        SarMoney costPrice = SarMoney.of(100000);
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);

        // When
        MurabahaCalculation result = MurabahaCalculator.calculate(costPrice, profitRate, tenure, startDate, SarMoney.zero());

        // Then
        List<InstallmentLine> schedule = result.schedule();
        assertThat(schedule).hasSize(12);

        // First installment should have opening principal equal to cost price
        InstallmentLine firstInstallment = schedule.get(0);
        assertThat(firstInstallment.installmentNumber()).isEqualTo(1);
        assertThat(firstInstallment.openingPrincipal()).isEqualTo(costPrice);
        assertThat(firstInstallment.dueDate()).isEqualTo(startDate.plusMonths(1));

        // Last installment should have closing principal of zero
        InstallmentLine lastInstallment = schedule.get(11);
        assertThat(lastInstallment.installmentNumber()).isEqualTo(12);
        assertThat(lastInstallment.closingPrincipal()).isEqualTo(SarMoney.zero());
        assertThat(lastInstallment.isFinalInstallment()).isTrue();
    }

    @Test
    @DisplayName("Should calculate monthly installment correctly (reducing balance)")
    void shouldCalculateMonthlyInstallmentCorrectly() {
        // Given
        SarMoney costPrice = SarMoney.of(100000);
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);

        // When
        SarMoney monthlyInstallment = MurabahaCalculator.calculateMonthlyInstallment(costPrice, profitRate, tenure, SarMoney.zero());

        // Then — reducing-balance EMI for P=100k, r=5%, n=12
        assertThat(monthlyInstallment.getValue()).isCloseTo(new BigDecimal("8560.75"), within(TOL));
    }

    @Test
    @DisplayName("Should calculate profit amount over tenure (reducing balance)")
    void shouldCalculateProfitAmountCorrectly() {
        // Given
        SarMoney costPrice = SarMoney.of(100000);
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);

        // When
        SarMoney profitAmount = MurabahaCalculator.calculateProfitAmount(costPrice, profitRate, tenure);

        // Then — reducing-balance total profit for P=100k, r=5%, n=12
        assertThat(profitAmount.getValue()).isCloseTo(new BigDecimal("2728.98"), within(TOL));
    }

    @Test
    @DisplayName("Should calculate sale price over tenure (reducing balance)")
    void shouldCalculateSalePriceCorrectly() {
        // Given
        SarMoney costPrice = SarMoney.of(100000);
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);

        // When
        SarMoney salePrice = MurabahaCalculator.calculateSalePrice(costPrice, profitRate, tenure);

        // Then — cost + reducing-balance total profit
        assertThat(salePrice.getValue()).isCloseTo(new BigDecimal("102728.98"), within(TOL));
    }

    @Test
    @DisplayName("Should handle zero profit rate by throwing exception")
    void shouldHandleZeroProfitRate() {
        // Given
        SarMoney costPrice = SarMoney.of(100000);
        Tenure tenure = Tenure.ofMonths(12);

        // When/Then
        assertThatThrownBy(() -> ProfitRate.ofPercentage(0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 0.01%");
    }

    @Test
    @DisplayName("Should throw exception when cost price is null")
    void shouldThrowExceptionWhenCostPriceIsNull() {
        // Given
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);

        // When/Then
        assertThatThrownBy(() -> MurabahaCalculator.calculate(null, profitRate, tenure, startDate, SarMoney.zero()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Cost price cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when profit rate is null")
    void shouldThrowExceptionWhenProfitRateIsNull() {
        // Given
        SarMoney costPrice = SarMoney.of(100000);
        Tenure tenure = Tenure.ofMonths(12);

        // When/Then
        assertThatThrownBy(() -> MurabahaCalculator.calculate(costPrice, null, tenure, startDate, SarMoney.zero()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Profit rate cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when tenure is null")
    void shouldThrowExceptionWhenTenureIsNull() {
        // Given
        SarMoney costPrice = SarMoney.of(100000);
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);

        // When/Then
        assertThatThrownBy(() -> MurabahaCalculator.calculate(costPrice, profitRate, null, startDate, SarMoney.zero()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Tenure cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when start date is null")
    void shouldThrowExceptionWhenStartDateIsNull() {
        // Given
        SarMoney costPrice = SarMoney.of(100000);
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);

        // When/Then
        assertThatThrownBy(() -> MurabahaCalculator.calculate(costPrice, profitRate, tenure, null, SarMoney.zero()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Start date cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when cost price is negative")
    void shouldThrowExceptionWhenCostPriceIsNegative() {
        // Given
        SarMoney costPrice = SarMoney.of(-100000);
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);

        // When/Then
        assertThatThrownBy(() -> MurabahaCalculator.calculate(costPrice, profitRate, tenure, startDate, SarMoney.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cost price must be positive");
    }

    @Test
    @DisplayName("Should throw exception when cost price is zero")
    void shouldThrowExceptionWhenCostPriceIsZero() {
        // Given
        SarMoney costPrice = SarMoney.zero();
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);

        // When/Then
        assertThatThrownBy(() -> MurabahaCalculator.calculate(costPrice, profitRate, tenure, startDate, SarMoney.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cost price must be positive");
    }

    @Test
    @DisplayName("Should calculate declining balance Murabaha")
    void shouldCalculateDecliningBalanceMurabaha() {
        // Given
        SarMoney costPrice = SarMoney.of(100000);
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);

        // When
        MurabahaCalculation result = MurabahaCalculator.calculateDecliningBalance(costPrice, profitRate, tenure, startDate, SarMoney.zero());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.costPrice()).isEqualTo(costPrice);
        assertThat(result.schedule()).hasSize(12);

        // Verify profit decreases over time in declining balance
        List<InstallmentLine> schedule = result.schedule();
        SarMoney firstProfit = schedule.get(0).profitComponent();
        SarMoney lastProfit = schedule.get(11).profitComponent();
        assertThat(firstProfit.isGreaterThan(lastProfit)).isTrue();
    }

    @Test
    @DisplayName("Should validate parameters for Murabaha")
    void shouldValidateParametersForMurabaha() {
        // Given
        SarMoney validCostPrice = SarMoney.of(100000);
        ProfitRate validProfitRate = ProfitRate.ofPercentage(5.0);
        Tenure validTenure = Tenure.ofMonths(12);

        // When/Then
        assertThat(MurabahaCalculator.isValidForMurabaha(validCostPrice, validProfitRate, validTenure))
                .isTrue();

        assertThat(MurabahaCalculator.isValidForMurabaha(SarMoney.zero(), validProfitRate, validTenure))
                .isFalse();

        assertThat(MurabahaCalculator.isValidForMurabaha(null, validProfitRate, validTenure))
                .isFalse();
    }

    @Test
    @DisplayName("Should calculate effective annual rate")
    void shouldCalculateEffectiveAnnualRate() {
        // Given
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);

        // When
        double effectiveRate = MurabahaCalculator.calculateEffectiveAnnualRate(profitRate);

        // Then
        assertThat(effectiveRate).isEqualTo(0.05);
    }

    @Test
    @DisplayName("Should calculate monthly payment ratio")
    void shouldCalculateMonthlyPaymentRatio() {
        // Given
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);

        // When
        double ratio = MurabahaCalculator.calculateMonthlyPaymentRatio(profitRate, tenure);

        // Then
        assertThat(ratio).isEqualTo(1.05 / 12);
    }

    @Test
    @DisplayName("Should calculate Murabaha with very small amount (reducing balance)")
    void shouldCalculateMurabahaWithSmallAmount() {
        // Given
        SarMoney costPrice = SarMoney.of(1000);
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);

        // When
        MurabahaCalculation result = MurabahaCalculator.calculate(costPrice, profitRate, tenure, startDate, SarMoney.zero());

        // Then — reducing-balance: P=1k, r=5%, n=12
        assertThat(result.profitAmount().getValue()).isCloseTo(new BigDecimal("27.30"), within(TOL));
        assertThat(result.salePrice().getValue()).isCloseTo(new BigDecimal("1027.30"), within(TOL));
        assertThat(result.monthlyInstallment().getValue()).isCloseTo(new BigDecimal("85.61"), within(TOL));
    }

    @Test
    @DisplayName("Should calculate Murabaha with large amount (reducing balance)")
    void shouldCalculateMurabahaWithLargeAmount() {
        // Given
        SarMoney costPrice = SarMoney.of(1000000);
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);

        // When
        MurabahaCalculation result = MurabahaCalculator.calculate(costPrice, profitRate, tenure, startDate, SarMoney.zero());

        // Then — reducing-balance: P=1M, r=5%, n=12
        assertThat(result.profitAmount().getValue()).isCloseTo(new BigDecimal("27289.79"), within(TOL));
        assertThat(result.salePrice().getValue()).isCloseTo(new BigDecimal("1027289.79"), within(TOL));
        assertThat(result.monthlyInstallment().getValue()).isCloseTo(new BigDecimal("85607.48"), within(TOL));
    }

    @Test
    @DisplayName("Should verify constructor throws UnsupportedOperationException")
    void shouldVerifyConstructorThrowsException() {
        // When/Then
        assertThatThrownBy(() -> {
            var constructor = MurabahaCalculator.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        })
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .hasStackTraceContaining("Utility class cannot be instantiated");
    }
}
