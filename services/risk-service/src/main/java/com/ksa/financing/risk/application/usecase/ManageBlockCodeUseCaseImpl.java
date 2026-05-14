package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.risk.domain.model.BlockCode;
import com.ksa.financing.risk.domain.port.in.ManageBlockCodeUseCase;
import com.ksa.financing.risk.domain.port.out.BlockCodeRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageBlockCodeUseCaseImpl implements ManageBlockCodeUseCase {

    private final BlockCodeRepository repository;

    @Override
    @Transactional
    public BlockCode createBlockCode(UUID tenantId, CreateBlockCodeCommand command) {
        if (repository.findByCode(tenantId, command.code()).isPresent()) {
            throw new BusinessException(ErrorCodes.CONFLICT, "Block code already exists: " + command.code());
        }

        BlockCode blockCode = BlockCode.create(
            tenantId,
            command.code(),
            command.description(),
            command.type()
        );

        return repository.save(blockCode);
    }

    @Override
    @Transactional
    public BlockCode updateBlockCode(UUID tenantId, UUID id, UpdateBlockCodeCommand command) {
        BlockCode existing = repository.findById(tenantId, id)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Block code not found"));

        BlockCode updated = BlockCode.builder()
            .id(existing.getId())
            .tenantId(existing.getTenantId())
            .code(existing.getCode())
            .description(command.description() != null ? command.description() : existing.getDescription())
            .type(command.type() != null ? command.type() : existing.getType())
            .active(command.active() != null ? command.active() : existing.isActive())
            .createdAt(existing.getCreatedAt())
            .updatedAt(Instant.now())
            .version(existing.getVersion())
            .build();

        return repository.save(updated);
    }

    @Override
    public List<BlockCode> listBlockCodes(UUID tenantId) {
        return repository.findAll(tenantId);
    }

    @Override
    @Transactional
    public void deleteBlockCode(UUID tenantId, UUID id) {
        repository.delete(tenantId, id);
    }
}
