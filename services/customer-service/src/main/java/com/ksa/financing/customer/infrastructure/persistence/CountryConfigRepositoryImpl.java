package com.ksa.financing.customer.infrastructure.persistence;

import com.ksa.financing.customer.domain.model.CountryOnboardingProfile;
import com.ksa.financing.customer.domain.model.SupportedCountry;
import com.ksa.financing.customer.domain.port.out.CountryConfigRepository;
import com.ksa.financing.customer.infrastructure.persistence.entity.CountryOnboardingProfileJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.entity.SupportedCountryJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.repository.JpaCountryOnboardingProfileRepository;
import com.ksa.financing.customer.infrastructure.persistence.repository.JpaSupportedCountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CountryConfigRepositoryImpl implements CountryConfigRepository {

    private final JpaCountryOnboardingProfileRepository profileRepo;
    private final JpaSupportedCountryRepository countryRepo;

    @Override
    public List<CountryOnboardingProfile> findStepsByCountryCode(String countryCode) {
        return profileRepo.findAllByCountryCodeAndEnabledTrueOrderByStepOrder(countryCode)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<SupportedCountry> findAllActiveCountries() {
        return countryRepo.findAllByActiveTrue().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<SupportedCountry> findCountryByCode(String countryCode) {
        return countryRepo.findByCountryCode(countryCode).map(this::toDomain);
    }

    @Override
    public boolean isCountrySupported(String countryCode) {
        return countryRepo.existsByCountryCodeAndActiveTrue(countryCode);
    }

    private CountryOnboardingProfile toDomain(CountryOnboardingProfileJpaEntity entity) {
        var profile = new CountryOnboardingProfile();
        profile.setId(entity.getId());
        profile.setTenantId(entity.getTenantId());
        profile.setCountryCode(entity.getCountryCode());
        profile.setStepOrder(entity.getStepOrder());
        profile.setStepType(entity.getStepType());
        profile.setStepLabel(entity.getStepLabel());
        profile.setStepLabelAr(entity.getStepLabelAr());
        profile.setDescription(entity.getDescription());
        profile.setProviderCode(entity.getProviderCode());
        profile.setSignalWait(entity.isSignalWait());
        profile.setTimeoutMinutes(entity.getTimeoutMinutes());
        profile.setRequired(entity.isRequired());
        profile.setEnabled(entity.isEnabled());
        profile.setConfigJson(entity.getConfigJson());
        return profile;
    }

    private SupportedCountry toDomain(SupportedCountryJpaEntity entity) {
        return new SupportedCountry(
                entity.getCountryCode(),
                entity.getCountryName(),
                entity.getCountryNameAr(),
                entity.getCurrencyCode(),
                entity.isActive(),
                entity.getIdTypes(),
                entity.getDefaultIdType(),
                entity.getKycProviders(),
                entity.getFlagEmoji(),
                entity.getDialCode(),
                entity.getNationalityEn(),
                entity.getNationalityAr()
        );
    }
}
