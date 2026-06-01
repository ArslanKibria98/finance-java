package com.ksa.financing.onboarding.shared.audit.repository;

import com.ksa.financing.onboarding.shared.audit.entity.OnboardingExternalRefJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OnboardingExternalRefRepository extends JpaRepository<OnboardingExternalRefJpaEntity, UUID> {

    List<OnboardingExternalRefJpaEntity> findBySessionId(UUID sessionId);

    List<OnboardingExternalRefJpaEntity> findByWorkflowIdAndRefType(String workflowId, String refType);
}
