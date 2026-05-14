package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.middleware.domain.model.ClientRequest;
import com.ksa.financing.middleware.domain.model.EnvironmentType;
import com.ksa.financing.middleware.domain.port.out.ClientRequestRepository;
import com.ksa.financing.middleware.infrastructure.persistence.entity.ClientRequestBaseJpaEntity;
import com.ksa.financing.middleware.infrastructure.persistence.entity.ClientRequestDevJpaEntity;
import com.ksa.financing.middleware.infrastructure.persistence.entity.ClientRequestProdJpaEntity;
import com.ksa.financing.middleware.infrastructure.persistence.entity.ClientRequestTestJpaEntity;
import com.ksa.financing.middleware.infrastructure.persistence.mapper.MiddlewarePersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ClientRequestRepositoryImpl implements ClientRequestRepository {

    private final JpaClientRequestTestRepository testRepo;
    private final JpaClientRequestDevRepository devRepo;
    private final JpaClientRequestProdRepository prodRepo;
    private final MiddlewarePersistenceMapper mapper;

    private static final Set<String> ALLOWED_FILTER_FIELDS =
            Set.of("apiId", "clientId", "status", "providerCode", "apiCode", "nationalId");
    private static final Set<String> SEARCHABLE_FIELDS =
            Set.of("requestId", "idempotencyKey", "requestUrl", "nationalId", "mobileNumber", "apiCode");

    @Override
    public ClientRequest save(ClientRequest request) {
        return switch (request.getEnvironment()) {
            case TEST -> saveTest(request);
            case DEV -> saveDev(request);
            case PROD -> saveProd(request);
        };
    }

    private ClientRequest saveTest(ClientRequest request) {
        var entity = request.getId() != null
                ? testRepo.findById(request.getId()).orElse(new ClientRequestTestJpaEntity())
                : new ClientRequestTestJpaEntity();
        mapper.copyToEntity(request, entity);
        var saved = testRepo.save(entity);
        return mapper.toDomain(saved, EnvironmentType.TEST);
    }

    private ClientRequest saveDev(ClientRequest request) {
        var entity = request.getId() != null
                ? devRepo.findById(request.getId()).orElse(new ClientRequestDevJpaEntity())
                : new ClientRequestDevJpaEntity();
        mapper.copyToEntity(request, entity);
        var saved = devRepo.save(entity);
        return mapper.toDomain(saved, EnvironmentType.DEV);
    }

    private ClientRequest saveProd(ClientRequest request) {
        var entity = request.getId() != null
                ? prodRepo.findById(request.getId()).orElse(new ClientRequestProdJpaEntity())
                : new ClientRequestProdJpaEntity();
        mapper.copyToEntity(request, entity);
        var saved = prodRepo.save(entity);
        return mapper.toDomain(saved, EnvironmentType.PROD);
    }

    @Override
    public Optional<ClientRequest> findById(UUID tenantId, EnvironmentType environment, UUID id) {
        return switch (environment) {
            case TEST -> testRepo.findByIdAndTenantId(id, tenantId)
                    .map(e -> mapper.toDomain(e, EnvironmentType.TEST));
            case DEV -> devRepo.findByIdAndTenantId(id, tenantId)
                    .map(e -> mapper.toDomain(e, EnvironmentType.DEV));
            case PROD -> prodRepo.findByIdAndTenantId(id, tenantId)
                    .map(e -> mapper.toDomain(e, EnvironmentType.PROD));
        };
    }

    @Override
    public Optional<ClientRequest> findByRequestId(UUID tenantId, EnvironmentType environment, String requestId) {
        return switch (environment) {
            case TEST -> testRepo.findByRequestIdAndTenantId(requestId, tenantId)
                    .map(e -> mapper.toDomain(e, EnvironmentType.TEST));
            case DEV -> devRepo.findByRequestIdAndTenantId(requestId, tenantId)
                    .map(e -> mapper.toDomain(e, EnvironmentType.DEV));
            case PROD -> prodRepo.findByRequestIdAndTenantId(requestId, tenantId)
                    .map(e -> mapper.toDomain(e, EnvironmentType.PROD));
        };
    }

    @Override
    public Optional<ClientRequest> findByIdempotencyKey(UUID tenantId, EnvironmentType environment, String idempotencyKey) {
        return switch (environment) {
            case TEST -> testRepo.findByIdempotencyKeyAndTenantId(idempotencyKey, tenantId)
                    .map(e -> mapper.toDomain(e, EnvironmentType.TEST));
            case DEV -> devRepo.findByIdempotencyKeyAndTenantId(idempotencyKey, tenantId)
                    .map(e -> mapper.toDomain(e, EnvironmentType.DEV));
            case PROD -> prodRepo.findByIdempotencyKeyAndTenantId(idempotencyKey, tenantId)
                    .map(e -> mapper.toDomain(e, EnvironmentType.PROD));
        };
    }

    @Override
    public PageResponse<ClientRequest> findAll(UUID tenantId, EnvironmentType environment, PageQuery query) {
        return switch (environment) {
            case TEST -> findAllInRepo(testRepo, tenantId, query, EnvironmentType.TEST);
            case DEV -> findAllInRepo(devRepo, tenantId, query, EnvironmentType.DEV);
            case PROD -> findAllInRepo(prodRepo, tenantId, query, EnvironmentType.PROD);
        };
    }

    private <E extends ClientRequestBaseJpaEntity, R extends org.springframework.data.jpa.repository.JpaRepository<E, UUID>
            & org.springframework.data.jpa.repository.JpaSpecificationExecutor<E>>
    PageResponse<ClientRequest> findAllInRepo(R repo, UUID tenantId, PageQuery query, EnvironmentType env) {
        Specification<E> tenantSpec = (root, q, cb) -> cb.equal(root.get("tenantId"), tenantId);
        Specification<E> dynamic = SpecificationBuilder.<E>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();
        Page<E> page = repo.findAll(tenantSpec.and(dynamic), query.toPageable());
        return PageResponse.from(page, e -> mapper.toDomain(e, env));
    }
}
