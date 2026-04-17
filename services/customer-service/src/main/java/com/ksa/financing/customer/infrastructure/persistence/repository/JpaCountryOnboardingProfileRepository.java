package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.CountryOnboardingProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaCountryOnboardingProfileRepository extends JpaRepository<CountryOnboardingProfileJpaEntity, UUID> {

    List<CountryOnboardingProfileJpaEntity> findAllByCountryCodeAndEnabledTrueOrderByStepOrder(String countryCode);
}
