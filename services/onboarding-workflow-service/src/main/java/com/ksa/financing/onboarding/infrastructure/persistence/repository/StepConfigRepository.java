package com.ksa.financing.onboarding.infrastructure.persistence.repository;

import com.ksa.financing.onboarding.infrastructure.persistence.entity.StepConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface StepConfigRepository extends JpaRepository<StepConfigEntity, UUID> {
    List<StepConfigEntity> findByCountryCodeOrderByOrderIndexAsc(String countryCode);
}
