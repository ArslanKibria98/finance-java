package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.SupportedCountryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaSupportedCountryRepository extends JpaRepository<SupportedCountryJpaEntity, UUID> {

    List<SupportedCountryJpaEntity> findAllByActiveTrue();

    Optional<SupportedCountryJpaEntity> findByCountryCode(String countryCode);

    boolean existsByCountryCodeAndActiveTrue(String countryCode);
}
