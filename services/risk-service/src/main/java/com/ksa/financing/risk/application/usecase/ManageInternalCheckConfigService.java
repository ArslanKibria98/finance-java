package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.risk.domain.model.InternalCheckConfig;
import com.ksa.financing.risk.domain.port.in.ManageInternalCheckConfigUseCase;
import com.ksa.financing.risk.domain.port.out.InternalCheckConfigRepository;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageInternalCheckConfigService implements ManageInternalCheckConfigUseCase {

    private final InternalCheckConfigRepository repository;

    @Override
    public List<InternalCheckConfig> listConfigs(UUID tenantId) {
        return repository.findAll(tenantId);
    }

    @Override
    @Transactional
    public InternalCheckConfig updateConfig(UUID tenantId, UUID id, UpdateConfigCommand command) {
        InternalCheckConfig existing = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("InternalCheckConfig", id.toString()));

        InternalCheckConfig updated = InternalCheckConfig.builder()
                .id(existing.getId())
                .tenantId(existing.getTenantId())
                .checkName(existing.getCheckName())
                .displayName(existing.getDisplayName())
                .description(existing.getDescription())
                .active(command.active() != null ? command.active() : existing.isActive())
                .blockCodeId(command.blockCodeId() != null ? command.blockCodeId() : existing.getBlockCodeId())
                .createdAt(existing.getCreatedAt())
                .updatedAt(Instant.now())
                .version(existing.getVersion())
                .build();

        return repository.save(updated);
    }
}
