package com.ksa.financing.onboarding.infrastructure.persistence.repository;

import com.ksa.financing.onboarding.infrastructure.persistence.entity.WorkflowConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowConfigRepository extends JpaRepository<WorkflowConfigEntity, String> {
}
