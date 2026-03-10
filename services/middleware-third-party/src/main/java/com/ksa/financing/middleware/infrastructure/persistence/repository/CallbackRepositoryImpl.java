package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.domain.model.CallbackResponse;
import com.ksa.financing.middleware.domain.port.out.CallbackRepository;
import com.ksa.financing.middleware.infrastructure.persistence.mapper.MiddlewarePersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CallbackRepositoryImpl implements CallbackRepository {

    private final JpaCallbackRepository jpaRepository;
    private final MiddlewarePersistenceMapper mapper;

    @Override
    public CallbackResponse save(CallbackResponse callback) {
        log.debug("Saving callback: apiId={}, tenantId={}", callback.getApiId(), callback.getTenantId());
        var entity = mapper.toEntity(callback);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CallbackResponse> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByIdAndTenantId(id, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public List<CallbackResponse> findAll(UUID tenantId, int page, int size) {
        return jpaRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size))
                .stream().map(mapper::toDomain).toList();
    }
}
