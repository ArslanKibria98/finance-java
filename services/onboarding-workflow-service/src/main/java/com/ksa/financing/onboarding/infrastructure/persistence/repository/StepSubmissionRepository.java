package com.ksa.financing.onboarding.infrastructure.persistence.repository;

import com.ksa.financing.onboarding.infrastructure.persistence.entity.StepSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface StepSubmissionRepository extends JpaRepository<StepSubmissionEntity, UUID> {
    List<StepSubmissionEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
