package com.ksa.financing.onboarding.shared.audit.repository;

import com.ksa.financing.onboarding.shared.audit.entity.OnboardingSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OnboardingSessionRepository extends JpaRepository<OnboardingSessionJpaEntity, UUID> {

    Optional<OnboardingSessionJpaEntity> findByWorkflowId(String workflowId);

    Optional<OnboardingSessionJpaEntity> findFirstByEmailHashOrderByStartedAtDesc(String emailHash);

    Optional<OnboardingSessionJpaEntity> findByKeycloakUserId(String keycloakUserId);
}
