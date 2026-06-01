package com.ksa.financing.onboarding.shared.audit.repository;

import com.ksa.financing.onboarding.shared.audit.entity.OnboardingStepAuditJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OnboardingStepAuditRepository extends JpaRepository<OnboardingStepAuditJpaEntity, UUID> {

    List<OnboardingStepAuditJpaEntity> findBySessionIdOrderByOccurredAtAsc(UUID sessionId);

    List<OnboardingStepAuditJpaEntity> findByWorkflowIdOrderByOccurredAtAsc(String workflowId);
}
