package com.ksa.financing.domain.valueobject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Immutable value object representing a Saudi address.
 * <p>
 * This value object encapsulates address information following Saudi Arabia's
 * addressing system. All addresses default to Saudi Arabia (SA) as the country.
 * </p>
 * <p>
 * Postal codes in Saudi Arabia follow a 5-digit format, with an optional
 * 4-digit extension (12345 or 12345-1234).
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record Address(
        @NotBlank(message = "Street cannot be blank")
        @Size(min = 3, max = 200, message = "Street must be between 3 and 200 characters")
        String street,

        @NotBlank(message = "District cannot be blank")
        @Size(min = 2, max = 100, message = "District must be between 2 and 100 characters")
        String district,

        @NotBlank(message = "City cannot be blank")
        @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
        String city,

        @NotBlank(message = "Postal code cannot be blank")
        @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "Postal code must be 5 digits or 5+4 digits (12345 or 12345-1234)")
        String postalCode,

        @NotBlank(message = "Country cannot be blank")
        @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be 2 uppercase letters")
        String country
) {
    private static final String DEFAULT_COUNTRY = "SA";

    /**
     * Canonical constructor with validation and normalization.
     */
    public Address {
        Objects.requireNonNull(street, "Street cannot be null");
        Objects.requireNonNull(district, "District cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(postalCode, "Postal code cannot be null");
        Objects.requireNonNull(country, "Country cannot be null");

        // Normalize: trim whitespace
        street = street.trim();
        district = district.trim();
        city = city.trim();
        postalCode = postalCode.trim();
        country = country.trim().toUpperCase();

        // Validate lengths
        if (street.length() < 3 || street.length() > 200) {
            throw new IllegalArgumentException("Street must be between 3 and 200 characters");
        }
        if (district.length() < 2 || district.length() > 100) {
            throw new IllegalArgumentException("District must be between 2 and 100 characters");
        }
        if (city.length() < 2 || city.length() > 100) {
            throw new IllegalArgumentException("City must be between 2 and 100 characters");
        }

        // Validate postal code format
        if (!postalCode.matches("^\\d{5}(-\\d{4})?$")) {
            throw new IllegalArgumentException(
                    "Postal code must be 5 digits or 5+4 digits (12345 or 12345-1234). Got: " + postalCode
            );
        }

        // Validate country code
        if (!country.matches("^[A-Z]{2}$")) {
            throw new IllegalArgumentException(
                    "Country code must be 2 uppercase letters. Got: " + country
            );
        }
    }

    /**
     * Create an Address with default Saudi Arabia country code.
     *
     * @param street     the street address
     * @param district   the district/neighborhood
     * @param city       the city
     * @param postalCode the postal code
     * @return a new Address instance
     */
    public static Address of(String street, String district, String city, String postalCode) {
        return new Address(street, district, city, postalCode, DEFAULT_COUNTRY);
    }

    /**
     * Create an Address with all fields specified.
     *
     * @param street     the street address
     * @param district   the district/neighborhood
     * @param city       the city
     * @param postalCode the postal code
     * @param country    the country code (2 letters)
     * @return a new Address instance
     */
    public static Address of(String street, String district, String city, String postalCode, String country) {
        return new Address(street, district, city, postalCode, country);
    }

    /**
     * Check if this is a Saudi address.
     *
     * @return true if country is SA
     */
    public boolean isSaudiAddress() {
        return DEFAULT_COUNTRY.equals(country);
    }

    /**
     * Get the base postal code (5 digits without extension).
     *
     * @return the base postal code
     */
    public String getBasePostalCode() {
        int dashIndex = postalCode.indexOf('-');
        if (dashIndex > 0) {
            return postalCode.substring(0, dashIndex);
        }
        return postalCode;
    }

    /**
     * Get the postal code extension (4 digits) if present.
     *
     * @return the postal code extension, or null if not present
     */
    public String getPostalCodeExtension() {
        int dashIndex = postalCode.indexOf('-');
        if (dashIndex > 0 && dashIndex < postalCode.length() - 1) {
            return postalCode.substring(dashIndex + 1);
        }
        return null;
    }

    /**
     * Check if this address has a postal code extension.
     *
     * @return true if postal code has extension
     */
    public boolean hasPostalCodeExtension() {
        return postalCode.contains("-");
    }

    /**
     * Create a new Address with a different street.
     *
     * @param newStreet the new street
     * @return a new Address instance
     */
    public Address withStreet(String newStreet) {
        return new Address(newStreet, district, city, postalCode, country);
    }

    /**
     * Create a new Address with a different district.
     *
     * @param newDistrict the new district
     * @return a new Address instance
     */
    public Address withDistrict(String newDistrict) {
        return new Address(street, newDistrict, city, postalCode, country);
    }

    /**
     * Create a new Address with a different city.
     *
     * @param newCity the new city
     * @return a new Address instance
     */
    public Address withCity(String newCity) {
        return new Address(street, district, newCity, postalCode, country);
    }

    /**
     * Create a new Address with a different postal code.
     *
     * @param newPostalCode the new postal code
     * @return a new Address instance
     */
    public Address withPostalCode(String newPostalCode) {
        return new Address(street, district, city, newPostalCode, country);
    }

    /**
     * Create a new Address with a different country.
     *
     * @param newCountry the new country code
     * @return a new Address instance
     */
    public Address withCountry(String newCountry) {
        return new Address(street, district, city, postalCode, newCountry);
    }

    /**
     * Format the address as a single line.
     *
     * @return formatted single-line address
     */
    public String toSingleLine() {
        return String.format("%s, %s, %s %s, %s",
                street, district, city, postalCode, country);
    }

    /**
     * Format the address as multiple lines.
     *
     * @return formatted multi-line address
     */
    public String toMultiLine() {
        return String.format("%s%n%s%n%s %s%n%s",
                street, district, city, postalCode, country);
    }

    @Override
    public String toString() {
        return toSingleLine();
    }
}
