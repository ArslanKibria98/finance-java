package com.ksa.financing.middleware.application.usecase;

import com.ksa.financing.middleware.application.dto.CreateEnvConfigRequest;
import com.ksa.financing.middleware.application.dto.EnvConfigResponse;
import com.ksa.financing.middleware.application.mapper.MiddlewareMapper;
import com.ksa.financing.middleware.domain.model.ApiEnvironmentConfig;
import com.ksa.financing.middleware.domain.model.AuthType;
import com.ksa.financing.middleware.domain.model.EnvironmentType;
import com.ksa.financing.middleware.domain.port.in.ManageEnvConfigUseCase;
import com.ksa.financing.middleware.domain.port.out.EnvConfigRepository;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ManageEnvConfigService implements ManageEnvConfigUseCase {

    private final EnvConfigRepository envConfigRepository;
    private final MiddlewareMapper mapper;

    @Override
    public EnvConfigResponse create(UUID tenantId, CreateEnvConfigRequest request, UUID createdBy) {
        var config = ApiEnvironmentConfig.create(
                tenantId,
                request.apiId(),
                EnvironmentType.valueOf(request.environment()),
                request.baseUrl(),
                request.endpointPath(),
                request.credentials(),
                request.headers(),
                request.queryParams(),
                request.authType() != null ? AuthType.valueOf(request.authType()) : null,
                createdBy
        );

        var saved = envConfigRepository.save(config);
        log.info("Created env config: apiId={}, env={}, tenantId={}", request.apiId(), request.environment(), tenantId);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EnvConfigResponse getById(UUID tenantId, UUID id) {
        return envConfigRepository.findById(tenantId, id)
                .map(mapper::toResponse)
                .orElseThrow(() -> NotFoundException.forEntity("EnvConfig", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvConfigResponse> listByApi(UUID tenantId, UUID apiId) {
        return envConfigRepository.findAllByApi(tenantId, apiId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvConfigResponse> listAll(UUID tenantId) {
        return envConfigRepository.findAllByTenant(tenantId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    public EnvConfigResponse update(UUID tenantId, UUID id, CreateEnvConfigRequest request) {
        var config = envConfigRepository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("EnvConfig", id.toString()));

        config.setBaseUrl(request.baseUrl());
        config.setEndpointPath(request.endpointPath());
        config.setCredentials(request.credentials());
        config.setHeaders(request.headers());
        config.setQueryParams(request.queryParams());
        if (request.authType() != null) {
            config.setAuthType(AuthType.valueOf(request.authType()));
        }

        var saved = envConfigRepository.save(config);
        log.info("Updated env config: id={}, tenantId={}", id, tenantId);
        return mapper.toResponse(saved);
    }

    @Override
    public void delete(UUID tenantId, UUID id) {
        var config = envConfigRepository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("EnvConfig", id.toString()));

        config.softDelete();
        envConfigRepository.save(config);
        log.info("Soft-deleted env config: id={}, tenantId={}", id, tenantId);
    }
}
