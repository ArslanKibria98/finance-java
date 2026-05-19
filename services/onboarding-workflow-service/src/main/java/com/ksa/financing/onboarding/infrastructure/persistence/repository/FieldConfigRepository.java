package com.ksa.financing.onboarding.infrastructure.persistence.repository;

import com.ksa.financing.onboarding.infrastructure.persistence.entity.FieldConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface FieldConfigRepository extends JpaRepository<FieldConfigEntity, UUID> {
    List<FieldConfigEntity> findByStepIdOrderByOrderIndexAsc(UUID stepId);
}
