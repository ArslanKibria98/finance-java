package com.ksa.financing.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for SarMoney (Money value object).
 * <p>
 * Tests cover:
 * - Creation methods (of(BigDecimal), of(double), of(long))
 * - Addition and subtraction
 * - Multiplication and division
 * - Precision (2 decimal places)
 * - Rounding (HALF_UP)
 * - Comparison methods (isGreaterThan, isLessThan, etc.)
 * - isZero, isPositive, isNegative
 * - Currency validation (only SAR allowed)
 * - equals and hashCode
 * - Division by zero throws exception
 * </p>
 */
@DisplayName("SarMoney Tests")
class SarMoneyTest {

    @Test
    @DisplayName("Should create SarMoney from BigDecimal")
    void shouldCreateFromBigDecimal() {
        // Given
        BigDecimal amount = new BigDecimal("1234.56");

        // When
        SarMoney money = SarMoney.of(amount);

        // Then
        assertThat(money).isNotNull();
        assertThat(money.getValue()).isEqualByComparingTo(new BigDecimal("1234.56"));
        assertThat(money.getCurrency().getCurrencyCode()).isEqualTo("SAR");
    }

    @Test
    @DisplayName("Should create SarMoney from double")
    void shouldCreateFromDouble() {
        // Given
        double amount = 1234.56;

        // When
        SarMoney money = SarMoney.of(amount);

        // Then
        assertThat(money).isNotNull();
        assertThat(money.getValue()).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    @DisplayName("Should create SarMoney from long")
    void shouldCreateFromLong() {
        // Given
        long amount = 1234L;

        // When
        SarMoney money = SarMoney.of(amount);

        // Then
        assertThat(money).isNotNull();
        assertThat(money.getValue()).isEqualByComparingTo(new BigDecimal("1234.00"));
    }

    @Test
    @DisplayName("Should create zero SarMoney")
    void shouldCreateZero() {
        // When
        SarMoney money = SarMoney.zero();

        // Then
        assertThat(money).isNotNull();
        assertThat(money.getValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(money.isZero()).isTrue();
    }

    @Test
    @DisplayName("Should add two SarMoney amounts")
    void shouldAddTwoAmounts() {
        // Given
        SarMoney money1 = SarMoney.of(100.50);
        SarMoney money2 = SarMoney.of(50.25);

        // When
        SarMoney result = money1.add(money2);

        // Then
        assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("150.75"));
    }

    @Test
    @DisplayName("Should subtract two SarMoney amounts")
    void shouldSubtractTwoAmounts() {
        // Given
        SarMoney money1 = SarMoney.of(100.50);
        SarMoney money2 = SarMoney.of(50.25);

        // When
        SarMoney result = money1.subtract(money2);

        // Then
        assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("50.25"));
    }

