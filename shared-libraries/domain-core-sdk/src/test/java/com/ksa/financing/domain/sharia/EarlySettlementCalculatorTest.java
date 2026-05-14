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
 * Comprehensive unit tests for EarlySettlementCalculator (IbraCalculator).
 * <p>
 * Tests cover:
 * - Full Ibra (100% waiver of unearned profit)
 * - Partial Ibra (50% waiver)
 * - No Ibra (customer pays all profit)
 * - Ibra calculation at different time points (25%, 50%, 75% of tenure)
 * - Settlement after 6 months of 12-month loan
 * - Settlement on day 1 (maximum Ibra)
 * - Settlement on last day (no unearned profit)
 * - Earned vs unearned profit calculation
 * - Customer savings calculation
 * - Different profit rates and tenures
 * </p>
 */
@DisplayName("EarlySettlementCalculator Tests")
class EarlySettlementCalculatorTest {

    private SarMoney principal;
    private SarMoney totalProfit;
    private List<InstallmentLine> schedule;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;

    @BeforeEach
    void setUp() {
        principal = SarMoney.of(100000);
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);
        contractStartDate = LocalDate.of(2024, 1, 1);

        // Generate Murabaha calculation for test data
        MurabahaCalculation murabaha = MurabahaCalculator.calculate(principal, profitRate, tenure, contractStartDate, SarMoney.zero());
        totalProfit = murabaha.profitAmount();
        schedule = murabaha.schedule();
        contractEndDate = contractStartDate.plusMonths(12);
    }

    @Test
    @DisplayName("Should calculate full Ibra (100% waiver) after 6 months")
    void shouldCalculateFullIbraAfter6Months() {
        // Given
        LocalDate settlementDate = contractStartDate.plusMonths(6);

        // When
        IbraCalculation result = EarlySettlementCalculator.calculateWithFullIbra(
                principal, totalProfit, schedule, contractStartDate, settlementDate
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.settlementDate()).isEqualTo(settlementDate);

        // After 6 months, 50% profit earned, 50% unearned
        assertThat(result.earnedProfit().getValue()).isCloseTo(
                new BigDecimal("2500.00"), within(new BigDecimal("1.00"))
        );

        // Full Ibra means 100% of unearned profit waived
        assertThat(result.waivedProfit().getValue()).isCloseTo(
                new BigDecimal("2500.00"), within(new BigDecimal("1.00"))
        );

        assertThat(result.hasIbra()).isTrue();
    }

    @Test
    @DisplayName("Should calculate partial Ibra (50% waiver) after 6 months")
    void shouldCalculatePartialIbraAfter6Months() {
        // Given
        LocalDate settlementDate = contractStartDate.plusMonths(6);
        BigDecimal ibraPercentage = new BigDecimal("0.50"); // 50% waiver

        // When
        IbraCalculation result = EarlySettlementCalculator.calculate(
                principal, totalProfit, schedule, contractStartDate, settlementDate, ibraPercentage
        );

        // Then
        assertThat(result).isNotNull();

        // With 50% waiver, half of unearned profit is waived
        assertThat(result.waivedProfit().getValue()).isCloseTo(
                new BigDecimal("1250.00"), within(new BigDecimal("1.00"))
        );

        assertThat(result.hasIbra()).isTrue();
    }

    @Test
    @DisplayName("Should calculate no Ibra (0% waiver) - customer pays all profit")
    void shouldCalculateNoIbra() {
        // Given
        LocalDate settlementDate = contractStartDate.plusMonths(6);

        // When
        IbraCalculation result = EarlySettlementCalculator.calculateWithoutIbra(
                principal, totalProfit, schedule, contractStartDate, settlementDate
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.waivedProfit()).isEqualTo(SarMoney.zero());
        assertThat(result.hasIbra()).isFalse();

        // Settlement amount equals outstanding principal + earned profit (no waiver)
        SarMoney expectedSettlement = result.outstandingPrincipal().add(result.earnedProfit());
        assertThat(result.settlementAmount()).isEqualTo(expectedSettlement);
    }

    @Test
    @DisplayName("Should calculate Ibra at 25% of tenure (3 months)")
    void shouldCalculateIbraAt25PercentOfTenure() {
        // Given
        LocalDate settlementDate = contractStartDate.plusMonths(3);

        // When
        IbraCalculation result = EarlySettlementCalculator.calculateWithFullIbra(
                principal, totalProfit, schedule, contractStartDate, settlementDate
        );

        // Then
        assertThat(result).isNotNull();

        // After 3 months, 25% profit earned, 75% unearned
        assertThat(result.earnedProfit().getValue()).isCloseTo(
                new BigDecimal("1250.00"), within(new BigDecimal("1.00"))
        );

        // Full Ibra means 75% of total profit waived
        assertThat(result.waivedProfit().getValue()).isCloseTo(
                new BigDecimal("3750.00"), within(new BigDecimal("1.00"))
        );
    }

    @Test
    @DisplayName("Should calculate Ibra at 75% of tenure (9 months)")
    void shouldCalculateIbraAt75PercentOfTenure() {
        // Given
        LocalDate settlementDate = contractStartDate.plusMonths(9);

        // When
        IbraCalculation result = EarlySettlementCalculator.calculateWithFullIbra(
                principal, totalProfit, schedule, contractStartDate, settlementDate
        );

        // Then
        assertThat(result).isNotNull();

        // After 9 months, 75% profit earned, 25% unearned
        assertThat(result.earnedProfit().getValue()).isCloseTo(
                new BigDecimal("3750.00"), within(new BigDecimal("1.00"))
        );

        // Full Ibra means 25% of total profit waived
        assertThat(result.waivedProfit().getValue()).isCloseTo(
                new BigDecimal("1250.00"), within(new BigDecimal("1.00"))
        );
    }

    @Test
    @DisplayName("Should calculate settlement on day 1 (maximum Ibra benefit)")
    void shouldCalculateSettlementOnDay1() {
        // Given
        LocalDate settlementDate = contractStartDate.plusDays(1);

        // When
        IbraCalculation result = EarlySettlementCalculator.calculateWithFullIbra(
                principal, totalProfit, schedule, contractStartDate, settlementDate
        );

        // Then
        assertThat(result).isNotNull();

        // Almost all profit is unearned on day 1
        assertThat(result.earnedProfit().getValue()).isLessThan(new BigDecimal("50.00"));

        // Almost all profit should be waived with full Ibra
        assertThat(result.waivedProfit().getValue()).isGreaterThan(new BigDecimal("4950.00"));

        // Customer saves almost the entire profit amount
        assertThat(result.getCustomerSavings()).isEqualTo(result.waivedProfit());
    }

    @Test
    @DisplayName("Should calculate settlement on contract end date (no unearned profit)")
    void shouldCalculateSettlementOnLastDay() {
        // Given
        LocalDate settlementDate = contractEndDate;

        // When
        IbraCalculation result = EarlySettlementCalculator.calculateWithFullIbra(
                principal, totalProfit, schedule, contractStartDate, settlementDate
        );

        // Then
        assertThat(result).isNotNull();

        // All profit is earned at contract end
        assertThat(result.earnedProfit()).isEqualTo(totalProfit);

        // No unearned profit to waive
        assertThat(result.waivedProfit()).isEqualTo(SarMoney.zero());
        assertThat(result.hasIbra()).isFalse();
    }

    @Test
    @DisplayName("Should calculate earned profit correctly based on time elapsed")
    void shouldCalculateEarnedProfitBasedOnTimeElapsed() {
        // Given
        LocalDate settlementDate = contractStartDate.plusMonths(6);

        // When
        SarMoney earnedProfit = EarlySettlementCalculator.calculateEarnedProfit(
                totalProfit, contractStartDate, contractEndDate, settlementDate
        );

        // Then - After 6 months of 12-month contract, 50% of profit is earned
        assertThat(earnedProfit.getValue()).isCloseTo(
                new BigDecimal("2500.00"), within(new BigDecimal("1.00"))
        );
    }

    @Test
    @DisplayName("Should calculate unearned profit correctly")
    void shouldCalculateUnearnedProfitCorrectly() {
        // Given
        SarMoney earnedProfit = SarMoney.of(2500.00);

        // When
        SarMoney unearnedProfit = EarlySettlementCalculator.calculateUnearnedProfit(totalProfit, earnedProfit);

        // Then
        assertThat(unearnedProfit).isEqualTo(SarMoney.of(2500.00));
    }

    @Test
    @DisplayName("Should calculate customer savings from Ibra")
    void shouldCalculateCustomerSavings() {
        // Given
        SarMoney unearnedProfit = SarMoney.of(2500.00);
        BigDecimal ibraPercentage = new BigDecimal("0.80"); // 80% waiver

        // When
        SarMoney savings = EarlySettlementCalculator.calculateCustomerSavings(unearnedProfit, ibraPercentage);

        // Then
        assertThat(savings).isEqualTo(SarMoney.of(2000.00));
    }

    @Test
    @DisplayName("Should calculate outstanding principal at settlement date")
    void shouldCalculateOutstandingPrincipalAtSettlementDate() {
        // Given
        LocalDate settlementDate = contractStartDate.plusMonths(6);

        // When
        SarMoney outstandingPrincipal = EarlySettlementCalculator.calculateOutstandingPrincipal(
                schedule, settlementDate
        );

        // Then - After 6 payments, approximately 50% of principal paid
        assertThat(outstandingPrincipal.getValue()).isCloseTo(
                new BigDecimal("50000.00"), within(new BigDecimal("1000.00"))
        );
    }

    @Test
    @DisplayName("Should calculate settlement with different profit rate (10%)")
    void shouldCalculateSettlementWithDifferentProfitRate() {
        // Given
        ProfitRate higherRate = ProfitRate.ofPercentage(10.0);
        Tenure tenure = Tenure.ofMonths(12);
        MurabahaCalculation murabaha = MurabahaCalculator.calculate(principal, higherRate, tenure, contractStartDate, SarMoney.zero());
        LocalDate settlementDate = contractStartDate.plusMonths(6);

        // When
        IbraCalculation result = EarlySettlementCalculator.calculateWithFullIbra(
                principal, murabaha.profitAmount(), murabaha.schedule(), contractStartDate, settlementDate
        );

        // Then
        assertThat(result).isNotNull();

        // With 10% rate, total profit is SAR 10,000
        // After 6 months, earned profit ~SAR 5,000, unearned ~SAR 5,000
        assertThat(result.earnedProfit().getValue()).isCloseTo(
                new BigDecimal("5000.00"), within(new BigDecimal("1.00"))
        );

        assertThat(result.waivedProfit().getValue()).isCloseTo(
                new BigDecimal("5000.00"), within(new BigDecimal("1.00"))
        );
    }

    @Test
    @DisplayName("Should calculate settlement with different tenure (24 months)")
    void shouldCalculateSettlementWithDifferentTenure() {
        // Given
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(24);
        MurabahaCalculation murabaha = MurabahaCalculator.calculate(principal, profitRate, tenure, contractStartDate, SarMoney.zero());
        LocalDate settlementDate = contractStartDate.plusMonths(12);

        // When
        IbraCalculation result = EarlySettlementCalculator.calculateWithFullIbra(
                principal, murabaha.profitAmount(), murabaha.schedule(), contractStartDate, settlementDate
        );

        // Then
        assertThat(result).isNotNull();

        // After 12 months of 24-month contract, 50% of profit earned
        assertThat(result.earnedProfit().getValue()).isCloseTo(
                new BigDecimal("2500.00"), within(new BigDecimal("1.00"))
        );
    }

    @Test
    @DisplayName("Should calculate from MurabahaCalculation directly")
    void shouldCalculateFromMurabahaCalculation() {
        // Given
        ProfitRate profitRate = ProfitRate.ofPercentage(5.0);
        Tenure tenure = Tenure.ofMonths(12);
        MurabahaCalculation murabaha = MurabahaCalculator.calculate(principal, profitRate, tenure, contractStartDate, SarMoney.zero());
        LocalDate settlementDate = contractStartDate.plusMonths(6);

        // When
        IbraCalculation result = EarlySettlementCalculator.calculateFromMurabaha(
                murabaha, contractStartDate, settlementDate, BigDecimal.ONE
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasIbra()).isTrue();
    }

    @Test
    @DisplayName("Should get recommended Ibra percentage at different time points")
    void shouldGetRecommendedIbraPercentage() {
        // Given/When/Then
        assertThat(EarlySettlementCalculator.getRecommendedIbraPercentage(new BigDecimal("0.10")))
                .isEqualByComparingTo(BigDecimal.ONE); // < 25% = 100% waiver

        assertThat(EarlySettlementCalculator.getRecommendedIbraPercentage(new BigDecimal("0.40")))
                .isEqualByComparingTo(new BigDecimal("0.80")); // 25-50% = 80% waiver

        assertThat(EarlySettlementCalculator.getRecommendedIbraPercentage(new BigDecimal("0.60")))
                .isEqualByComparingTo(new BigDecimal("0.60")); // 50-75% = 60% waiver

        assertThat(EarlySettlementCalculator.getRecommendedIbraPercentage(new BigDecimal("0.80")))
                .isEqualByComparingTo(new BigDecimal("0.40")); // > 75% = 40% waiver
    }

    @Test
    @DisplayName("Should validate settlement date is valid")
    void shouldValidateSettlementDateIsValid() {
        // Given
        LocalDate validDate = contractStartDate.plusMonths(6);
        LocalDate beforeStart = contractStartDate.minusDays(1);
        LocalDate afterEnd = contractEndDate.plusDays(1);

        // When/Then
        assertThat(EarlySettlementCalculator.isValidSettlementDate(validDate, contractStartDate, contractEndDate))
                .isTrue();

        assertThat(EarlySettlementCalculator.isValidSettlementDate(beforeStart, contractStartDate, contractEndDate))
                .isFalse();

        assertThat(EarlySettlementCalculator.isValidSettlementDate(afterEnd, contractStartDate, contractEndDate))
                .isFalse();
    }

    @Test
    @DisplayName("Should throw exception when settlement date is before contract start")
    void shouldThrowExceptionWhenSettlementDateIsBeforeStart() {
        // Given
        LocalDate invalidDate = contractStartDate.minusDays(1);

        // When/Then
        assertThatThrownBy(() -> EarlySettlementCalculator.calculateWithFullIbra(
                principal, totalProfit, schedule, contractStartDate, invalidDate
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Settlement date cannot be before contract start date");
    }

    @Test
    @DisplayName("Should throw exception when settlement date is after contract end")
    void shouldThrowExceptionWhenSettlementDateIsAfterEnd() {
        // Given
        LocalDate invalidDate = contractEndDate.plusDays(1);

        // When/Then
        assertThatThrownBy(() -> EarlySettlementCalculator.calculateWithFullIbra(
                principal, totalProfit, schedule, contractStartDate, invalidDate
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Settlement date is after contract end date");
    }

    @Test
    @DisplayName("Should throw exception when principal is null")
    void shouldThrowExceptionWhenPrincipalIsNull() {
        // Given
        LocalDate settlementDate = contractStartDate.plusMonths(6);

        // When/Then
        assertThatThrownBy(() -> EarlySettlementCalculator.calculateWithFullIbra(
                null, totalProfit, schedule, contractStartDate, settlementDate
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Principal cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when schedule is empty")
    void shouldThrowExceptionWhenScheduleIsEmpty() {
        // Given
        LocalDate settlementDate = contractStartDate.plusMonths(6);
        List<InstallmentLine> emptySchedule = List.of();

        // When/Then
        assertThatThrownBy(() -> EarlySettlementCalculator.calculateWithFullIbra(
                principal, totalProfit, emptySchedule, contractStartDate, settlementDate
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Schedule cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception when Ibra percentage is invalid")
    void shouldThrowExceptionWhenIbraPercentageIsInvalid() {
        // Given
        LocalDate settlementDate = contractStartDate.plusMonths(6);
        BigDecimal invalidPercentage = new BigDecimal("1.5"); // > 1.0

        // When/Then
        assertThatThrownBy(() -> EarlySettlementCalculator.calculate(
                principal, totalProfit, schedule, contractStartDate, settlementDate, invalidPercentage
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ibra percentage must be between 0 and 1");
    }

    @Test
    @DisplayName("Should calculate settlement amount correctly with full Ibra")
    void shouldCalculateSettlementAmountCorrectlyWithFullIbra() {
        // Given
        LocalDate settlementDate = contractStartDate.plusMonths(6);

        // When
        IbraCalculation result = EarlySettlementCalculator.calculateWithFullIbra(
                principal, totalProfit, schedule, contractStartDate, settlementDate
        );

        // Then
        // Settlement = Outstanding Principal + Earned Profit - Waived Profit
        SarMoney expectedSettlement = result.outstandingPrincipal()
                .add(result.earnedProfit())
                .subtract(result.waivedProfit());

        assertThat(result.settlementAmount()).isEqualTo(expectedSettlement);
    }

    @Test
    @DisplayName("Should provide settlement breakdown for customer communication")
    void shouldProvideSettlementBreakdown() {
        // Given
        LocalDate settlementDate = contractStartDate.plusMonths(6);

        // When
        IbraCalculation result = EarlySettlementCalculator.calculateWithFullIbra(
                principal, totalProfit, schedule, contractStartDate, settlementDate
        );

        String breakdown = result.getSettlementBreakdown();

        // Then
        assertThat(breakdown).isNotEmpty();
        assertThat(breakdown).contains("Early Settlement Breakdown");
        assertThat(breakdown).contains("Outstanding Principal");
        assertThat(breakdown).contains("Earned Profit");
        assertThat(breakdown).contains("Ibra (Rebate)");
        assertThat(breakdown).contains("Settlement Amount");
    }

    @Test
    @DisplayName("Should calculate effective discount rate")
    void shouldCalculateEffectiveDiscountRate() {
        // Given
        LocalDate settlementDate = contractStartDate.plusMonths(6);

        // When
        IbraCalculation result = EarlySettlementCalculator.calculateWithFullIbra(
                principal, totalProfit, schedule, contractStartDate, settlementDate
        );

        // Then
        double discountRate = result.getEffectiveDiscountRate();
        assertThat(discountRate).isGreaterThan(0.0);
        assertThat(discountRate).isLessThanOrEqualTo(1.0);
    }

    @Test
    @DisplayName("Should verify constructor throws UnsupportedOperationException")
    void shouldVerifyConstructorThrowsException() {
        // When/Then
        assertThatThrownBy(() -> {
            var constructor = EarlySettlementCalculator.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        })
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .hasStackTraceContaining("Utility class cannot be instantiated");
    }
}
