package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.assessment.AssessmentAnswer;
import com.ksa.financing.risk.domain.port.out.AssessmentAnswerRepository;
import com.ksa.financing.risk.infrastructure.persistence.entity.AssessmentAnswerJpaEntity;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AssessmentAnswerRepositoryImpl implements AssessmentAnswerRepository {
    private final JpaAssessmentAnswerRepository jpa;

    @Override
    public AssessmentAnswer save(AssessmentAnswer answer) {
        return RiskPersistenceMapper.toDomain(jpa.save(RiskPersistenceMapper.toEntity(answer)));
    }
    @Override
    public List<AssessmentAnswer> saveAll(List<AssessmentAnswer> answers) {
        var entities = answers.stream().map(RiskPersistenceMapper::toEntity).toList();
        return jpa.saveAll(entities).stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public Optional<AssessmentAnswer> findById(UUID tenantId, UUID id) {
        return jpa.findByIdAndTenantId(id, tenantId).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public List<AssessmentAnswer> findBySessionId(UUID tenantId, UUID sessionId) {
        if (tenantId == null) {
            return jpa.findAllBySessionId(sessionId).stream().map(RiskPersistenceMapper::toDomain).toList();
        }
        return jpa.findAllByTenantIdAndSessionId(tenantId, sessionId).stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<AssessmentAnswer> findActiveBySessionId(UUID tenantId, UUID sessionId) {
        if (tenantId == null) {
            return jpa.findAllBySessionIdAndVersionStatus(sessionId, AssessmentAnswerJpaEntity.VersionStatusEnum.ACTIVE)
                    .stream().map(RiskPersistenceMapper::toDomain).toList();
        }
        return jpa.findAllByTenantIdAndSessionIdAndVersionStatus(tenantId, sessionId, AssessmentAnswerJpaEntity.VersionStatusEnum.ACTIVE)
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<AssessmentAnswer> findBySessionIdAndParameterId(UUID tenantId, UUID sessionId, UUID parameterId) {
        return jpa.findAllByTenantIdAndSessionIdAndParameterId(tenantId, sessionId, parameterId)
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
}
