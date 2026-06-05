package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.CountryOnboardingProfile;
import com.ksa.financing.customer.domain.model.SupportedCountry;

import java.util.List;
import java.util.Optional;

/**
 * Output port for country onboarding configuration data.
 */
public interface CountryConfigRepository {

    List<CountryOnboardingProfile> findStepsByCountryCode(String countryCode);

    List<SupportedCountry> findAllActiveCountries();

    Optional<SupportedCountry> findCountryByCode(String countryCode);

    boolean isCountrySupported(String countryCode);
}
