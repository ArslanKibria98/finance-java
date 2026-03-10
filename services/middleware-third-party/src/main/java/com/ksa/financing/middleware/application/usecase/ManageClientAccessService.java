package com.ksa.financing.middleware.application.usecase;

import com.ksa.financing.middleware.application.dto.ClientApiAccessResponse;
import com.ksa.financing.middleware.application.dto.ClientProviderAccessResponse;
import com.ksa.financing.middleware.application.dto.GrantAccessRequest;
import com.ksa.financing.middleware.domain.model.AccessEnvironment;
import com.ksa.financing.middleware.domain.model.ClientApiAccess;
import com.ksa.financing.middleware.domain.model.ClientProviderAccess;
import com.ksa.financing.middleware.domain.port.in.ManageClientAccessUseCase;
import com.ksa.financing.middleware.domain.port.out.ClientAccessRepository;
import com.ksa.financing.middleware.domain.port.out.ClientRepository;
import com.ksa.financing.middleware.domain.port.out.ProviderApiRepository;
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
public class ManageClientAccessService implements ManageClientAccessUseCase {

    private final ClientAccessRepository clientAccessRepository;
    private final ClientRepository clientRepository;
    private final ProviderRepository providerRepository;
    private final ProviderApiRepository providerApiRepository;

    @Override
    public ClientProviderAccessResponse grantProviderAccess(UUID tenantId, UUID clientId,
                                                             GrantAccessRequest request, UUID grantedBy) {
        // Validate client exists
        clientRepository.findById(tenantId, clientId)
                .orElseThrow(() -> NotFoundException.forEntity("Client", clientId.toString()));

        // Validate provider exists
        var provider = providerRepository.findById(tenantId, request.targetId())
                .orElseThrow(() -> NotFoundException.forEntity("Provider", request.targetId().toString()));

        // Check if access already exists
        var existing = clientAccessRepository.findProviderAccess(tenantId, clientId, request.targetId());
        if (existing.isPresent()) {
            var access = existing.get();
            if (access.isActive()) {
                throw new BusinessException("MIDDLEWARE.ACCESS.ALREADY_GRANTED",
                        "Provider access already granted for client: " + clientId + " -> provider: " + request.targetId());
            }
            // Reactivate if previously revoked
            access.reactivate();
            var saved = clientAccessRepository.saveProviderAccess(access);
            log.info("Reactivated provider access: clientId={}, providerId={}, tenantId={}", clientId, request.targetId(), tenantId);
            return toProviderAccessResponse(saved, provider.getName(), provider.getCode());
        }

        var access = ClientProviderAccess.grant(
                tenantId, clientId, request.targetId(),
                AccessEnvironment.valueOf(request.environment()), grantedBy
        );

        var saved = clientAccessRepository.saveProviderAccess(access);
        log.info("Granted provider access: clientId={}, providerId={}, tenantId={}", clientId, request.targetId(), tenantId);
        return toProviderAccessResponse(saved, provider.getName(), provider.getCode());
    }

    @Override
    public ClientApiAccessResponse grantApiAccess(UUID tenantId, UUID clientId,
                                                   GrantAccessRequest request, UUID grantedBy) {
        // Validate client exists
        clientRepository.findById(tenantId, clientId)
                .orElseThrow(() -> NotFoundException.forEntity("Client", clientId.toString()));

        // Validate API exists
        var api = providerApiRepository.findById(tenantId, request.targetId())
                .orElseThrow(() -> NotFoundException.forEntity("ProviderApi", request.targetId().toString()));

        // Validate provider exists for enriched response
        var provider = providerRepository.findById(tenantId, api.getProviderId())
                .orElseThrow(() -> NotFoundException.forEntity("Provider", api.getProviderId().toString()));

        // Check if access already exists
        var existing = clientAccessRepository.findApiAccess(tenantId, clientId, request.targetId());
        if (existing.isPresent()) {
            var access = existing.get();
            if (access.isActive()) {
                throw new BusinessException("MIDDLEWARE.ACCESS.ALREADY_GRANTED",
                        "API access already granted for client: " + clientId + " -> api: " + request.targetId());
            }
            access.reactivate();
            var saved = clientAccessRepository.saveApiAccess(access);
            log.info("Reactivated API access: clientId={}, apiId={}, tenantId={}", clientId, request.targetId(), tenantId);
            return toApiAccessResponse(saved, api.getName(), api.getCode(), provider.getId(), provider.getName());
        }

        var access = ClientApiAccess.grant(
                tenantId, clientId, request.targetId(),
                AccessEnvironment.valueOf(request.environment()), grantedBy
        );

        var saved = clientAccessRepository.saveApiAccess(access);
        log.info("Granted API access: clientId={}, apiId={}, tenantId={}", clientId, request.targetId(), tenantId);
        return toApiAccessResponse(saved, api.getName(), api.getCode(), provider.getId(), provider.getName());
    }

