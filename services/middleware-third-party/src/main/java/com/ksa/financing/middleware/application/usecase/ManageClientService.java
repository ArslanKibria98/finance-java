package com.ksa.financing.middleware.application.usecase;

import com.ksa.financing.middleware.application.dto.ClientResponse;
import com.ksa.financing.middleware.application.dto.CreateClientRequest;
import com.ksa.financing.middleware.application.mapper.MiddlewareMapper;
import com.ksa.financing.middleware.domain.model.AccessEnvironment;
import com.ksa.financing.middleware.domain.model.ApiClient;
import com.ksa.financing.middleware.domain.port.in.ManageClientUseCase;
import com.ksa.financing.middleware.domain.port.out.ClientRepository;
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
public class ManageClientService implements ManageClientUseCase {

    private final ClientRepository clientRepository;
    private final MiddlewareMapper mapper;

    @Override
    public ClientResponse create(UUID tenantId, CreateClientRequest request, UUID createdBy) {
        if (clientRepository.existsByCode(tenantId, request.code())) {
            throw new BusinessException("MIDDLEWARE.CLIENT.DUPLICATE_CODE",
                    "Client with code already exists: " + request.code(), request.code());
        }

        var client = ApiClient.create(
                tenantId,
                request.name(),
                request.code(),
                request.description(),
                request.callbackUrl(),
                AccessEnvironment.valueOf(request.environment()),
                createdBy
        );
        client.setIpWhitelist(request.ipWhitelist());

        var saved = clientRepository.save(client);
        log.info("Created client: code={}, tenantId={}", saved.getCode(), tenantId);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getById(UUID tenantId, UUID id) {
        return clientRepository.findById(tenantId, id)
                .map(mapper::toResponse)
                .orElseThrow(() -> NotFoundException.forEntity("Client", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> listAll(UUID tenantId) {
        return clientRepository.findAllByTenant(tenantId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    public ClientResponse update(UUID tenantId, UUID id, CreateClientRequest request) {
        var client = clientRepository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Client", id.toString()));

        client.setName(request.name());
        client.setDescription(request.description());
        client.setCallbackUrl(request.callbackUrl());
        client.setEnvironment(AccessEnvironment.valueOf(request.environment()));
        client.setIpWhitelist(request.ipWhitelist());

        var saved = clientRepository.save(client);
        log.info("Updated client: id={}, tenantId={}", id, tenantId);
        return mapper.toResponse(saved);
    }

    @Override
    public ClientResponse regenerateSecret(UUID tenantId, UUID id) {
        var client = clientRepository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Client", id.toString()));

        client.regenerateSecretKey();
        var saved = clientRepository.save(client);
        log.info("Regenerated secret key for client: id={}, tenantId={}", id, tenantId);
        return mapper.toResponse(saved);
    }

    @Override
    public void delete(UUID tenantId, UUID id) {
        var client = clientRepository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Client", id.toString()));

        client.softDelete();
        clientRepository.save(client);
        log.info("Soft-deleted client: id={}, tenantId={}", id, tenantId);
    }
}
