package com.ksa.financing.middleware.application.usecase;

import com.ksa.financing.middleware.application.dto.CreateProviderRequest;
import com.ksa.financing.middleware.application.dto.ProviderResponse;
import com.ksa.financing.middleware.application.dto.UpdateProviderRequest;
import com.ksa.financing.middleware.application.mapper.MiddlewareMapper;
import com.ksa.financing.middleware.domain.model.AuthType;
import com.ksa.financing.middleware.domain.model.ProviderCategory;
import com.ksa.financing.middleware.domain.model.ThirdPartyProvider;
import com.ksa.financing.middleware.domain.port.in.ManageProviderUseCase;
import com.ksa.financing.middleware.domain.port.out.ProviderRepository;
import com.ksa.financing.infra.exception.BusinessException;
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
public class ManageProviderService implements ManageProviderUseCase {

    private final ProviderRepository providerRepository;
    private final MiddlewareMapper mapper;

    @Override
    public ProviderResponse create(UUID tenantId, CreateProviderRequest request, UUID createdBy) {
        if (providerRepository.existsByCode(tenantId, request.code())) {
            throw new BusinessException("MIDDLEWARE.PROVIDER.DUPLICATE_CODE",
                    "Provider with code already exists: " + request.code(), request.code());
        }

        var provider = ThirdPartyProvider.create(
                tenantId,
                request.code(),
                request.name(),
                request.description(),
                ProviderCategory.valueOf(request.category()),
                AuthType.valueOf(request.authType()),
                request.timeoutMs() > 0 ? request.timeoutMs() : 30000,
                request.retryCount() > 0 ? request.retryCount() : 3,
                createdBy
        );
        provider.setBaseUrlDev(request.baseUrlDev());
        provider.setBaseUrlProd(request.baseUrlProd());

        var saved = providerRepository.save(provider);
        log.info("Created provider: code={}, tenantId={}", saved.getCode(), tenantId);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderResponse getById(UUID tenantId, UUID id) {
        return providerRepository.findById(tenantId, id)
                .map(mapper::toResponse)
                .orElseThrow(() -> NotFoundException.forEntity("Provider", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderResponse getByCode(UUID tenantId, String code) {
        return providerRepository.findByCode(tenantId, code)
                .map(mapper::toResponse)
                .orElseThrow(() -> NotFoundException.forEntity("Provider", code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderResponse> listAll(UUID tenantId) {
        return providerRepository.findAllByTenant(tenantId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    public ProviderResponse update(UUID tenantId, UUID id, UpdateProviderRequest request) {
        var provider = providerRepository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Provider", id.toString()));

        provider.update(
                request.name(),
                request.description(),
                ProviderCategory.valueOf(request.category()),
                AuthType.valueOf(request.authType()),
                request.timeoutMs(),
                request.retryCount()
        );
        provider.setBaseUrlDev(request.baseUrlDev());
        provider.setBaseUrlProd(request.baseUrlProd());

        var saved = providerRepository.save(provider);
        log.info("Updated provider: id={}, tenantId={}", id, tenantId);
        return mapper.toResponse(saved);
    }

    @Override
    public void delete(UUID tenantId, UUID id) {
        var provider = providerRepository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Provider", id.toString()));

        provider.softDelete();
        providerRepository.save(provider);
        log.info("Soft-deleted provider: id={}, tenantId={}", id, tenantId);
    }
}
