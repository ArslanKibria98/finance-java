package com.ksa.financing.middleware.application.usecase;

import com.ksa.financing.middleware.application.dto.CreateProviderApiRequest;
import com.ksa.financing.middleware.application.dto.ProviderApiResponse;
import com.ksa.financing.middleware.application.dto.UpdateProviderApiRequest;
import com.ksa.financing.middleware.application.mapper.MiddlewareMapper;
import com.ksa.financing.middleware.domain.model.HttpMethod;
import com.ksa.financing.middleware.domain.model.ProviderApi;
import com.ksa.financing.middleware.domain.port.in.ManageProviderApiUseCase;
import com.ksa.financing.middleware.domain.port.out.ProviderApiRepository;
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
public class ManageProviderApiService implements ManageProviderApiUseCase {

    private final ProviderApiRepository providerApiRepository;
    private final MiddlewareMapper mapper;

    @Override
    public ProviderApiResponse create(UUID tenantId, CreateProviderApiRequest request, UUID createdBy) {
        if (providerApiRepository.existsByCode(tenantId, request.code())) {
            throw new BusinessException("MIDDLEWARE.API.DUPLICATE_CODE",
                    "Provider API with code already exists: " + request.code(), request.code());
        }

        var api = ProviderApi.create(
                tenantId,
                request.providerId(),
                request.code(),
                request.nameEn(),
                request.nameAr(),
                request.descriptionEn(),
                request.descriptionAr(),
                HttpMethod.valueOf(request.httpMethod()),
                request.endpointPath(),
                request.async(),
                request.timeoutMs(),
                createdBy
        );

        var saved = providerApiRepository.save(api);
        log.info("Created provider API: code={}, tenantId={}", saved.getCode(), tenantId);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderApiResponse getById(UUID tenantId, UUID id) {
        return providerApiRepository.findById(tenantId, id)
                .map(mapper::toResponse)
                .orElseThrow(() -> NotFoundException.forEntity("ProviderApi", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderApiResponse> listByProvider(UUID tenantId, UUID providerId) {
        return providerApiRepository.findAllByProvider(tenantId, providerId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderApiResponse> listAll(UUID tenantId) {
        return providerApiRepository.findAllByTenant(tenantId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    public ProviderApiResponse update(UUID tenantId, UUID id, UpdateProviderApiRequest request) {
        var api = providerApiRepository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("ProviderApi", id.toString()));

        api.setNameEn(request.nameEn());
        api.setNameAr(request.nameAr());
        api.setDescriptionEn(request.descriptionEn());
        api.setDescriptionAr(request.descriptionAr());
        api.setHttpMethod(HttpMethod.valueOf(request.httpMethod()));
        api.setEndpointPath(request.endpointPath());
        api.setAsync(request.async());
        api.setTimeoutMs(request.timeoutMs());

        var saved = providerApiRepository.save(api);
        log.info("Updated provider API: id={}, tenantId={}", id, tenantId);
        return mapper.toResponse(saved);
    }

    @Override
    public void delete(UUID tenantId, UUID id) {
        var api = providerApiRepository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("ProviderApi", id.toString()));

        api.softDelete();
        providerApiRepository.save(api);
        log.info("Soft-deleted provider API: id={}, tenantId={}", id, tenantId);
    }
}