    @Test
    @DisplayName("Should multiply by BigDecimal factor")
    void shouldMultiplyByBigDecimalFactor() {
        // Given
        SarMoney money = SarMoney.of(100.00);
        BigDecimal factor = new BigDecimal("1.5");

        // When
        SarMoney result = money.multiply(factor);

        // Then
        assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Should multiply by double factor")
    void shouldMultiplyByDoubleFactor() {
        // Given
        SarMoney money = SarMoney.of(100.00);
        double factor = 0.05;

        // When
        SarMoney result = money.multiply(factor);

        // Then
        assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("Should divide by BigDecimal divisor")
    void shouldDivideByBigDecimalDivisor() {
        // Given
        SarMoney money = SarMoney.of(100.00);
        BigDecimal divisor = new BigDecimal("4");

        // When
        SarMoney result = money.divide(divisor);

        // Then
        assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("Should divide by long divisor")
    void shouldDivideByLongDivisor() {
        // Given
        SarMoney money = SarMoney.of(100.00);
        long divisor = 4L;

        // When
        SarMoney result = money.divide(divisor);

        // Then
        assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("Should throw exception when dividing by zero BigDecimal")
    void shouldThrowExceptionWhenDividingByZeroBigDecimal() {
        // Given
        SarMoney money = SarMoney.of(100.00);
        BigDecimal divisor = BigDecimal.ZERO;

        // When/Then
        assertThatThrownBy(() -> money.divide(divisor))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("Cannot divide by zero");
    }

    @Test
    @DisplayName("Should throw exception when dividing by zero long")
    void shouldThrowExceptionWhenDividingByZeroLong() {
        // Given
        SarMoney money = SarMoney.of(100.00);
        long divisor = 0L;

        // When/Then
        assertThatThrownBy(() -> money.divide(divisor))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("Cannot divide by zero");
    }

    @Test
    @DisplayName("Should round to 2 decimal places with HALF_UP")
    void shouldRoundToTwoDecimalPlaces() {
        // Given
        BigDecimal amount = new BigDecimal("1234.567");

        // When
        SarMoney money = SarMoney.of(amount);

        // Then
        assertThat(money.getValue()).isEqualByComparingTo(new BigDecimal("1234.57"));
    }

    @Test
    @DisplayName("Should round down when less than 0.5")
    void shouldRoundDownWhenLessThanHalf() {
        // Given
        BigDecimal amount = new BigDecimal("1234.564");

        // When
        SarMoney money = SarMoney.of(amount);

        // Then
        assertThat(money.getValue()).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    @DisplayName("Should identify zero amount")
    void shouldIdentifyZeroAmount() {
        // Given
        SarMoney money = SarMoney.zero();

        // When/Then
        assertThat(money.isZero()).isTrue();
        assertThat(money.isPositive()).isFalse();
        assertThat(money.isNegative()).isFalse();
    }

    @Test
    @DisplayName("Should identify positive amount")
    void shouldIdentifyPositiveAmount() {
        // Given
        SarMoney money = SarMoney.of(100.00);

        // When/Then
        assertThat(money.isPositive()).isTrue();
        assertThat(money.isZero()).isFalse();
        assertThat(money.isNegative()).isFalse();
    }

    @Test
    @DisplayName("Should identify negative amount")
    void shouldIdentifyNegativeAmount() {
        // Given
        SarMoney money = SarMoney.of(-100.00);

        // When/Then
        assertThat(money.isNegative()).isTrue();
        assertThat(money.isPositive()).isFalse();
        assertThat(money.isZero()).isFalse();
    }

    @Test
    @DisplayName("Should compare if greater than another amount")
    void shouldCompareIsGreaterThan() {
        // Given
        SarMoney money1 = SarMoney.of(100.00);
        SarMoney money2 = SarMoney.of(50.00);

        // When/Then
        assertThat(money1.isGreaterThan(money2)).isTrue();
        assertThat(money2.isGreaterThan(money1)).isFalse();
    }

    @Test
    @DisplayName("Should compare if less than another amount")
    void shouldCompareIsLessThan() {
        // Given
        SarMoney money1 = SarMoney.of(50.00);
        SarMoney money2 = SarMoney.of(100.00);

        // When/Then
        assertThat(money1.isLessThan(money2)).isTrue();
        assertThat(money2.isLessThan(money1)).isFalse();
    }

    @Test
    @DisplayName("Should compare if greater than or equal to another amount")
    void shouldCompareIsGreaterThanOrEqualTo() {
        // Given
        SarMoney money1 = SarMoney.of(100.00);
        SarMoney money2 = SarMoney.of(100.00);
        SarMoney money3 = SarMoney.of(50.00);

        // When/Then
        assertThat(money1.isGreaterThanOrEqualTo(money2)).isTrue();
        assertThat(money1.isGreaterThanOrEqualTo(money3)).isTrue();
        assertThat(money3.isGreaterThanOrEqualTo(money1)).isFalse();
    }

    @Test
    @DisplayName("Should enforce SAR currency only")
    void shouldEnforceSarCurrencyOnly() {
        // Given
        SarMoney money = SarMoney.of(100.00);

        // When/Then
        assertThat(money.getCurrency().getCurrencyCode()).isEqualTo("SAR");
    }

    @Test
    @DisplayName("Should have proper equals implementation")
    void shouldHaveProperEquals() {
        // Given
        SarMoney money1 = SarMoney.of(100.00);
        SarMoney money2 = SarMoney.of(100.00);
        SarMoney money3 = SarMoney.of(200.00);

        // When/Then
        assertThat(money1).isEqualTo(money2);
        assertThat(money1).isNotEqualTo(money3);
        assertThat(money1).isNotEqualTo(null);
        assertThat(money1).isEqualTo(money1);
    }

    @Test
    @DisplayName("Should have proper hashCode implementation")
    void shouldHaveProperHashCode() {
        // Given
        SarMoney money1 = SarMoney.of(100.00);
        SarMoney money2 = SarMoney.of(100.00);

        // When/Then
        assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
    }

    @Test
    @DisplayName("Should format as string with currency symbol")
    void shouldFormatAsStringWithCurrencySymbol() {
        // Given
        SarMoney money = SarMoney.of(1234.56);

        // When
        String formatted = money.toFormattedString();

        // Then
        assertThat(formatted).isEqualTo("SAR 1,234.56");
    }

    @Test
    @DisplayName("Should throw exception when amount is null")
    void shouldThrowExceptionWhenAmountIsNull() {
        // When/Then
        assertThatThrownBy(() -> SarMoney.of((BigDecimal) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Amount cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when adding null amount")
    void shouldThrowExceptionWhenAddingNull() {
        // Given
        SarMoney money = SarMoney.of(100.00);

        // When/Then
        assertThatThrownBy(() -> money.add(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Other amount cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when subtracting null amount")
    void shouldThrowExceptionWhenSubtractingNull() {
        // Given
        SarMoney money = SarMoney.of(100.00);

        // When/Then
        assertThatThrownBy(() -> money.subtract(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Other amount cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when multiplying by null factor")
    void shouldThrowExceptionWhenMultiplyingByNull() {
        // Given
        SarMoney money = SarMoney.of(100.00);

        // When/Then
        assertThatThrownBy(() -> money.multiply((BigDecimal) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Factor cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when dividing by null divisor")
    void shouldThrowExceptionWhenDividingByNull() {
        // Given
        SarMoney money = SarMoney.of(100.00);

        // When/Then
        assertThatThrownBy(() -> money.divide((BigDecimal) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Divisor cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when comparing with null")
    void shouldThrowExceptionWhenComparingWithNull() {
        // Given
        SarMoney money = SarMoney.of(100.00);

        // When/Then
        assertThatThrownBy(() -> money.isGreaterThan(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Other amount cannot be null");

        assertThatThrownBy(() -> money.isLessThan(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Other amount cannot be null");
    }

    @Test
    @DisplayName("Should handle very small amounts with proper precision")
    void shouldHandleVerySmallAmounts() {
        // Given
        SarMoney money = SarMoney.of(0.01);

        // When/Then
        assertThat(money.getValue()).isEqualByComparingTo(new BigDecimal("0.01"));
        assertThat(money.isPositive()).isTrue();
    }

    @Test
    @DisplayName("Should handle very large amounts")
    void shouldHandleVeryLargeAmounts() {
        // Given
        SarMoney money = SarMoney.of(999999999.99);

        // When/Then
        assertThat(money.getValue()).isEqualByComparingTo(new BigDecimal("999999999.99"));
        assertThat(money.isPositive()).isTrue();
    }

    @Test
    @DisplayName("Should perform multiple operations correctly")
    void shouldPerformMultipleOperationsCorrectly() {
        // Given
        SarMoney money = SarMoney.of(100.00);

        // When
        SarMoney result = money
                .add(SarMoney.of(50.00))
                .multiply(2.0)
                .subtract(SarMoney.of(100.00))
                .divide(2L);

        // Then
        assertThat(result.getValue()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should have correct toString implementation")
    void shouldHaveCorrectToString() {
        // Given
        SarMoney money = SarMoney.of(100.00);

        // When
        String result = money.toString();

        // Then
        assertThat(result).contains("SAR").contains("100");
    }
}