    @Override
    public void revokeProviderAccess(UUID tenantId, UUID clientId, UUID providerId) {
        var access = clientAccessRepository.findProviderAccess(tenantId, clientId, providerId)
                .orElseThrow(() -> NotFoundException.forEntity("ClientProviderAccess",
                        "clientId=" + clientId + ", providerId=" + providerId));

        clientAccessRepository.deleteProviderAccess(tenantId, clientId, providerId);
        log.info("Revoked provider access: clientId={}, providerId={}, tenantId={}", clientId, providerId, tenantId);
    }

    @Override
    public void revokeApiAccess(UUID tenantId, UUID clientId, UUID apiId) {
        var access = clientAccessRepository.findApiAccess(tenantId, clientId, apiId)
                .orElseThrow(() -> NotFoundException.forEntity("ClientApiAccess",
                        "clientId=" + clientId + ", apiId=" + apiId));

        clientAccessRepository.deleteApiAccess(tenantId, clientId, apiId);
        log.info("Revoked API access: clientId={}, apiId={}, tenantId={}", clientId, apiId, tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientProviderAccessResponse> listProviderAccess(UUID tenantId, UUID clientId) {
        return clientAccessRepository.findProviderAccessByClient(tenantId, clientId)
                .stream().map(access -> {
                    var provider = providerRepository.findById(tenantId, access.getProviderId()).orElse(null);
                    return toProviderAccessResponse(access,
                            provider != null ? provider.getName() : null,
                            provider != null ? provider.getCode() : null);
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientApiAccessResponse> listApiAccess(UUID tenantId, UUID clientId) {
        return clientAccessRepository.findApiAccessByClient(tenantId, clientId)
                .stream().map(access -> enrichApiAccessResponse(tenantId, access)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientApiAccessResponse> listApiAccessByProvider(UUID tenantId, UUID clientId, UUID providerId) {
        return clientAccessRepository.findApiAccessByClientAndProvider(tenantId, clientId, providerId)
                .stream().map(access -> enrichApiAccessResponse(tenantId, access)).toList();
    }

    private ClientApiAccessResponse enrichApiAccessResponse(UUID tenantId, ClientApiAccess access) {
        var api = providerApiRepository.findById(tenantId, access.getApiId()).orElse(null);
        var provider = api != null ? providerRepository.findById(tenantId, api.getProviderId()).orElse(null) : null;
        return toApiAccessResponse(access,
                api != null ? api.getName() : null,
                api != null ? api.getCode() : null,
                provider != null ? provider.getId() : null,
                provider != null ? provider.getName() : null);
    }

    private ClientProviderAccessResponse toProviderAccessResponse(ClientProviderAccess access,
                                                                    String providerName, String providerCode) {
        return new ClientProviderAccessResponse(
                access.getId(), access.getClientId(), access.getProviderId(),
                providerName, providerCode,
                access.getEnvironment() != null ? access.getEnvironment().name() : null,
                access.isActive(), access.getGrantedAt()
        );
    }

    private ClientApiAccessResponse toApiAccessResponse(ClientApiAccess access,
                                                         String apiName, String apiCode,
                                                         UUID providerId, String providerName) {
        return new ClientApiAccessResponse(
                access.getId(), access.getClientId(), access.getApiId(),
                apiName, apiCode, providerId, providerName,
                access.getEnvironment() != null ? access.getEnvironment().name() : null,
                access.isActive(), access.getGrantedAt()
        );
    }
}
