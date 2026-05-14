package com.ksa.financing.middleware.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.middleware.application.dto.ClientRequestResponse;
import com.ksa.financing.middleware.domain.model.ApiClient;
import com.ksa.financing.middleware.domain.model.ClientRequest;
import com.ksa.financing.middleware.domain.model.EnvironmentType;
import com.ksa.financing.middleware.domain.model.ProviderApi;
import com.ksa.financing.middleware.domain.model.ThirdPartyProvider;
import com.ksa.financing.middleware.domain.port.in.ManageClientRequestUseCase;
import com.ksa.financing.middleware.domain.port.out.ClientRepository;
import com.ksa.financing.middleware.domain.port.out.ClientRequestRepository;
import com.ksa.financing.middleware.domain.port.out.ProviderApiRepository;
import com.ksa.financing.middleware.domain.port.out.ProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageClientRequestService implements ManageClientRequestUseCase {

    private final ClientRequestRepository clientRequestRepository;
    private final ClientRepository clientRepository;
    private final ProviderApiRepository providerApiRepository;
    private final ProviderRepository providerRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public ClientRequestResponse getById(UUID tenantId, EnvironmentType environment, UUID id) {
        var req = clientRequestRepository.findById(tenantId, environment, id)
                .orElseThrow(() -> NotFoundException.forEntity("ClientRequest", id.toString()));
        return enrich(tenantId, req);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientRequestResponse getByRequestId(UUID tenantId, EnvironmentType environment, String requestId) {
        var req = clientRequestRepository.findByRequestId(tenantId, environment, requestId)
                .orElseThrow(() -> NotFoundException.forEntity("ClientRequest", requestId));
        return enrich(tenantId, req);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClientRequestResponse> listAll(UUID tenantId, EnvironmentType environment, PageQuery query) {
        var page = clientRequestRepository.findAll(tenantId, environment, query);

        // Cache lookups for the duration of this page render so the same client/
        // api/provider isn't fetched repeatedly when many rows share them.
        var clientCache = new HashMap<UUID, ApiClient>();
        var apiCache = new HashMap<UUID, ProviderApi>();
        var providerCache = new HashMap<UUID, ThirdPartyProvider>();

        return page.map(req -> toResponse(req,
                lookupClient(tenantId, req.getClientId(), clientCache),
                lookupApi(tenantId, req.getApiId(), apiCache),
                lookupProvider(tenantId, lookupApi(tenantId, req.getApiId(), apiCache), providerCache)));
    }

    private ClientRequestResponse enrich(UUID tenantId, ClientRequest req) {
        ApiClient client = req.getClientId() != null
                ? clientRepository.findById(tenantId, req.getClientId()).orElse(null)
                : null;
        ProviderApi api = req.getApiId() != null
                ? providerApiRepository.findById(tenantId, req.getApiId()).orElse(null)
                : null;
        ThirdPartyProvider provider = api != null && api.getProviderId() != null
                ? providerRepository.findById(tenantId, api.getProviderId()).orElse(null)
                : null;
        return toResponse(req, client, api, provider);
    }

    private ApiClient lookupClient(UUID tenantId, UUID clientId, Map<UUID, ApiClient> cache) {
        if (clientId == null) return null;
        return cache.computeIfAbsent(clientId,
                id -> clientRepository.findById(tenantId, id).orElse(null));
    }

    private ProviderApi lookupApi(UUID tenantId, UUID apiId, Map<UUID, ProviderApi> cache) {
        if (apiId == null) return null;
        return cache.computeIfAbsent(apiId,
                id -> providerApiRepository.findById(tenantId, id).orElse(null));
    }

    private ThirdPartyProvider lookupProvider(UUID tenantId, ProviderApi api,
                                               Map<UUID, ThirdPartyProvider> cache) {
        if (api == null || api.getProviderId() == null) return null;
        return cache.computeIfAbsent(api.getProviderId(),
                id -> providerRepository.findById(tenantId, id).orElse(null));
    }

    private ClientRequestResponse toResponse(ClientRequest req,
                                              ApiClient client,
                                              ProviderApi api,
                                              ThirdPartyProvider provider) {
        return new ClientRequestResponse(
                req.getId(),
                req.getTenantId(),
                req.getApiId(),
                req.getClientId(),
                client != null ? client.getName() : null,
                req.getRequestId(),
                req.getProviderCode(),
                provider != null ? provider.getNameEn() : null,
                req.getApiCode(),
                api != null ? api.getNameEn() : null,
                req.getCallerService(),
                req.getEnvironment() != null ? req.getEnvironment().name() : null,
                req.getHttpMethod() != null ? req.getHttpMethod().name() : null,
                req.getRequestUrl(),
                parseJson(req.getRequestHeaders()),
                parseJson(req.getRequestBody()),
                req.getResponseStatus(),
                parseJson(req.getResponseHeaders()),
                parseJson(req.getResponseBody()),
                req.getStatus() != null ? req.getStatus().name() : null,
                req.getDurationMs(),
                req.getErrorMessage(),
                req.getIdempotencyKey(),
                req.getNationalId(),
                req.getMobileNumber(),
                req.getCallerService(),
                req.getCreatedAt()
        );
    }

    private Object parseJson(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return raw;
        }
    }
}
