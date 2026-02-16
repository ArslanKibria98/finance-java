package com.ksa.financing.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for IBAN (Saudi International Bank Account Number).
 * <p>
 * Tests cover:
 * - Valid Saudi IBAN (SA + 22 digits)
 * - Invalid format (wrong country, wrong length)
 * - Checksum validation with correct checksums
 * - Checksum validation with incorrect checksums
 * - Normalization (spaces removed, uppercase)
 * - Formatted string output
 * - Masked string (security)
 * - getBankCode(), getAccountNumber()
 * </p>
 */
@DisplayName("IBAN Tests")
class IBANTest {

    @Test
    @DisplayName("Should create valid Saudi IBAN")
    void shouldCreateValidSaudiIban() {
        // Given
        String validIban = "SA0380000000608010167519";

        // When
        IBAN iban = IBAN.of(validIban);

        // Then
        assertThat(iban).isNotNull();
        assertThat(iban.value()).isEqualTo(validIban);
        assertThat(iban.getCountryCode()).isEqualTo("SA");
    }

    @Test
    @DisplayName("Should validate correct IBAN checksum")
    void shouldValidateCorrectChecksum() {
        // Given - IBAN with valid checksum
        String validIban = "SA0380000000608010167519";

        // When/Then - Should not throw exception
        assertThatCode(() -> IBAN.of(validIban))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject invalid IBAN checksum")
    void shouldRejectInvalidChecksum() {
        // Given - IBAN with invalid checksum (changed last digit)
        String invalidIban = "SA0380000000608010167518";

        // When/Then
        assertThatThrownBy(() -> IBAN.of(invalidIban))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid IBAN checksum");
    }

    @Test
    @DisplayName("Should reject IBAN with wrong country code")
    void shouldRejectWrongCountryCode() {
        // Given
        String invalidIban = "GB82WEST12345698765432";

        // When/Then
        assertThatThrownBy(() -> IBAN.of(invalidIban))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Saudi IBAN format");
    }

    @Test
    @DisplayName("Should reject IBAN with wrong length")
    void shouldRejectWrongLength() {
        // Given - Too short
        String tooShort = "SA038000000060801016751";

        // When/Then
        assertThatThrownBy(() -> IBAN.of(tooShort))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Saudi IBAN format");
    }

    @Test
    @DisplayName("Should reject IBAN with letters in account number")
    void shouldRejectLettersInAccountNumber() {
        // Given
        String invalidIban = "SA0380000000608010ABC519";

        // When/Then
        assertThatThrownBy(() -> IBAN.of(invalidIban))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Saudi IBAN format");
    }

    @Test
    @DisplayName("Should normalize IBAN by removing spaces")
    void shouldNormalizeByRemovingSpaces() {
        // Given - IBAN with spaces
        String ibanWithSpaces = "SA03 8000 0000 6080 1016 7519";

        // When
        IBAN iban = IBAN.of(ibanWithSpaces);

        // Then
        assertThat(iban.value()).isEqualTo("SA0380000000608010167519");
    }

    @Test
    @DisplayName("Should normalize IBAN to uppercase")
    void shouldNormalizeToUppercase() {
        // Given - IBAN in lowercase
        String lowercaseIban = "sa0380000000608010167519";

        // When
        IBAN iban = IBAN.of(lowercaseIban);

        // Then
        assertThat(iban.value()).isEqualTo("SA0380000000608010167519");
    }

    @Test
    @DisplayName("Should get check digits correctly")
    void shouldGetCheckDigits() {
        // Given
        String validIban = "SA0380000000608010167519";

        // When
        IBAN iban = IBAN.of(validIban);

        // Then
        assertThat(iban.getCheckDigits()).isEqualTo("03");
    }

    @Test
    @DisplayName("Should get BBAN (Basic Bank Account Number)")
    void shouldGetBban() {
        // Given
        String validIban = "SA0380000000608010167519";

        // When
        IBAN iban = IBAN.of(validIban);

        // Then
        assertThat(iban.getBban()).isEqualTo("80000000608010167519");
        assertThat(iban.getBban()).hasSize(20);
    }

    @Test
    @DisplayName("Should get bank code (first 2 digits of BBAN)")
    void shouldGetBankCode() {
        // Given
        String validIban = "SA0380000000608010167519";

        // When
        IBAN iban = IBAN.of(validIban);

        // Then
        assertThat(iban.getBankCode()).isEqualTo("80");
    }

    @Test
    @DisplayName("Should get account number")
    void shouldGetAccountNumber() {
        // Given
        String validIban = "SA0380000000608010167519";

        // When
        IBAN iban = IBAN.of(validIban);

        // Then
        assertThat(iban.getAccountNumber()).isEqualTo("000000608010167519");
    }

    @Test
    @DisplayName("Should format IBAN with spaces")
    void shouldFormatWithSpaces() {
        // Given
        String validIban = "SA0380000000608010167519";

        // When
        IBAN iban = IBAN.of(validIban);
        String formatted = iban.toFormattedString();

        // Then
        assertThat(formatted).isEqualTo("SA03 8000 0000 6080 1016 7519");
    }

    @Test
    @DisplayName("Should mask IBAN for security")
    void shouldMaskIbanForSecurity() {
        // Given
        String validIban = "SA0380000000608010167519";

        // When
        IBAN iban = IBAN.of(validIban);
        String masked = iban.toMaskedString();

        // Then
        assertThat(masked).isEqualTo("SA** **** **** **** **** 7519");
        assertThat(masked).contains("7519");
        assertThat(masked).doesNotContain("8000");
    }

    @Test
    @DisplayName("Should have proper toString implementation")
    void shouldHaveProperToString() {
        // Given
        String validIban = "SA0380000000608010167519";

        // When
        IBAN iban = IBAN.of(validIban);

        // Then
        assertThat(iban.toString()).isEqualTo(validIban);
    }

    @Test
    @DisplayName("Should throw exception when IBAN is null")
    void shouldThrowExceptionWhenIbanIsNull() {
        // When/Then
        assertThatThrownBy(() -> IBAN.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("IBAN cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when IBAN is empty")
    void shouldThrowExceptionWhenIbanIsEmpty() {
        // When/Then
        assertThatThrownBy(() -> IBAN.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Saudi IBAN format");
    }

    @Test
    @DisplayName("Should have proper equals implementation")
    void shouldHaveProperEquals() {
        // Given
        IBAN iban1 = IBAN.of("SA0380000000608010167519");
        IBAN iban2 = IBAN.of("SA0380000000608010167519");
        IBAN iban3 = IBAN.of("SA4420000001234567891234");

        // When/Then
        assertThat(iban1).isEqualTo(iban2);
        assertThat(iban1).isNotEqualTo(iban3);
        assertThat(iban1).isEqualTo(iban1);
    }

    @Test
    @DisplayName("Should have proper hashCode implementation")
    void shouldHaveProperHashCode() {
        // Given
        IBAN iban1 = IBAN.of("SA0380000000608010167519");
        IBAN iban2 = IBAN.of("SA0380000000608010167519");

        // When/Then
        assertThat(iban1.hashCode()).isEqualTo(iban2.hashCode());
    }

    @Test
    @DisplayName("Should validate another valid Saudi IBAN")
    void shouldValidateAnotherValidSaudiIban() {
        // Given - Another valid Saudi IBAN
        String validIban = "SA4420000001234567891234";

        // When
        IBAN iban = IBAN.of(validIban);

        // Then
        assertThat(iban).isNotNull();
        assertThat(iban.value()).isEqualTo(validIban);
        assertThat(iban.getBankCode()).isEqualTo("20");
    }

    @Test
    @DisplayName("Should handle IBAN with mixed spaces and case")
    void shouldHandleIbanWithMixedSpacesAndCase() {
        // Given
        String messyIban = "sa03 8000 0000 6080 1016 7519";

        // When
        IBAN iban = IBAN.of(messyIban);

        // Then
        assertThat(iban.value()).isEqualTo("SA0380000000608010167519");
    }

    @Test
    @DisplayName("Should reject IBAN with special characters")
    void shouldRejectIbanWithSpecialCharacters() {
        // Given
        String invalidIban = "SA03-8000-0000-6080-1016-7519";

        // When/Then
        assertThatThrownBy(() -> IBAN.of(invalidIban))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should validate IBAN length is exactly 24 characters")
    void shouldValidateIbanLengthIs24() {
        // Given
        String validIban = "SA0380000000608010167519";

        // When
        IBAN iban = IBAN.of(validIban);

        // Then
        assertThat(iban.value()).hasSize(24);
    }

    @Test
    @DisplayName("Should extract correct components from different valid IBANs")
    void shouldExtractCorrectComponentsFromDifferentIbans() {
        // Given
        IBAN iban1 = IBAN.of("SA0380000000608010167519");
        IBAN iban2 = IBAN.of("SA4420000001234567891234");

        // When/Then
        assertThat(iban1.getBankCode()).isEqualTo("80");
        assertThat(iban2.getBankCode()).isEqualTo("20");

        assertThat(iban1.getCheckDigits()).isEqualTo("03");
        assertThat(iban2.getCheckDigits()).isEqualTo("44");
    }

    @Test
    @DisplayName("Should validate using record pattern matching")
    void shouldValidateUsingRecordPatternMatching() {
        // Given
        IBAN iban = IBAN.of("SA0380000000608010167519");

        // When
        String value = iban.value();

        // Then
        assertThat(value).startsWith("SA");
        assertThat(value).hasSize(24);
    }

    @Test
    @DisplayName("Should validate MOD-97 algorithm with edge case")
    void shouldValidateMod97AlgorithmWithEdgeCase() {
        // Given - Testing with different valid checksums
        String[] validIbans = {
            "SA0380000000608010167519",
            "SA4420000001234567891234"
        };

        // When/Then - All should pass validation
        for (String ibanString : validIbans) {
            assertThatCode(() -> IBAN.of(ibanString))
                    .as("IBAN %s should be valid", ibanString)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Should reject IBAN with too many digits")
    void shouldRejectIbanWithTooManyDigits() {
        // Given - IBAN with 25 characters
        String tooLong = "SA03800000006080101675191";

        // When/Then
        assertThatThrownBy(() -> IBAN.of(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Saudi IBAN format");
    }
}
