package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.risk.domain.model.tenant.TenantConfig;
import com.ksa.financing.risk.domain.model.tenant.TenantStatus;
import com.ksa.financing.risk.domain.port.in.ManageTenantConfigUseCase;
import com.ksa.financing.risk.domain.port.out.TenantConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageTenantConfigService implements ManageTenantConfigUseCase {

    private final TenantConfigRepository tenantConfigRepository;

    @Override
    @Transactional
    public TenantConfig create(UUID tenantId, CreateTenantConfigCommand command) {
        if (tenantConfigRepository.existsByTenantId(tenantId)) {
            throw new BusinessException("RISK.TENANT.ALREADY_EXISTS",
                    "Tenant config already exists for tenant: " + tenantId);
        }

        var config = new TenantConfig();
        config.setId(UUID.randomUUID());
        config.setTenantId(tenantId);
        config.setTenantName(command.tenantName());
        config.setTenantNameAr(command.tenantNameAr());
        config.setStatus(TenantStatus.ACTIVE);
        config.setCustomerRiskEnabled(command.customerRiskEnabled());
        config.setBusinessRiskEnabled(command.businessRiskEnabled());
        config.setLoanRiskEnabled(command.loanRiskEnabled());
        config.setCreatedAt(Instant.now());
        config.setUpdatedAt(Instant.now());
        config.setVersion(1);

        var saved = tenantConfigRepository.save(config);
        log.info("Tenant config created for tenant: {}", tenantId);
        return saved;
    }

    @Override
    @Transactional
    public TenantConfig update(UUID tenantId, UUID id, UpdateTenantConfigCommand command) {
        var config = tenantConfigRepository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("TenantConfig", id.toString()));

        if (command.tenantName() != null) config.setTenantName(command.tenantName());
        if (command.tenantNameAr() != null) config.setTenantNameAr(command.tenantNameAr());
        if (command.customerRiskEnabled() != null) config.setCustomerRiskEnabled(command.customerRiskEnabled());
        if (command.businessRiskEnabled() != null) config.setBusinessRiskEnabled(command.businessRiskEnabled());
        if (command.loanRiskEnabled() != null) config.setLoanRiskEnabled(command.loanRiskEnabled());
        config.setUpdatedAt(Instant.now());

        var saved = tenantConfigRepository.save(config);
        log.info("Tenant config updated for tenant: {}", tenantId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public TenantConfig getById(UUID tenantId, UUID id) {
        return tenantConfigRepository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("TenantConfig", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public TenantConfig getByTenantId(UUID tenantId) {
        return tenantConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> NotFoundException.forEntity("TenantConfig", tenantId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantConfig> getAll() {
        return tenantConfigRepository.findAll();
    }

    @Override
    @Transactional
    public void activate(UUID tenantId, UUID id) {
        var config = tenantConfigRepository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("TenantConfig", id.toString()));
        config.setStatus(TenantStatus.ACTIVE);
        config.setUpdatedAt(Instant.now());
        tenantConfigRepository.save(config);
        log.info("Tenant config activated: {}", id);
    }

    @Override
    @Transactional
    public void deactivate(UUID tenantId, UUID id) {
        var config = tenantConfigRepository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("TenantConfig", id.toString()));
        config.setStatus(TenantStatus.INACTIVE);
        config.setUpdatedAt(Instant.now());
        tenantConfigRepository.save(config);
        log.info("Tenant config deactivated: {}", id);
    }
}
